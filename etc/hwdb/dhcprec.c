/*
 * Copy (and free) a single DHCP record into a DhcpData structure.
 * This is to be contrasted with flowrec.h or linkrec.h where such
 * methods are implemented in the aggregate (cf. flowmonitor.h).
 */

#include "dhcprec.h"

#include <stdlib.h>
#include <string.h>

#include "i8_parser.h"
#include <arpa/inet.h>

DhcpData *dhcp_convert(Rtab *results) {
	DhcpData *ans;
	if (! results || results->mtype != 0)
		return NULL;
	if (results->nrows != 1) {
		return NULL;
	}
	if (!(ans = (DhcpData *)malloc(sizeof(DhcpData))))
		return NULL;
	
	char **columns;
	columns = rtab_getrow(results, 0);
	
	/* populate record */
	ans->tstamp = string_to_timestamp(columns[0]);
	ans->action = action2index(columns[1]);
	ans->mac_addr = string_to_mac(columns[2]);
	inet_aton(columns[3], (struct in_addr *)&ans->ip_addr);	
	strcpy(ans->hostname, columns[4]);
	
	return ans;
}

void dhcp_free(DhcpData *p) {
	if (p) {
		free(p);
	}
}

unsigned int action2index(char *action) {
	if (strcmp(action, "add") == 0)
		return 0;
	else
	if (strcmp(action, "del") == 0)
		return 1;
	else
	if (strcmp(action, "old") == 0)
		return 2;
	else
		return 3;
}

char *index2action(unsigned int index) {
	if (index == 0)
		return "add";
	else
	if (index == 1)
		return "del";
	else
	if (index == 2)
		return "old";
	else
		return "unknown";
}
