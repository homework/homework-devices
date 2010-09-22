/*
 * flowlogger - use pcap to detect traffic on an interface and insert
 *              corresponding tuples into the homework database server
 *
 * Joe Sventek
 *
 * some pcap code borrowed from tutorials by Martin Casado and Tim Carstens
 */

#include "flow_accumulator.h"
#include "srpc.h"
#include "config.h"
#include <pcap.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <time.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <net/ethernet.h>
#include <pthread.h>

#define ETHER_ADDR_SIZE 6
#define SIZE_ETHERNET 14

struct flowlog_ethernet {
    u_char ether_dhost[ETHER_ADDR_SIZE];	/* destination host address */
    u_char ether_shost[ETHER_ADDR_SIZE];	/* source host address */
    u_short ether_type;				/* IP, ARP, RARP, ? */
};

struct flowlog_ip {
    u_char ip_vhl;		/* version << 4 | header length >> 2 */
    u_char ip_tos;		/* type of service */
    u_short ip_len;		/* total length */
    u_short ip_id;		/* identification */
    u_short ip_off;		/* fragment offset field */
#define IP_RF 0x8000		/* reserved fragment flag */
#define IP_DF 0x4000		/* dont fragment flag */
#define IP_MF 0x2000		/* more fragments flag */
#define IP_OFFMASK 0x1fff	/* mask for fragmenting bits */
    u_char ip_ttl;		/* time to live */
    u_char ip_p;		/* protocol */
    u_short ip_sum;		/* checksum */
    struct in_addr ip_src,ip_dst; /* source and dest address */
};
#define IP_HL(ip)		(((ip)->ip_vhl) & 0x0f)
#define IP_V(ip)		(((ip)->ip_vhl) >> 4)

struct flowlog_tcp {
    u_short th_sport;	/* source port */
    u_short th_dport;	/* destination port */
    u_int32_t th_seq;	/* sequence number */
    u_int32_t th_ack;	/* acknowledgement number */

    u_char th_offx2;	/* data offset, rsvd */
#define TH_OFF(th)	(((th)->th_offx2 & 0xf0) >> 4)
    u_char th_flags;
#define TH_FIN 0x01
#define TH_SYN 0x02
#define TH_RST 0x04
#define TH_PUSH 0x08
#define TH_ACK 0x10
#define TH_URG 0x20
#define TH_ECE 0x40
#define TH_CWR 0x80
#define TH_FLAGS (TH_FIN|TH_SYN|TH_RST|TH_ACK|TH_URG|TH_ECE|TH_CWR)
    u_short th_win;		/* window */
    u_short th_sum;		/* checksum */
    u_short th_urp;		/* urgent pointer */
};

struct flowlog_udp {
    u_short uh_sport;		/* source port */
    u_short uh_dport;		/* destination port */
    u_short uh_ulen;		/* udp length */
    u_short uh_sum;		/* udp checksum */
};
#define SIZE_UDP 8

static pcap_t* descr;

void my_callback(u_char *args, const struct pcap_pkthdr* pkthdr,
		 const u_char* packet) {
    u_int size_ip;
    struct flowlog_ethernet *ep = (struct flowlog_ethernet *)packet;
    struct flowlog_ip *ip;
    struct flowlog_tcp *tcp;
    struct flowlog_udp *udp;
    u_char proto;
    u_int size_pkt = pkthdr->len;	/* on the "wire" length */
    u_short sport, dport;

    if (ntohs (ep->ether_type) != ETHERTYPE_IP)
        return;
    ip = (struct flowlog_ip *)(packet + SIZE_ETHERNET);
    size_ip = IP_HL(ip) * 4;
    if (size_ip < 20) {
        printf("Invalid IP header length: %u bytes\n", size_ip);
        return;
    }
    proto = ip->ip_p;
    if (proto == 6) {		/* TCP packet */
        tcp = (struct flowlog_tcp *)(packet + SIZE_ETHERNET + size_ip);
        sport = ntohs(tcp->th_sport);
        dport = ntohs(tcp->th_dport);
    } else if (proto == 17) {	/* UDP packet */
        udp = (struct flowlog_udp *)(packet + SIZE_ETHERNET + size_ip);
        sport = ntohs(udp->uh_sport);
        dport = ntohs(udp->uh_dport);
/*
 * For other values of proto, if one can dig into the IP payload for
 * port information, you will need to define flowlog_XXX structures above,
 * and add an equivalent "else if" for each protocol here
 */
    } else {
        sport = 0;
	dport = 0;
    }
    acc_update(ip->ip_src, ip->ip_dst, sport, dport, proto, size_pkt);
}

#define QUERY_SIZE 50000
#define MAX_INSERTS (QUERY_SIZE/120)	/* each insert ~120 characters */
static RpcConnection rpc;
static struct timespec time_delay = {1, 0};	/* delay 1 second */
static char buf[QUERY_SIZE], resp[32768];
static unsigned long long dropped = 0;
/*
 * thread that communicates new flow tuples to HWDB
 */
