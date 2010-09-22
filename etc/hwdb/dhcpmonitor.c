/*
 * DHCP callback client for the Homework Database. It displays events
 * on standard output.
 */

#include "util.h"
#include "rtab.h"
#include "srpc.h"
#include "config.h"
#include "i8_parser.h"
#include "timestamp.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <time.h>
#include <sys/time.h>

#include <arpa/inet.h>

#include <signal.h>

#include <pthread.h>

#include "dhcprec.h"

#define USAGE "./dhcpmonitor [-h host] [-p port]"

static RpcService rps;

static int exit_status = 0;
static int sig_received;

static void *handler(void *args) {
	char event[SOCK_RECV_BUF_LEN], resp[100];
	char stsmsg[RTAB_MSG_MAX_LENGTH];
	RpcConnection sender;
	unsigned len, rlen;
	Rtab *results;
	DhcpData *dr;
	while ((len = rpc_query(rps, &sender, event, SOCK_RECV_BUF_LEN)) > 0) {
		sprintf(resp, "OK");
		rlen = strlen(resp) + 1;
		rpc_response(rps, sender, resp, rlen);
		event[len] = '\0';
		results = rtab_unpack(event, len);
		if (results && ! rtab_status(event, stsmsg)) {
			
			/* 
 			 * 
 			 * Do process */

			dr = dhcp_convert(results);
			
			/* print results - actually, this is dhcpmonitor's functionality. */
			char *s = timestamp_to_string(dr->tstamp);
			char *a = strdup(inet_ntoa(*(struct in_addr *)&dr->ip_addr));
			printf( "%s %s;%012llx;%s;%s\n", 
s, index2action(dr->action), dr->mac_addr, a, dr->hostname);
			free(s);
			free(a);
			
			dhcp_free(dr);
		}
		rtab_free(results);
	}
	return (args) ? NULL : args;	/* unused warning subterfuge */
}

static void signal_handler(int signum) {
	sig_received = signum;
}

int main(int argc, char *argv[]) {
	RpcConnection rpc;
	unsigned rlen;
	char question[1000], resp[100], myhost[100], qname[64];
	unsigned short myport;
	pthread_t thr;
	int i, j;

	unsigned short port;
	char *target;
	char *service;

	sigset_t mask, oldmask;

	target = HWDB_SERVER_ADDR;
	port = HWDB_SERVER_PORT;
	service = "LeasesMonitorHandler";

	for (i = 1; i < argc; ) {
		if ((j = i + 1) == argc) {
			fprintf(stderr, "usage: %s\n", USAGE);
			exit(1);
		}
		if (strcmp(argv[i], "-h") == 0)
			target = argv[j];
		else if (strcmp(argv[i], "-p") == 0)
			port = atoi(argv[j]);
		else {
			fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
		}
		i = j + 1;
	}

	/* initialize the RPC system and offer the Callback service */
	if (!rpc_init(0)) {
		fprintf(stderr, "Initialization failure for rpc system\n");
		exit(-1);
	}
	rps = rpc_offer(service);
	if (! rps) {
		fprintf(stderr, "Failure offering %s service\n", service);
		exit(-1);
	}
	rpc_details(myhost, &myport);
	
	/* connect to HWDB service */
	rpc = rpc_connect(target, port, "HWDB", 1l);
	if (rpc == NULL) {
		fprintf(stderr, "Error connecting to HWDB at %s:%05u\n", target, port);
		exit(1);
	}
	
	sprintf(qname, "LeasesLast");
	/* subscribe to query 'qname' */
	sprintf(question, "SQL:subscribe %s %s %hu %s", 
		qname, myhost, myport, service);
	if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
		fprintf(stderr, "Error issuing subscribe command\n");
		exit(1);
	}
	resp[rlen] = '\0';
	printf("Response to subscribe command: %s", resp);

	/* start handler thread */
	if (pthread_create(&thr, NULL, handler, NULL)) {
		fprintf(stderr, "Failure to start handler thread\n");
		exit(-1);
	}

	/* establish signal handlers to gracefully exit from loop */
	if (signal(SIGTERM, signal_handler) == SIG_IGN)
		signal(SIGTERM, SIG_IGN);
	if (signal(SIGINT, signal_handler) == SIG_IGN)
		signal(SIGINT, SIG_IGN);
	if (signal(SIGHUP, signal_handler) == SIG_IGN)
		signal(SIGHUP, SIG_IGN);

	sigemptyset(&mask);
	sigaddset(&mask, SIGINT);
	sigaddset(&mask, SIGHUP);
	sigaddset(&mask, SIGTERM);
	/* suspend until signal */
	sigprocmask(SIG_BLOCK, &mask, &oldmask);
	while (!sig_received)
		sigsuspend(&oldmask);
	sigprocmask(SIG_UNBLOCK, &mask, NULL);
	
	/* issue unsubscribe command */
	sprintf(question, "SQL:unsubscribe %s %s %hu %s", 
		qname, myhost, myport, service);
	if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
		fprintf(stderr, "Error issuing unsubscribe command\n");
		exit(1);
	}
	resp[rlen] = '\0';
	printf("Response to unsubscribe command: %s", resp);
    
	/* disconnect from server */
	rpc_disconnect(rpc);
	exit(exit_status);
}

