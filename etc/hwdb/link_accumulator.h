#ifndef _LINK_ACCUMULATOR_H_INCLUDED_
#define _LINK_ACCUMULATOR_H_INCLUDED_

/* 
 * The primary key of a link table record is the MAC address of the sender.
 *
 * It follows the "flow_accumulator.h" structure.
 */

#include <stdint.h>

typedef struct link_rec {
	uint64_t mac;
} LinkRec;

typedef struct lt_element {
	struct lt_element *next;
	LinkRec id;
	double avg_rss;
	// double std_rss;
	unsigned long counter; 		/* Used for the moving average. */
	unsigned long retries; 		/* Another indicator of quality (or packet loss). */
	unsigned long packets; 		/* Number of packets accumulated by the sniffer. */
	unsigned long nobytes;
} LinkTableElem;

void lt_init();

void lt_update(uint64_t addr, int rss, int retry, unsigned long bytes);

LinkTableElem **lt_swap(int *size, unsigned long *nelems);

void lt_freeChain(LinkTableElem *elem);

#endif /* _LINK_ACCUMULATOR_H_INCLUDED_ */