static void *handler(void *args) {
    int size;
    unsigned long nelems;
    AccElem **buckets;
    AccElem *returns, *p, *q;
    int i;
    unsigned int sofar;
    unsigned int len;
    struct pcap_stat stats;
    unsigned long long curdrop;
    unsigned long totalP, totalB, dp, psize;
    unsigned long ninserts;

    for (;;) {
        nanosleep(&time_delay, NULL);	/* sleep for 1 second */
        buckets = acc_swap(&size, &nelems);
        pcap_stats(descr, &stats);	/* determine if any dropped pkts */
	curdrop = (unsigned long long)(stats.ps_drop);
	if (curdrop > dropped) {
	    dp = (unsigned long) (curdrop - dropped);
	    dropped = curdrop;
	} else
	    dp = 0;
	if (nelems <= 0 && dp == 0)		/* nothing to do */
            continue;
	ninserts = nelems + (dp ? 1l : 0);
	sofar = 0;
	sofar += sprintf(buf+sofar, "BULK:%ld\n", ninserts);
	returns = NULL;
	totalP = 0;
	totalB = 0;
	if (nelems > 0) {
            for (i = 0; i < size; i++) {
                p = buckets[i];
	        while (p != NULL) {
                    FlowRec *f = &(p->id);
                    q = p->next;
		    sofar += sprintf(buf+sofar,
				"insert into Flows values ('%u', ", f->proto);
		    sofar += sprintf(buf+sofar, "\"%s\", '%hu', ",
				inet_ntoa(f->ip_src), f->sport);
		    sofar += sprintf(buf+sofar, "\"%s\", '%hu', ",
				inet_ntoa(f->ip_dst), f->dport);
		    sofar += sprintf(buf+sofar, "'%ld', '%ld')\n",
				p->packets, p->bytes);
		    totalP += p->packets;
		    totalB += p->bytes;
		    p->next = returns;
		    returns = p;
		    p = q;
	        }
	    }
	    acc_freeChain(returns);
	}
	if (dp > 0) {
	    if (totalP <= 0)
		psize = 80;
	    else
		psize = totalB / totalP;
	    sofar += sprintf(buf+sofar, "insert into Flows values ('0', ");
	    sofar += sprintf(buf+sofar, "\"0.0.0.0\", '0', ");
	    sofar += sprintf(buf+sofar, "\"0.0.0.0\", '0', ");
	    sofar += sprintf(buf+sofar, "'%ld', '%ld')\n", dp, dp * psize);
	}
	if (! rpc_call(rpc, buf, sofar, resp, sizeof(resp), &len)) {
            fprintf(stderr, "rpc_call() failed\n");
	}
    }
    return (args) ? NULL : args;	/* unused warning subterfuge */
}

#define USAGE "./flowlogger [-d device] [-f filter string]"

int main(int argc,char *argv[])
{ 
    char *dev;
    char *pfp;
    char errbuf[PCAP_ERRBUF_SIZE];
    struct bpf_program fp;      /* hold compiled program     */
    bpf_u_int32 maskp;          /* subnet mask               */
    bpf_u_int32 netp;           /* ip                        */
    u_char* args = NULL;
    int i, j;
    pthread_t thr;
    char *target;
    unsigned short port;

    dev = "wlan0";
    pfp = "ip";
    target = HWDB_SERVER_ADDR;
    port = HWDB_SERVER_PORT;
    for (i = 1; i < argc; ) {
        if ((j = i + 1) == argc) {
            fprintf(stderr, "usage: %s\n", USAGE);
            exit(1);
        }
        if (strcmp(argv[i], "-d") == 0)
	    dev = argv[j];
        else if (strcmp(argv[i], "-f") == 0)
            pfp = argv[j];
        else {
            fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
        }
        i = j + 1;
    }

    if (! rpc_init(0)) {
        fprintf(stderr, "Initialization failure for rpc system\n");
	exit(-1);
    }
    rpc = rpc_connect(target, port, "HWDB", 1l);
    if (rpc == NULL) {
        fprintf(stderr, "Error connecting to HWDB at %s:%05u\n", target, port);
	exit(-1);
    }
    acc_init();

    /* ask pcap for the network address and mask of the device */
    pcap_lookupnet(dev, &netp, &maskp, errbuf);

    /* open device for reading. NOTE: defaulting to
     * promiscuous mode*/
    descr = pcap_open_live(dev, BUFSIZ, 1, -1, errbuf);
    if(descr == NULL) {
        fprintf(stderr, "pcap_open_live(): %s\n", errbuf);
	exit(1);
    }

    if(pfp != NULL) {
        /* Lets try and compile the program.. non-optimized */
        if(pcap_compile(descr, &fp, pfp, 0, netp) == -1) {
            fprintf(stderr, "Error calling pcap_compile\n");
	    exit(1);
	}

        /* set the compiled program as the filter */
        if(pcap_setfilter(descr, &fp) == -1) {
            fprintf(stderr, "Error setting filter\n");
	    exit(1);
	}
    }

    if (pthread_create(&thr, NULL, handler, NULL)) {
        fprintf(stderr, "Failure to start database thread\n");
	exit(1);
    }
    /* ... and loop */ 
    pcap_loop(descr, -1, my_callback, args);

    fprintf(stderr, "\nfinished\n");
    return 0;
}
