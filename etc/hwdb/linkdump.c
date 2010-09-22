/*
 * program to dump contents of link database
 */
#include "timestamp.h"
#include "linkrec.h"
#include <sys/types.h>
#include <stdio.h>
#include <string.h>
#include <db.h>
#include <sys/time.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <netinet/in.h>

#define USAGE "./linkdump [-d databasename] [-b datestring] [-e datestring]"
#define LINK_DATABASE "link.db"

int main(int argc, char *argv[]) {
    int i, j;
    char *database;
    DB *dbp;
    DBT key, data;
    DBC *dbcp;
    int ret;
    LinkData lr;
    tstamp_t tbegin, tend;
    int ifBegin, ifEnd;

    database = LINK_DATABASE;
    ifBegin = 0;
    ifEnd = 0;
    for (i = 1; i < argc; ) {
        if ((j = i + 1) == argc) {
            fprintf(stderr, "usage: %s\n", USAGE);
            exit(1);
        }
        if (strcmp(argv[i], "-d") == 0)
            database = argv[j];
	else if (strcmp(argv[i], "-b") == 0) {
	    ifBegin = 1;
	    tbegin = datestring_to_timestamp(argv[j]);
	} else if (strcmp(argv[i], "-e") == 0) {
	    ifEnd = 1;
	    tend = datestring_to_timestamp(argv[j]);
	} else {
            fprintf(stderr, "Unknown flag: %s %s\n", argv[i], argv[j]);
        }
        i = j + 1;
    }
    if ((ret = db_create(&dbp, NULL, 0)) != 0) {
        fprintf(stderr, "db_create: %s\n", db_strerror(ret));
        exit(1);
    }
    if ((ret = dbp->open(dbp, NULL, database, NULL,
			 DB_RECNO, 0, 0664)) != 0) {
        dbp->err(dbp, ret, "%s", database);
        goto err;
    }
    if ((ret = dbp->cursor(dbp, NULL, &dbcp, 0)) != 0) {
        dbp->err(dbp, ret, "DB->cursor");
	goto err;
    }
    memset(&key, 0, sizeof(key));
    memset(&data, 0, sizeof(data));
    data.flags = DB_DBT_USERMEM;
    data.data = &lr;
    data.ulen = sizeof(lr);
    while ((ret = dbcp->get(dbcp, &key, &data, DB_NEXT)) == 0) {
	if (ifBegin && (lr.tstamp < tbegin))
	    continue;
	if (ifEnd && (lr.tstamp > tend))
	    continue;
        char *s = timestamp_to_string(lr.tstamp);

	printf("%s %llx;%.2f;%lu;%lu;%lu\n", s, lr.mac, lr.rss, lr.retries,
	       lr.packets, lr.bytes);
	free(s);
    }
    if (ret != DB_NOTFOUND)
	dbp->err(dbp, ret, "DBcursor->get");
    if ((ret = dbcp->close(dbcp)) != 0)
	dbp->err(dbp, ret, "DBcursor->close");
err:
    ret = dbp->close(dbp, 0);
    exit(ret);
}
