/*
 * flow persist client for the Homework Database
 *
 * this is a sample timed client of the Homework Database, in this case
 * obtaining flow records; the program shows how to obtain the records and
 * how store them persistently in a Berkeley DB database file
 *
 * usage: ./flowpersist [-h host] [-p port] -d databasefile
 *
 * periodically solicits all Flows records received by the database in the
 * period; converts the returned results to a binary array that can then
 * be stored in the database
 *
 * -d databasefile must be specified, specifying the file to which the
 * records shoule be stored, appending to it if it already exists
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
#include <db.h>
#include <signal.h>
#include <netinet/in.h>

#define USAGE "./flowmonitor [-h host] [-p port] [-d databasename] [-m ports]"
#define TIME_DELTA 5		/* in seconds */

static struct timespec time_delay = {TIME_DELTA, 0};
static DB *dbp;
static db_recno_t recno = 0;
static int must_exit = 0;
static int exit_status = 0;
static int sig_received;
static int ifDatabase;		/* true if -d flag processed */

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
    /* first attempt to connect to the database server */
    if (!rpc_init(0)) {
        fprintf(stderr, "Failure to initialize rpc system\n");
        exit(-1);
    }
    if (!(rpc = rpc_connect(host, port, "HWDB", 1l))) {
        fprintf(stderr, "Failure to connect to HWDB at %s:%05u\n", host, port);
        exit(-1);
    }
    /* then attempt to open the database file in append mode */
    if ((ret = db_create(&dbp, NULL, 0)) != 0) {
        fprintf(stderr, "db_create: %s\n", db_strerror(ret));
        rpc_disconnect(rpc);
        exit(-1);
    }
    if ((ret = dbp->open(dbp, NULL, database, NULL, DB_RECNO, DB_CREATE, 0664)) != 0) {
        dbp->err(dbp, ret, "%s", database);
        ret = dbp->close(dbp, 0);
        rpc_disconnect(rpc);
        exit(-1);
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
    printf("left loop, calling dbp->close\n");
    if ((ret = dbp->close(dbp, 0))) {
        exit_status = -1;
    }
    rpc_disconnect(rpc);
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
    DBT key, data;
    int ret;

    results = rtab_unpack(buf, len);
    if (results && ! rtab_status(buf, stsmsg)) {
        p = mon_convert(results);
        for (i = 0; i < p->nflows; i++) {
            FlowData *f = p->data[i];
            recno++;
	    memset(&key, 0, sizeof(key));
	    memset(&data, 0, sizeof(data));
	    key.data = &recno;
	    key.size = sizeof(recno);
	    data.data = f;
	    data.size = sizeof(FlowData);
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
        mon_free(p);
    }
    rtab_free(results);
    return (last);
}
