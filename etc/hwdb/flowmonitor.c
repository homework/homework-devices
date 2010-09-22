/*
 * flow monitor client for the Homework Database
 *
 * this is a sample timed client of the Homework Database, in this case
 * obtaining flow records; the program shows how to obtain the records,
 * how to map from port numbers to application protocols, and how to
 * map from IP addresses to host names
 *
 * usage: ./flowmonitor [-h host] [-p port] [-m ports] [-m hosts]
 *
 * periodically solicits all Flows records received by the database in the
 * period; converts the returned results to a binary array that can then
 * be processed and displayed to the user;
 *
 * displays the flow records received on standard output.
 *
 * if [-m ports] is specified, attempts to map the
 * port numbers associated with a flow to the IANA-defined application
 * protocol associated with one of those ports
 *
 * if [-m hosts] is specified, attempts to map the IP addresses to the
 * associated hostname
 *
 * if [-m proto] is specified, attempts to map the protocol numbers to the
 * associated names (e.g. 6 is TCP, 17 is UDP)
 *
 */

#include "config.h"
#include "util.h"
#include "rtab.h"
#include "srpc.h"
#include "timestamp.h"
#include "flowmonitor.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>
#include <arpa/inet.h>
#include <signal.h>
#include <netinet/in.h>
#include "portmap.h"
#include "hostmap.h"
#include "protomap.h"

#define USAGE "./flowmonitor [-h host] [-p port] [-m ports] [-m hosts] [-m proto]"
#define TIME_DELTA 5		/* in seconds */

static struct timespec time_delay = {TIME_DELTA, 0};
static int must_exit = 0;
static int exit_status = 0;
static int sig_received;
static int mapPorts;		/* true if mapping ports to appl protocols */
static int mapHosts;		/* true if mapping ip addresses to hostnames */
static int mapProto;		/* true if mapping protocol numbers to protocol names */

static tstamp_t processresults(char *buf, unsigned int len);

static void signal_handler(int signum) {
    must_exit = 1;
    sig_received = signum;
}

int main(int argc, char *argv[]) {
    RpcConnection rpc;
    char query[SOCK_RECV_BUF_LEN];
    char resp[SOCK_RECV_BUF_LEN];
    int qlen;
    unsigned len;
    char *host;
    unsigned short port;
    int i, j;
    struct timeval expected, current;
    tstamp_t last = 0LL;

    host = HWDB_SERVER_ADDR;
    port = HWDB_SERVER_PORT;
    mapPorts = 0;
    mapHosts = 0;
    mapProto = 0;
    for (i = 1; i < argc; ) {
        if ((j = i + 1) == argc) {
            fprintf(stderr, "usage: %s\n", USAGE);
            exit(1);
        }
        if (strcmp(argv[i], "-h") == 0)
            host = argv[j];
        else if (strcmp(argv[i], "-p") == 0)
            port = atoi(argv[j]);
	else if (strcmp(argv[i], "-m") == 0) {
	    if (strcmp(argv[j], "ports") == 0)
			mapPorts = 1;
		else if (strcmp(argv[j], "hosts") == 0)
			mapHosts = 1;
		else if (strcmp(argv[j], "proto") == 0)
			mapProto = 1;
	    else
		fprintf(stderr, "Don't know how to map %s\n", argv[j]);
	} else {
            fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
        }
        i = j + 1;
    }
    /* first attempt to connect to the database server */
    if (!rpc_init(0)) {
        fprintf(stderr, "Failure to initialize rpc system\n");
        exit(-1);
    }
    if (!(rpc = rpc_connect(host, port, "HWDB", 1l))) {
        fprintf(stderr, "Failure to connect to HWDB at %s:%05u\n", host, port);
        exit(-1);
    }
    if (mapPorts) {
	portmap_init(TCP, TCP_FILE);
	portmap_init(UDP, UDP_FILE);
    }
    if (mapHosts) {
        hostmap_init();
    }
    if (mapProto) {
        protomap_init(PROTO_FILE);
    }
    /* now establish signal handlers to gracefully exit from loop */
    if (signal(SIGTERM, signal_handler) == SIG_IGN)
        signal(SIGTERM, SIG_IGN);
    if (signal(SIGINT, signal_handler) == SIG_IGN)
        signal(SIGINT, SIG_IGN);
    if (signal(SIGHUP, signal_handler) == SIG_IGN)
        signal(SIGHUP, SIG_IGN);
    gettimeofday(&expected, NULL);
    expected.tv_usec = 0;
    while (! must_exit) {
        tstamp_t last_seen;
        expected.tv_sec += TIME_DELTA;
        if (last) {
            char *s = timestamp_to_string(last);
            sprintf(query,
    "SQL:select * from Flows [ range %d seconds] where timestamp > %s\n",
                    TIME_DELTA+1, s);
            free(s);
        } else
            sprintf(query, "SQL:select * from Flows [ range %d seconds]\n",
                    TIME_DELTA);
        qlen = strlen(query) + 1;
        gettimeofday(&current, NULL);
        if (current.tv_usec > 0) {
            time_delay.tv_nsec = 1000 * (1000000 - current.tv_usec);
            time_delay.tv_sec = expected.tv_sec - current.tv_sec - 1;
        } else {
            time_delay.tv_nsec = 0;
            time_delay.tv_sec = expected.tv_sec - current.tv_sec;
        }
        nanosleep(&time_delay, NULL);
        if (! rpc_call(rpc, query, qlen, resp, sizeof(resp), &len)) {
            fprintf(stderr, "rpc_call() failed\n");
            break;
        }
        resp[len] = '\0';
        if ((last_seen = processresults(resp, len)))
            last = last_seen;
    }
    rpc_disconnect(rpc);
    if (mapPorts) {
	portmap_free(TCP);
	portmap_free(UDP);
    }
    if (mapHosts) {
        hostmap_free();
    }
    if (mapProto) {
        protomap_free();
    }
    exit(exit_status);
}

