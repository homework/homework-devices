#include <pcap.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "rt_parser.h"	/* PHY layer: the radiotap header */
#include "i8_parser.h"	/* MAC layer: the IEEE 802.11 header */

#include <arpa/inet.h>
#include <netinet/ether.h>
#include <netinet/if_ether.h>
#include <net/ethernet.h>

#include "link_accumulator.h"

#include "srpc.h"
#include "config.h"

#include <pthread.h>

static pcap_t* descr;
static int verbose = 0;

struct llc_snap_header {
	// LLC  is 3 bytes;
	// SNAP is 5 bytes.
	uint8_t dsap; // ; Destination Service Access Point;
	uint8_t ssap; // ; Source Service Access Point;
	uint8_t cntl; // ; Control.
	// SNAP is used when LLC carries IP packets.
	uint16_t orgcode1;
	uint8_t  orgcode2;
	uint16_t ethernet_type;
};
#define LLC_SNAP_HEADER_LENGTH 8

// The following structures are courtesy of flowlogger.c:
// flowlog_ip; flowlog_tcp; flowlog_udp;

struct flowlog_ip {
	u_char ip_vhl;
	u_char ip_tos;
	u_short ip_len;
	u_short ip_id;
	u_short ip_off;
#define IP_RF 	0x8000
#define IP_DF 	0x4000
#define IP_MF 	0x2000
#define IP_OFFMASK 0x1fff
	u_char ip_ttl;
	u_char ip_p;
	u_short ip_sum;
	struct in_addr ip_src,ip_dst;
};
#define IP_HL(ip) 	(((ip)->ip_vhl) & 0x0f)
#define IP_V(ip) 	(((ip)->ip_vhl) >> 4)

struct flowlog_tcp {
	u_short th_sport;
	u_short th_dport;
	u_int32_t th_seq;
	u_int32_t th_ack;
	u_char th_offx2;
#define TH_OFF(th) 	(((th)->th_offx2 & 0xf0) >> 4)
	u_char th_flags;
#define TH_FIN 	0x01
#define TH_SYN 	0x02
#define TH_RST 	0x04
#define TH_PUSH 0x08
#define TH_ACK 	0x10
#define TH_URG 	0x20
#define TH_ECE 	0x40
#define TH_CWR 	0x80
#define TH_FLAGS 	(TH_FIN|TH_SYN|TH_RST|TH_ACK|TH_URG|TH_ECE|TH_CWR)
	u_short th_win;
	u_short th_sum;
	u_short th_urp;
};

struct flowlog_udp {
	u_short uh_sport;
	u_short uh_dport;
	u_short uh_ulen;
	u_short uh_sum;
};
#define SIZE_UDP 8

