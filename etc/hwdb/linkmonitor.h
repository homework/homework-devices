/*
 * interface and data structures associated with link monitor
 */
#ifndef _LINKMONITOR_H_INCLUDED_
#define _LINKMONITOR_H_INCLUDED_

#include "linkrec.h"
#include "rtab.h"

typedef struct link_results {
    unsigned long nlinks;
    LinkData **data;
} LinkResults;

/*
 * convert Rtab results into LinkResults
 */
LinkResults *link_mon_convert(Rtab *results);

/*
 * free heap storage associated with LinkResults
 */
void link_mon_free(LinkResults *p);

#endif /* _LINKMONITOR_H_INCLUDED_ */