/*
 * converts the returned Flows tuples into a dynamically-allocated array
 * of FlowData structures.  after the user is finished with the array,
 * mon_free should be called to return the storage to the heap
 *
 * Assumes that the Flow tuple is as defined by hwdb.rc - i.e.
 *
 * create table Flows (proto integer, saddr varchar(16), sport integer,
 * daddr varchar(16), dport integer, npkts integer, nbytes integer)
 */
FlowResults *mon_convert(Rtab *results) {
    FlowResults *ans;
    unsigned int i;

    if (! results || results->mtype != 0)
        return NULL;
    if (!(ans = (FlowResults *)malloc(sizeof(FlowResults))))
        return NULL;
    ans->nflows = results->nrows;
    ans->data = (FlowData **)calloc(ans->nflows, sizeof(FlowData *));
    if (! ans->data) {
        free(ans);
        return NULL;
    }
    for (i = 0; i < ans->nflows; i++) {
        char **columns;
        FlowData *p = (FlowData *)malloc(sizeof(FlowData));
        if (!p) {
            mon_free(ans);
            return NULL;
        }
        ans->data[i] = p;
        columns = rtab_getrow(results, i);
        p->tstamp = string_to_timestamp(columns[0]);
        p->proto = atoi(columns[1]) & 0xff;
        inet_aton(columns[2], (struct in_addr *)&p->ip_src);
        p->sport = atoi(columns[3]) & 0xffff;
        inet_aton(columns[4], (struct in_addr *)&p->ip_dst);
        p->dport = atoi(columns[5]) & 0xffff;
        p->packets = atol(columns[6]);
        p->bytes = atol(columns[7]);
    }
    return ans;
}

void mon_free(FlowResults *p) {
    unsigned int i;

    if (p) {
        for (i = 0; i < p->nflows && p->data[i]; i++)
            free(p->data[i]);
        free(p);
    }
}

static tstamp_t processresults(char *buf, unsigned int len) {
    Rtab *results;
    char stsmsg[RTAB_MSG_MAX_LENGTH];
    FlowResults *p;
    unsigned long i;
    tstamp_t last = 0LL;

    results = rtab_unpack(buf, len);
    if (results && ! rtab_status(buf, stsmsg)) {
        p = mon_convert(results);
        /* do something with the data pointed to by p */
        printf("Retrieved %ld flow records from database\n", p->nflows);
        for (i = 0; i < p->nflows; i++) {
            FlowData *f = p->data[i];
            char *s = timestamp_to_string(f->tstamp);
	    char *src;
	    char *dst;
	    char protocol[128];
	    if (mapProto) {
	        sprintf(protocol, "%s", protomap_classify(f->proto));
	    } else {
	        sprintf(protocol, "%u", f->proto);
	    }
	    if (mapHosts) {
		    src = hostmap_resolve(f->ip_src);
		    dst = hostmap_resolve(f->ip_dst);
            } else {
	            src = strdup(inet_ntoa(*(struct in_addr *)(&(f->ip_src))));
	            dst = strdup(inet_ntoa(*(struct in_addr *)(&(f->ip_dst))));
	    }
	    if (mapPorts) {
		char *app = portmap_classify(f->proto, f->sport, f->dport);
                printf("%s %s:%s:%s:%hu:%hu:%s:%lu:%lu\n", s, 
		   protocol,
	    	   src, dst,
		   f->sport, f->dport, app,
		   f->packets, f->bytes);
            } else {
                printf("%s %s:%s:%s:%hu:%hu:%lu:%lu\n", s,
		   protocol,
	    	   src, dst,
		   f->sport, f->dport,
		   f->packets, f->bytes);
	    }

	    free(s);
	    free(src);
	    free(dst);
        }
        if (i > 0) {
            i--;
            last = p->data[i]->tstamp;
        }
        mon_free(p);
    }
    rtab_free(results);
    return (last);
}
