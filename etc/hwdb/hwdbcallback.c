/*
 * Callback client over SRPC
 *
 * main thread
 *   creates a service named Handler (or name provided in command line)
 *   spins off handler thread
 *   connects to Callback
 *   sends connect request to Callback server
 *   sleeps for 5 minutes
 *   sends disconnect request to Callback server
 *   exits
 *
 * handler thread
 *   for each received event message
 *     prints the received event message
 *     sends back a response of "OK"
 */

#include "config.h"
#include "util.h"
#include "rtab.h"
#include "srpc.h"
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#define USAGE "./hwdbcallback [-h host] [-p port] [-s service] [-t minutes] -q query"

/*
 * global data shared by main thread and handler thread
 */
static RpcService rps;

/*
 * handler thread
 */
static void *handler(void *args) {
    char event[SOCK_RECV_BUF_LEN], resp[100];
    RpcConnection sender;
    unsigned len, rlen;
    Rtab *results;

    while ((len = rpc_query(rps, &sender, event, SOCK_RECV_BUF_LEN)) > 0) {
	sprintf(resp, "OK");
        rlen = strlen(resp) + 1;
        rpc_response(rps, sender, resp, rlen);
	event[len] = '\0';
	printf("%s", event);
	results = rtab_unpack(event, len);
	rtab_print(results);
	rtab_free(results);
    }
    return (args) ? NULL : args;	/* unused warning subterfuge */
}

#define DELAY 5		/* number of minutes to delay before unsubscribing */

static struct timespec time_delay = {0, 0};

/*
 *   creates a service named Handler
 *   spins off handler thread
 *   connects to Callback
 *   sends connect request to Callback server
 *   sleeps for 5 minutes
 *   sends disconnect request to Callback server
 *   exits
 */
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
    char *query;
    int delay;

    target = HWDB_SERVER_ADDR;
    port = HWDB_SERVER_PORT;
    service = "Handler";
    query = NULL;
    delay = DELAY;
    for (i = 1; i < argc; ) {
	if ((j = i + 1) == argc) {
	    fprintf(stderr, "usage: %s\n", USAGE);
	    exit(1);
	}
	if (strcmp(argv[i], "-h") == 0)
	    target = argv[j];
	else if (strcmp(argv[i], "-p") == 0)
	    port = atoi(argv[j]);
	else if (strcmp(argv[i], "-t") == 0)
	    delay = atoi(argv[j]);
	else if (strcmp(argv[i], "-s") == 0)
	    service = argv[j];
	else if (strcmp(argv[i], "-q") == 0)
	    query = argv[j];
	else {
	    fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
	}
	i = j + 1;
    }
    if (query == NULL) {
	fprintf(stderr, "usage: %s\n", USAGE);
	exit(1);
    }
    time_delay.tv_sec = 60 * delay;
    /*
     * initialize the RPC system and offer the Callback service
     */
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
    /*
     * start handler thread
     */
    if (pthread_create(&thr, NULL, handler, NULL)) {
	fprintf(stderr, "Failure to start timer thread\n");
	exit(-1);
    }
    /*
     * connect to HWDB service
     */
    rpc = rpc_connect(target, port, "HWDB", 1l);
    if (rpc == NULL) {
	fprintf(stderr, "Error connecting to HWDB at %s:%05u\n", target, port);
	exit(1);
    }
    /*
     * Generate query name
     */
    sprintf(qname, "CBQuery%d", getpid());
    /*
     * delete query (if it exists)
     */
    sprintf(question, "SQL:delete query %s", qname);
    if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
	fprintf(stderr, "Error issuing delete command\n");
	exit(1);
    }
    resp[rlen] = '\0';
    printf("Response to delete command: %s", resp);
    /*
     * save query
     */
    sprintf(question, "SQL:save (%s) as %s", query, qname);
    if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
	fprintf(stderr, "Error issuing save command\n");
	exit(1);
    }
    resp[rlen] = '\0';
    printf("Response to save command: %s", resp);
    /*
     * now subscribe to query
     */
    sprintf(question, "SQL:subscribe %s %s %hu %s", qname, myhost, myport, service);
    if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
	fprintf(stderr, "Error issuing subscribe command\n");
	exit(1);
    }
    resp[rlen] = '\0';
    printf("Response to subscribe command: %s", resp);
    /*
     * sleep for 5 minutes
     */
    nanosleep(&time_delay, NULL);
    /*
     * issue unsubscribe command
     */
    sprintf(question, "SQL:unsubscribe %s %s %hu %s", qname, myhost, myport, service);
    if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
	fprintf(stderr, "Error issuing unsubscribe command\n");
	exit(1);
    }
    resp[rlen] = '\0';
    printf("Response to unsubscribe command: %s", resp);
    /*
     * delete query
     */
    sprintf(question, "SQL:delete query %s", qname);
    if (!rpc_call(rpc, question, strlen(question)+1, resp, 100, &rlen)) {
	fprintf(stderr, "Error issuing delete command\n");
	exit(1);
    }
    resp[rlen] = '\0';
    printf("Response to delete command: %s", resp);
    /*
     * now disconnect from server
     */
    rpc_disconnect(rpc);
    exit(0);
}
