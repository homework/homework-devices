/*
 * data structure associated with link record
 */
#ifndef _LINKREC_H_INCLUDED_
#define _LINKREC_H_INCLUDED_

#include "timestamp.h"
#include <stdint.h>

typedef struct link_data {
	uint64_t mac;
	double rss;
	unsigned long retries;
	unsigned long packets;
	unsigned long bytes;
	tstamp_t tstamp;
} LinkData;

#endif /* _LINKREC_H_INCLUDED_ */