void my_callback (u_char *args, const struct pcap_pkthdr* pkthdr,
	const u_char *packet) {
	/* The rss value from the radio.
	 */
	int8_t rss = 0;
	const u_char *eot = NULL; /* eight-o-two 11 header. */
	uint16_t fc;
	/* Source address (SA), destination address (DA),
	 * and basic service set identification (BSSID).
	 */
	uint64_t SA = 0;
	uint64_t DA = 0;
	uint64_t BSSID = 0;
	u_int size_pkt = pkthdr->len - get_radiotap_header_length(packet);
	/* First access the radiotap header. */
	rss = get_rss (packet);
	if (verbose) printf("rss = %d; ", rss);
	/* Then, the 802.11 header. */
	eot = (u_char *) (packet + get_radiotap_header_length (packet));
	fc = EXTRACT_LE_16BITS (eot);
	int retry = 0;
	if (RETRY(fc))
		retry = 1;
	if (verbose) printf("retry = %d; ", retry);
	switch (TYPE(fc)) {
		case TYPE_MGMT:
			// Management frame.
			SA    = address_field (eot + 10);
			DA    = address_field (eot +  4);
			BSSID = address_field (eot + 16);
			if (verbose) sample_management_frame(fc, SA, DA);
			break;
		case TYPE_CTRL:
			// Control frame.
			if (verbose) sample_control_frame(fc);
			return; /* Do not process control packets. */

		case TYPE_DATA:
/* Data frame.
 *
 * According to the 802.11 standard:
 *
 * 		Addr1		Addr2		Addr3		Addr4
 * __________________________________________________________________
 * 0	0	RA=DA		TA=SA		BSSID		n/a
 * 0	1	RA=DA		TA=BSSID	SA		n/a
 * 1	0	RA=BSSID	TA=SA		DA		n/a
 * __________________________________________________________________
 *
 * The above table translates as follows:
 */
			if (!TDS(fc) && !FDS(fc)){
				printf( "Ad hoc mode. Not supported yet.\n" );
				return;
			} else
			if (!TDS(fc) &&  FDS(fc)){
				// AP to STA.
				SA    = address_field (eot + 16);
				DA    = address_field (eot +  4);
				BSSID = address_field (eot + 10);
					if (verbose) {
				char* sas = mac_to_string(SA);
				char* das = mac_to_string(DA);
				char* bss = mac_to_string(BSSID);
				printf("SA %s -> DA %s (BSSID %s)\n", 
				sas, das, bss);
				free(sas);
				free(das);
				free(bss);
					}
			} else
			if ( TDS(fc) && !FDS(fc)){
				// STA to AP.
				SA    = address_field (eot + 10);
				DA    = address_field (eot + 16);
				BSSID = address_field (eot +  4);
					if (verbose) {
				char* sas = mac_to_string(SA);
				char* das = mac_to_string(DA);
				char* bss = mac_to_string(BSSID);
				printf("SA %s -> DA %s (BSSID %s)\n", 
				sas, das, bss);
				free(sas);
				free(das);
				free(bss);
					}
			} else
			if( TDS(fc) &&  FDS(fc)) {
				printf( "Not defined yet in standard.\n" );
				return;
			}
			break;
		case TYPE_RESV:
			printf( "Type is reserved. Not supported yet.\n" );
			return;

			break;
		default:
			printf("Unknown type. Exit.\n");
			exit(1);
	}
	/* Update accumulated values address */
	lt_update(SA, rss, retry, size_pkt);
	
	/* Move further into the packet? */
	
	/*
	if (TYPE(fc) == TYPE_DATA) {
		if (PROTECTED(fc)) {
			// printf( "Unfortunately, the packet is encrypted.\n" );
			return;
		} else {
			printf( "Moving further into packet.\n" );
		}
		uint8_t isIPv4 = 0;
		const u_char *llc = (u_char *)
			(packet + get_radiotap_header_length (packet) + 
				IEEE80211_DATA_HEADER_LENGTH);
		// Just get the ethernet type.
		uint16_t ethernet_type = EXTRACT_BE_16BITS(llc + 6);
		if (ethernet_type == ETHERTYPE_ARP) {
			printf("ARP. Ignore.\n");
		} else
		if (ethernet_type == ETHERTYPE_IPV6) {
			printf("IPv6. Ignore.\n"); // At least for now.
		} else
		if (ethernet_type == ETHERTYPE_IP) {
			printf("IPv4. Process.\n");
			isIPv4 = 1;
		} else {
			printf("Unkown ethernet type.\n");
		}
		// Since this is a data packet, print it.
		if (isIPv4) {
			struct flowlog_ip *ip = (struct flowlog_ip *)(llc + 8);
       	 		printf("Data packet from %s\t", inet_ntoa(ip->ip_src));
        		printf("to %s\t", inet_ntoa(ip->ip_dst));
        		printf("of type %u\n", ip->ip_p);
		}
	}
	*/

	return;
}

#define QUERY_SIZE 50000
#define MAX_INSERTS (QUERY_SIZE/120)
static RpcConnection rpc;
static struct timespec time_delay = {10, 0};	/* delay 1 second */
static char buf[QUERY_SIZE], resp[32768];
/*
 * thread that communicates new flow tuples to HWDB
 */
