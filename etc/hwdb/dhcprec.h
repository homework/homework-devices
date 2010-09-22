/*
 * data structure associated with dhcp record
 */
#ifndef _DHCPREC_H_INCLUDED_
#define _DHCPREC_H_INCLUDED_

#include "timestamp.h"
#include "rtab.h"
/*
 * For the uint32_t,
 */
#include <stdint.h>
/*
 * For in_addr_t,
 */
#include <netinet/in.h>

typedef struct dhcp_data {
	unsigned int action; // 0:add 1:del 2:old
	uint64_t mac_addr;
	in_addr_t ip_addr;
	char hostname[80];
	tstamp_t tstamp;
} DhcpData;

/*
 * convert Rtab results into DhcpData
 */
DhcpData *dhcp_convert(Rtab *results);

/*
 * free heap storage associated with DhcpData
 */
void dhcp_free(DhcpData *p);

unsigned int action2index(char *action);
char *index2action(unsigned int index);

#endif /* _DHCPREC_H_INCLUDED_ */
