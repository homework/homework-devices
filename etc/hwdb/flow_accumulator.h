/*
 * interface and data structures associated with flow accumulator
 */
#ifndef _FLOWACCUMULATOR_H_INCLUDED_
#define _FLOWACCUMULATOR_H_INCLUDED_

#include <netinet/ip.h>			/* defines in_addr */

typedef struct flow_rec {
    struct in_addr ip_src;
    struct in_addr ip_dst;
    unsigned short sport;
    unsigned short dport;
    unsigned char proto;
} FlowRec;

typedef struct acc_elem {
    struct acc_elem *next;
    FlowRec id;
    unsigned long packets;
    unsigned long bytes;
} AccElem;

/*
 * initialize the data structures
 */
void acc_init();

/*
 * update packet and byte counts associated with particular flow
 * creating flow record if first time flow has been seen
 */
void acc_update(struct in_addr src, struct in_addr dst,
		unsigned short sport, unsigned short dport,
		unsigned long proto, unsigned long bytes);

/*
 * swap accumulator tables, returning previous table for processing
 */
AccElem **acc_swap(int *size, unsigned long *nelems);

/*
 * add chain of elements to free list
 */
void acc_freeChain(AccElem *elem);

#endif /* _FLOWACCUMULATOR_H_INCLUDED_ */
