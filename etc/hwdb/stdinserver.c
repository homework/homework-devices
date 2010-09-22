/*
 * Homework DB server
 *
 * single-threaded provider of the Homework Database using SRPC
 * just has the logic to start the database, obtaining queries from stdin
 * and printing results on stdout
 *
 * expects SQL statement in input buffer, sends back results of
 * query in output buffer
 */

#include "config.h"
#include "util.h"
#include "hwdb.h"
#include "rtab.h"
#include "mb.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define USAGE "./stdinserver [-l packets]"

extern int log_allocation;

static char buf[SOCK_RECV_BUF_LEN];

int main(int argc, char *argv[]) {
	unsigned len;
	int i, j;
	Rtab *results;
	int log;

	log = 0;
	for (i = 1; i < argc; ) {
		if ((j = i + 1) == argc) {
			fprintf(stderr, "usage: %s\n", USAGE);
			exit(1);
		}
		if (strcmp(argv[i], "-l") == 0) {
			if (strcmp(argv[j], "packets") == 0)
				log++;
			else
				fprintf(stderr, "usage: %s\n", USAGE);
		} else {
			fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
		}
		i = j + 1;
	}
	printf("initializing database\n");
	hwdb_init();
	printf("starting to read queries from stdin\n");
	//log_allocation = 1;
	j = 0;
	while (fgets(buf, sizeof(buf), stdin) != NULL) {
		len = strlen(buf) - 1;	/* get rid of \n */
		if (len == 0)
			continue;		/* ignore empty lines */
		buf[len] = '\0';
		j++;
		if (log)
			printf(">> %s\n", buf);
		results = hwdb_exec_query(buf);
		if (! results) {
		    sprintf(buf, "1<|>Error<|>0<|>0<|>\n");
		    len = strlen(buf) + 1;
		} else {
		    if (! rtab_pack(results, buf, SOCK_RECV_BUF_LEN, &i)) {
			    printf("query results truncated\n");
		    }
		    len = i;
		}
		if (log)
			printf("<< %s", buf);
		rtab_free(results);
		if (j >= 10000) {
			mb_dump();
			j = 0;
		}
	}
	return 0;
}
