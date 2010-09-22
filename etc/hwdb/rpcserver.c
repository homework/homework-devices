/*
 * Homework DB server
 *
 * single-threaded provider of the Homework Database using SRPC
 * just has the logic to set up the RPC system
 *
 * expects SQL statement in input buffer, sends back results of
 * query in output buffer
 */

#include "config.h"
#include "srpc.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define USAGE "./hwdbserver [-p port]"

extern int log_allocation;

static char buf[SOCK_RECV_BUF_LEN];

int main(int argc, char *argv[]) {
	RpcConnection sender;
	unsigned len;
	RpcService rps;
	unsigned short port;
	int i, j;
	//Rtab *results;

	port = HWDB_SERVER_PORT;
	for (i = 1; i < argc; ) {
		if ((j = i + 1) == argc) {
			fprintf(stderr, "usage: %s\n", USAGE);
			exit(1);
		}
		if (strcmp(argv[i], "-p") == 0)
			port = atoi(argv[j]);
		else {
			fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
		}
		i = j + 1;
	}
	printf("initializing rpc system\n");
	if (!rpc_init(port)) {
		fprintf(stderr, "Failure to initialize rpc system\n");
		exit(-1);
	}
	printf("offering service\n");
	rps = rpc_offer("HWDB");
	if (! rps) {
		fprintf(stderr, "Failure offering HWDB service\n");
		exit(-1);
	}
	printf("starting to read queries from network\n");
	while ((len = rpc_query(rps, &sender, buf, SOCK_RECV_BUF_LEN)) > 0) {
		buf[len] = '\0';
		printf("Received: %s\n", buf);
		sprintf(buf, "1<|>Error<|>0<|>0<|>\n");
		len = strlen(buf) + 1;
		rpc_response(rps, sender, buf, len);
	}
	return 0;
}
