/*
 * The Homework Database
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */
#ifndef HWDB_PUBSUB_H
#define HWDB_PUBSUB_H

#include "srpc.h"

typedef struct subscription {
	char *queryname;
	char *ipaddr;
	char *port;
	char *service;
	RpcConnection rpc;
} Subscription;

#endif
