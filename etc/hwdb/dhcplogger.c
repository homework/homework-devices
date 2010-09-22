#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "srpc.h"
#include "config.h"
#include "rtab.h"
#include <db.h>

/*
 * 1. store events in circular buffer;
 * 2. store events persistently, in BerkeleyDB; or
 * 3. simply store events in a text file;
 */

#define DATABASE_NAME "data/dhcp.db"
#define TEXTFILE_NAME "data/dhcp.txt"

int store_in_bdb(); // In BerkeleyDB, or
int store_in_txt(); // in text file.

int main(int argc, char *argv[]) {
	int i;
	char *host;
	unsigned short port;
	RpcConnection rpc;
	char query[SOCK_RECV_BUF_LEN];
	char resp[SOCK_RECV_BUF_LEN];
	host = HWDB_SERVER_ADDR;
	port = HWDB_SERVER_PORT;
	unsigned int bytes;
	unsigned int len;
	char stsmsg[RTAB_MSG_MAX_LENGTH];
	
	/*
	DB *dbp;
	db_recno_t recno = 0;
	int ret;
	*/

	/* check for correct number and type of arguments */
	if (argc != 5) {
                fprintf(stderr, "dnsmasq does not comply with hwdb schema.\n");
		return 0;
	} else if (strcmp(argv[1], "old") == 0) {
		/* This ignores any events when the DHCP (dnsmasq) server restarts. */
		return 0;
	}

	/* Connect to HWDB server */
	if (!rpc_init(0)) {
                fprintf(stderr, "Failure to initialize rpc system\n");
		return 0;
	}
	if (!(rpc = rpc_connect(host, port, "HWDB", 1l))) {
                fprintf(stderr, "Failure to connect to HWDB at %s:%05u\n",
			host, port);
		return 0;
	}

	/* process event (lease info) */
	bytes = 0;
	bytes += sprintf(query + bytes, "SQL:insert into Leases values (" );
	for (i = 1; i < argc; i++) {
		bytes += sprintf(query + bytes, "\"%s\"%s", argv[i],
				 (i < argc-1) ? ", " : ")\n");
	}
	if (! rpc_call(rpc, query, bytes, resp, sizeof(resp), &len)) {
		fprintf(stderr, "rpc_call() failed\n");
		rpc_disconnect(rpc);
		return 0;
	}
	resp[len] = '\0';
	if (rtab_status(resp, stsmsg))
		fprintf(stderr, "RPC error: %s\n", stsmsg);
	rpc_disconnect(rpc);

	return 0;
}

int store_in_bdb() {
	return 0;
}

int store_in_txt() {
	return 0;
}