static void *handler(void *args) {
	int size;
	unsigned long nelems;
	LinkTableElem **buckets;
	LinkTableElem *returns, *p, *q;
	int i;
	unsigned int sofar;
	unsigned int len;
	for (;;) {
		nanosleep(&time_delay, NULL);	/* sleep for 1 second */
		buckets = lt_swap(&size, &nelems);
		if (nelems <= 0) 		/* nothing to do */
			continue;
		sofar = 0;
		sofar += sprintf(buf+sofar, "BULK:%ld\n", nelems);
		returns = NULL;
		for (i = 0; i < size; i++) {
			p = buckets[i];
			while (p != NULL) {
				LinkRec *f = &(p->id);
				char *addr = mac_to_string(f->mac);
				q = p->next;
				sofar += sprintf(buf+sofar,
"insert into Links values (\"%s\", '%f', '%ld', '%ld', '%ld')\n",
	addr, p->avg_rss, p->retries, p->packets, p->nobytes);
				p->next = returns;
				returns = p;
				p = q;
				free(addr);
			}
		}
		lt_freeChain(returns);
		// printf( "Query is %s", buf);
		if (! rpc_call(rpc, buf, sofar, resp, sizeof(resp), &len)) {
	 		fprintf(stderr, "rpc_call() failed\n");
	 	}
	}
	return (args) ? NULL : args;	/* unused warning subterfuge */
}

#define USAGE "./linklogger [-d device] [-v packets]"

int main(int argc,char *argv[]) {
	
	char *dev;
	char errbuf[PCAP_ERRBUF_SIZE];
	bpf_u_int32 maskp;
	bpf_u_int32 netp;
	u_char* args = NULL;
	int i, j;
	pthread_t thr;
	char *target;
	unsigned short port;

	dev = "mon.wlan0";
	target = HWDB_SERVER_ADDR;
	port = HWDB_SERVER_PORT;
	for (i = 1; i < argc; ) {
		if ((j = i + 1) == argc) {
			fprintf(stderr, "usage: %s\n", USAGE);
			exit(1);
		}
		if (strcmp(argv[i], "-d") == 0)
			dev = argv[j];
		else if (strcmp(argv[i], "-v") == 0) {
			if (strcmp(argv[j], "packets") == 0)
				verbose = 1;
			else fprintf(stderr, "Unknown flag: %s %s\n", 
				argv[i], argv[j]);
		}
		else {
			fprintf(stderr, "Unknown flag: %s %s\n", 
				argv[i], argv[j]);
		}
		i = j + 1;
	}

	/* ask pcap for the network address and mask of the device */
	if (verbose) 
		printf("Device is %s\n", dev);
	pcap_lookupnet(dev, &netp, &maskp, errbuf);

	/* open device for reading. NOTE: defaulting to
	 * promiscuous mode*/
	descr = pcap_open_live(dev, BUFSIZ, 1, -1, errbuf);
	if(descr == NULL) {
		fprintf(stderr, "pcap_open_live(): %s\n", errbuf);
		exit(1);
	}

	// For radiotap, datalink should be DLT_IEEE802_11_RADIO. 
	// Otherwise, exit.
	if( pcap_datalink(descr) != DLT_IEEE802_11_RADIO) {
		if (pcap_set_datalink (descr, DLT_IEEE802_11_RADIO) == -1) {
			pcap_perror(descr, "Error");
			exit(1);
		}
	}
	if (! rpc_init(0)) {
		fprintf(stderr, "Initialization failure for rpc system\n");
		exit(-1);
	}
	rpc = rpc_connect(target, port, "HWDB", 1l);
	if (rpc == NULL) {
		fprintf(stderr, "Error connecting to HWDB at %s:%05u\n", 
			target, port);
		exit(-1);
	}
	// Initialize link accumulator.
	lt_init();
	// and the thread that processes (inserts into hwdb) the accumulated results.
	if (pthread_create(&thr, NULL, handler, NULL)) {
		fprintf(stderr, "Failure to start database thread\n");
		exit(1);
	}
	/* ... and loop */ 
	pcap_loop(descr, -1, my_callback, args);
	fprintf(stderr, "\nfinished\n");
	pcap_close(descr);
	return 0;
}
