#include "config.h"
#include "util.h"
#include "rtab.h"
#include "srpc.h"
#include "timestamp.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>
#include <arpa/inet.h>
#include <db.h>
#include <signal.h>

#include "linkmonitor.h"
#include "i8_parser.h"

#define USAGE "./linkmonitor [-h host] [-p port] -d database"
#define TIME_DELTA 10		/* in seconds */

static struct timespec time_delay = {TIME_DELTA, 0};
static DB *dbp;
static db_recno_t recno = 0;
static int must_exit = 0;
static int sig_received;
static int exit_status = 0;
static int ifDatabase;		/* true if persisting link records */

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
	char *database;
	unsigned short port;
	int i, j;
	struct timeval expected, current;
	tstamp_t last = 0LL;
	int ret;

	host = HWDB_SERVER_ADDR;
	port = HWDB_SERVER_PORT;
	ifDatabase = 0;
	for (i = 1; i < argc; ) {
		if ((j = i + 1) == argc) {
			fprintf(stderr, "usage: %s\n", USAGE);
			exit(1);
		}
		if (strcmp(argv[i], "-h") == 0)
			host = argv[j];
		else if (strcmp(argv[i], "-p") == 0)
			port = atoi(argv[j]);
		else if (strcmp(argv[i], "-d") == 0) {
			ifDatabase = 1;
			database = argv[j];
		} else {
			fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
		}
		i = j + 1;
	}
	if (! ifDatabase) {
	    fprintf(stderr, "usage: %s\n", USAGE);
	    exit(-1);
	}
	if (!rpc_init(0)) {
		fprintf(stderr, "Failure to initialize rpc system\n");
		exit(-1);
	}
	if (!(rpc = rpc_connect(host, port, "HWDB", 1l))) {
		fprintf(stderr, "Failure to connect to HWDB at %s:%05u\n",
				host, port);
		exit(-1);
	}
	if ((ret = db_create(&dbp, NULL, 0)) != 0) {
	    fprintf(stderr, "db_create: %s\n", db_strerror(ret));
	    rpc_disconnect(rpc);
	    exit(-1);
	}
	if ((ret = dbp->open(dbp, NULL, database, NULL, DB_RECNO,
	    		 DB_CREATE, 0664)) != 0) {
	    dbp->err(dbp, ret, "%s", database);
	    ret = dbp->close(dbp, 0);
	    rpc_disconnect(rpc);
	    exit(-1);
	}
	if (signal(SIGTERM, signal_handler) == SIG_IGN)
            signal(SIGTERM, SIG_IGN);
	if (signal(SIGINT, signal_handler) == SIG_IGN)
            signal(SIGINT, SIG_IGN);
	if (signal(SIGHUP, signal_handler) == SIG_IGN)
            signal(SIGHUP, SIG_IGN);
	gettimeofday(&expected, NULL);
	expected.tv_usec = 0;
	while(! must_exit) {
	    tstamp_t last_seen;
	    expected.tv_sec += TIME_DELTA;
	    if (last) {
		char *s = timestamp_to_string(last);
		sprintf(query,
	"SQL:select * from Links [ range %d seconds] where timestamp > %s\n",
	                TIME_DELTA+1, s);
		free(s);
	    } else
	        sprintf(query,
			"SQL:select * from Links [ range %d seconds]\n",
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
        printf("left loop, calling dbp->close\n");
	if ((ret = dbp->close(dbp, 0))) {
	    exit_status = -1;
	}
	rpc_disconnect(rpc);
	exit(exit_status);
}

LinkResults *link_mon_convert(Rtab *results) {
    LinkResults *ans;
    unsigned int i;

    if (! results || results->mtype != 0)
        return NULL;
    if (!(ans = (LinkResults *)malloc(sizeof(LinkResults))))
        return NULL;
    ans->nlinks = results->nrows;
    ans->data = (LinkData **)calloc(ans->nlinks, sizeof(LinkData *));
    if (! ans->data) {
        free(ans);
	return NULL;
    }
    for (i = 0; i < ans->nlinks; i++) {
        char **columns;
        LinkData *p = (LinkData *)malloc(sizeof(LinkData));
	if (!p) {
            link_mon_free(ans);
	    return NULL;
	}
	ans->data[i] = p;
	columns = rtab_getrow(results, i);
	p->tstamp = string_to_timestamp(columns[0]);
	// mac; rss; retries; packets; bytes.
	//p->mac = malloc(strlen(columns[1]+1));
	//strcpy(p->mac, columns[1]);
	p->mac = string_to_mac(columns[1]);
	p->rss = atof(columns[2]);
	p->retries = atoi(columns[3]);
	p->packets = atol(columns[4]);
	p->bytes = atol(columns[5]);
    }
    return ans;
}

void link_mon_free(LinkResults *p) {
    unsigned int i;

    if (p) {
        for (i = 0; i < p->nlinks && p->data[i]; i++)
            free(p->data[i]);
        free(p);
    }
}

static tstamp_t processresults(char *buf, unsigned int len) {
    Rtab *results;
    char stsmsg[RTAB_MSG_MAX_LENGTH];
    LinkResults *p;
    unsigned long i;
    tstamp_t last = 0LL;
    DBT key, data;
    int ret;

    results = rtab_unpack(buf, len);
    if (results && ! rtab_status(buf, stsmsg)) {
	// rtab_print(results);
	p = link_mon_convert(results);
	/* do something with the data pointed to by p */
	for (i = 0; i < p->nlinks; i++) {
	    LinkData *f = p->data[i];
	    recno++;
	    memset(&key, 0, sizeof(key));
	    memset(&data, 0, sizeof(data));
	    key.data = &recno;
	    key.size = sizeof(recno);
	    data.data = f;
	    data.size = sizeof(LinkData);
	    if ((ret = dbp->put(dbp, NULL, &key, &data, DB_APPEND))) {
	        dbp->err(dbp, ret, "DB->put error");
	        must_exit = 1;
	        exit_status = -1;
	        break;
	    }
	}
	if (i > 0) {
	    i--;
	    last = p->data[i]->tstamp;
	}
	link_mon_free(p);
    }
    rtab_free(results);
    return (last);
}
