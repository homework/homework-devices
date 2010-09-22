/*
 * program to dump contents of flow database
 */
#include "flowrec.h"
#include "timestamp.h"
#include <sys/types.h>
#include <stdio.h>
#include <string.h>
#include <db.h>
#include <sys/time.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include "portmap.h"
#include "hostmap.h"
#include "protomap.h"

#define USAGE "./flowdump [-d databasename] [-m ports] [-b datestring] [-e datestring]"
#define FLOW_DATABASE "flow.db"

int main(int argc, char *argv[]) {
    int i, j;
    char *database;
    DB *dbp;
    DBT key, data;
    DBC *dbcp;
    int ret;
    FlowData fr;
    int mapPorts;
    int mapHosts;
    int mapProto;
    tstamp_t tbegin, tend;
    int ifBegin, ifEnd;

    database = FLOW_DATABASE;
    mapPorts = 0;
    mapHosts = 0;
    mapProto = 0;
    ifBegin = 0;
    ifEnd = 0;
    for (i = 1; i < argc; ) {
        if ((j = i + 1) == argc) {
            fprintf(stderr, "usage: %s\n", USAGE);
            exit(1);
        }
        if (strcmp(argv[i], "-d") == 0)
            database = argv[j];
	else if (strcmp(argv[i], "-m") == 0) {
            if (strcmp(argv[j], "ports") == 0)
				mapPorts = 1;
			else if (strcmp(argv[j], "hosts") == 0)
				mapPorts = 1;
			else if (strcmp(argv[j], "proto") == 0)
				mapProto = 1;
	    else {
                fprintf(stderr, "Don't know how to map %s\n", argv[j]);
	    }
	} else if (strcmp(argv[i], "-b") == 0) {
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
    data.data = &fr;
    data.ulen = sizeof(fr);
    while ((ret = dbcp->get(dbcp, &key, &data, DB_NEXT)) == 0) {
	if (ifBegin && (fr.tstamp < tbegin))
	    continue;
	if (ifEnd && (fr.tstamp > tend))
	    continue;
	char *s = timestamp_to_string(fr.tstamp);
	char *src;
	char *dst;
	char *app = "unclassified";
	if (mapPorts) {
            app = portmap_classify(fr.proto, fr.sport, fr.dport);
	}
	if (mapHosts) {
            src = hostmap_resolve(fr.ip_src);
            dst = hostmap_resolve(fr.ip_dst);
	} else {
	    src = strdup(inet_ntoa(*(struct in_addr *)(&(fr.ip_src))));
	    dst = strdup(inet_ntoa(*(struct in_addr *)(&(fr.ip_dst))));
	}
	if (mapProto) {
	    char *protocol = protomap_classify(fr.proto);
        printf("%s %s:%s:%s:%04hx:%04hx:%s:%lu:%lu\n", s, protocol,
	    src, dst,
	    fr.sport, fr.dport, app,
	    fr.packets, fr.bytes);
	} else {
        printf("%s %u:%s:%s:%04hx:%04hx:%s:%lu:%lu\n", s, fr.proto,
	    src, dst,
	    fr.sport, fr.dport, app,
	    fr.packets, fr.bytes);
	}

	free(s);
	free(src);
	free(dst);
    }
    if (ret != DB_NOTFOUND)
	dbp->err(dbp, ret, "DBcursor->get");
    if ((ret = dbcp->close(dbcp)) != 0)
	dbp->err(dbp, ret, "DBcursor->close");
err:
    ret = dbp->close(dbp, 0);
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
    exit(ret);
}
