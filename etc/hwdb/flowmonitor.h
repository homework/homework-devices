/*
 * interface and data structures associated with flow monitor
 */
#ifndef _FLOWMONITOR_H_INCLUDED_
#define _FLOWMONITOR_H_INCLUDED_

#include "flowrec.h"
#include <netinet/ip.h>			/* defines in_addr */
#include "timestamp.h"

typedef struct flow_results {
    unsigned long nflows;
    FlowData **data;
} FlowResults;

/*
 * convert Rtab results into FlowResults
 */
FlowResults *mon_convert(Rtab *results);

/*
 * free heap storage associated with FlowResults
 */
void mon_free(FlowResults *p);

#endif /* _FLOWMONITOR_H_INCLUDED_ */
