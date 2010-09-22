/*
 * data structure associated with IP flows
 */
#ifndef _FLOWREC_H_INCLUDED_
#define _FLOWREC_H_INCLUDED_

#include <netinet/in.h>			/* defines in_addr_t */
#include "timestamp.h"

typedef struct flow_data {
    unsigned char proto;
    in_addr_t ip_src;
    in_addr_t ip_dst;
    unsigned short sport;
    unsigned short dport;
    unsigned long packets;
    unsigned long bytes;
    tstamp_t tstamp;
} FlowData;

#endif /* _FLOWREC_H_INCLUDED_ */
