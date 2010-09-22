/**
 * nodecrawler.h
 *
 * Node Crawler
 *
 * Created by Oliver Sharma on 2009-05-06
 * Copyright (c) 2009. All rights reserved.
 */
#include "nodecrawler.h"

#include "util.h"
#include "list.h"
#include "rtab.h"
#include "sqlstmts.h"
#include "table.h"
#include "tuple.h"
#include "timestamp.h"

#include <string.h>
#include <sys/time.h>
#include <stdlib.h>

#define set_dropped(node) {\
	(node)->tstamp |= DROPPED; }
#define clr_dropped(node) {\
	(node)->tstamp &= ~DROPPED; }
#define is_dropped(node) ((node)->tstamp & DROPPED)

Nodecrawler *nodecrawler_new(Node *first, Node *last) {
	Nodecrawler *nc;
	
	nc = mem_alloc(sizeof(Nodecrawler));
	nc->first = first;
	nc->last = last;
	nc->current = first;
	nc->empty = 0; /*false*/
	
	if (!first && !last) {
		debugvf("Nodecrawler: empty list!\n");
		nc->empty = 1; /*true*/
	}
	
	return nc;
}

Nodecrawler *nodecrawler_new_from_window(Table *tbl, sqlwindow *win) {
	Nodecrawler *nc;
	
	nc = nodecrawler_new(tbl->oldest, tbl->newest);
	
	nodecrawler_apply_window(nc, win);
	
	return nc;
}

void nodecrawler_free(Nodecrawler *nc) {
	mem_free(nc);
}

void nodecrawler_apply_tuplewindow(Nodecrawler *nc, sqlwindow *win) {
	int i;
	Node *tmp;

	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}

	if (win->num < 0) {
		errorf("Nodecrawler: window with invalid row number: %d\n", win->num);
		return;
	}
	
	tmp = nc->last;
	
	i = win->num;
	while (i>1 && tmp->prev != NULL) {
		debugvf("Nodecrawler: moving first window back by one\n");
		tmp = tmp->prev;
		i--;
	}
	
	/* Reached last now. Since we're going backwards, this will be first */
	nc->first = tmp;
	
	/* Might as well reset current at this stage */
	nodecrawler_set_to_start(nc);
}

/*
 * in_time_window - returns true/false if 'then' is in the time window of
 * 		    [now-units, now]
 *
 * returns 1 if now - then <= units
 *         0               >  units
 *
 * units is interpreted as seconds if 'ifmillis' == 0
 *                         millis                   1
 */
static int in_time_window(tstamp_t now, tstamp_t then,
		          unsigned long units, int ifmillis) {
	tstamp_t incr;

	incr = timestamp_add_incr(then, units, ifmillis);
	return (now <= incr);
}

void nodecrawler_apply_timewindow(Nodecrawler *nc, sqlwindow *win) {
	struct timeval now;
	Node *tmp;
	int units;
	int ifmillis = 0;
	tstamp_t nowts;
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	/* Get current time */
	if (gettimeofday(&now, NULL) != 0) {
		errorf("gettimeofday() failed. Unable to apply time window\n");
		return;
	}
	nowts = timeval_to_timestamp(&now);
		
	/* Convert given units into seconds or milliseconds */
	switch (win->unit) {
		
		/* Special case NOW window (i.e. only tuples with equivalent timestamp)*/
		case SQL_WINTYPE_TIME_NOW:
		units = 0;
		break;
		
		case SQL_WINTYPE_TIME_HOURS:
		units = win->num * 3600;
		break;
		
		case SQL_WINTYPE_TIME_MINUTES:
		units = win->num * 60;
		break;
		
		case SQL_WINTYPE_TIME_SECONDS:
		units = win->num;
		break;
		
		case SQL_WINTYPE_TIME_MILLIS:
		units = win->num;
		ifmillis = 1;
		break;
		
		default:
		errorf("Unknown unit format in nodecrawler_apply_timewindow");
		return;
		break;
		
	}
		
	/* Loop backwards from last to first */
	tmp = nc->last;
	
	if (! in_time_window(nowts, tmp->tstamp, units, ifmillis)) {
		debugf("Nodecrawler: Nothing fits into this time window.\n");
		nc->first=nc->last;
		nc->current=nc->last;
		nc->empty = 1; /*true*/
		return;
	}
	
	while (tmp->prev != nc->first->prev) {
		
		if (! in_time_window(nowts, tmp->tstamp, units, ifmillis)) {
			tmp = tmp->next;
			break;
		}
		
		debugvf("Timestamp in range. Moving back by one\n");
		tmp = tmp->prev;
	}
	
	/* Reached last tuple where timestamp matches. Since we're going
	 * backwards, this last one will be nc->first
	 */
	nc->first = tmp;
	
	/* Might as well reset current at this stage */
	nodecrawler_set_to_start(nc);
}

void nodecrawler_apply_window(Nodecrawler *nc, sqlwindow *win) {
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}

	switch(win->type) {
		
		case SQL_WINTYPE_TPL:
		debugvf("Nodecrawler: wintype_tpl\n");
		nodecrawler_apply_tuplewindow(nc, win);
		break;
		
		case SQL_WINTYPE_TIME:
		debugvf("Nodecrawler: wintype_time\n");
		nodecrawler_apply_timewindow(nc, win);
		break;
		
		case SQL_WINTYPE_NONE:
		debugvf("Nodecrawler: wintype_none. doing nothing\n");
		break;
		
		default:
		errorf("Nodecrawler encountered invalid window type\n");
		break;
	}

}

static int compare(int op, void *cVal, int *cType, union filterval *filVal) {
    if (cType == PRIMTYPE_INTEGER) {
        int val = atoi((char *)cVal);
	switch(op) {
	    case SQL_FILTER_EQUAL:
		return (val == filVal->intv); break;
	    case SQL_FILTER_GREATER:
		return (val > filVal->intv); break;
	    case SQL_FILTER_LESS:
		return (val < filVal->intv); break;
	    case SQL_FILTER_LESSEQ:
		return (val <= filVal->intv); break;
	    case SQL_FILTER_GREATEREQ:
		return (val >= filVal->intv); break;
	}
    } else if (cType == PRIMTYPE_REAL) {
	double val = strtod((char *)cVal, NULL);
	switch(op) {
	    case SQL_FILTER_EQUAL:
		return (val == filVal->realv); break;
	    case SQL_FILTER_GREATER:
		return (val > filVal->realv); break;
	    case SQL_FILTER_LESS:
		return (val < filVal->realv); break;
	    case SQL_FILTER_LESSEQ:
		return (val <= filVal->realv); break;
	    case SQL_FILTER_GREATEREQ:
		return (val >= filVal->realv); break;
	}
    }else if (cType == PRIMTYPE_TIMESTAMP) {
	unsigned long long val = *(tstamp_t *)cVal;
	switch(op) {
	    case SQL_FILTER_EQUAL:
		return (val == filVal->tstampv); break;
	    case SQL_FILTER_GREATER:
		return (val > filVal->tstampv); break;
	    case SQL_FILTER_LESS:
		return (val < filVal->tstampv); break;
	    case SQL_FILTER_LESSEQ:
		return (val <= filVal->tstampv); break;
	    case SQL_FILTER_GREATEREQ:
		return (val >= filVal->tstampv); break;
	}
    }
    return 0;			/* false if we get here */
}

int passed_filter(Node *n, Table *tn, int nfilters, sqlfilter **filters, int filtertype) {
	int i;
	char *filName;
	union filterval filVal;
	int filSign;
	int colIdx;
	void *colVal;
	int *colType;
	union Tuple *p = (union Tuple *)(n->tuple);

	for (i = 0; i < nfilters; i++) {
		
		filName = filters[i]->varname;
		memcpy(&filVal, &filters[i]->value, sizeof(union filterval));
		filSign = filters[i]->sign;
		
		colIdx = table_lookup_colindex(tn, filName);	
		if (colIdx == -1)  { /* i.e. invalid variable name */
			if (strcmp(filName, "timestamp") == 0) {
				colVal = (void *)&(n->tstamp);
				colType = PRIMTYPE_TIMESTAMP;
			} else {
				errorf("Invalid column name in filter: %s\n", filName);
				return 1;       /* automatically pass this filter */
			}
		} else {
			colType = tn->coltype[colIdx];
			colVal = (void *)(p->ptrs[colIdx]);
		}
		
		if (filtertype == SQL_FILTER_TYPE_OR) {
			
			debugf("Filtertype in nodecrawler is OR\n");
			
			if (compare(filSign, colVal, colType, &filVal)) {
				return 1;
			}
			
		} else { /* default to AND filter */
		
			if (! compare(filSign, colVal, colType, &filVal)) {
				return 0;
			}
		}
		
	}
	
	/* If we get here, all filters passed */
	return 1;
}

void nodecrawler_apply_filter(Nodecrawler *nc, Table *tn, int nfilters,
		              sqlfilter **filters, int filtertype) {
		
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	nodecrawler_set_to_start(nc);
	while(nodecrawler_has_more(nc)) {
		
		if (!passed_filter(nc->current, tn, nfilters, filters, filtertype)) {
			set_dropped(nc->current);
		}
		
		nodecrawler_move_to_next(nc);
	}
	
}

void nodecrawler_set_to_start(Nodecrawler *nc) {
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	nc->current = nc->first;
	
	if (is_dropped(nc->current))
		nodecrawler_move_to_next(nc);
}

int nodecrawler_has_more(Nodecrawler *nc) {
	
	return (!nc->empty && nc->current != nc->last->next);
}

void nodecrawler_move_to_next(Nodecrawler *nc) {
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	if (!nodecrawler_has_more(nc))
		return;
	
	nc->current = nc->current->next;
	
	while (nodecrawler_has_more(nc) && is_dropped(nc->current)) {
		nc->current = nc->current->next;
	}
}

/* NB: dangerous method if last does not follow first in chain */
int nodecrawler_count_nondropped(Nodecrawler *nc) {
	Node *tmp;
	int count;
	
	if (nc->empty) {
		return 0;
	}
	
	tmp = nc->first;
	count = 0;
	
	while (tmp != nc->last->next) {
		
		if (! is_dropped(tmp)) {
			count++;
		}
		
		tmp = tmp->next;
	}
	
	return count;
}

void nodecrawler_reset_all_dropped(Nodecrawler *nc) {
	Node *tmp;
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	tmp = nc->first;
	
	while (tmp != nc->last->next) {
		clr_dropped(tmp);
		tmp = tmp->next;
	}
}

void nodecrawler_project_cols(Nodecrawler *nc, Table *tn, Rtab *results) {
	List *rowlist;
	Rrow *r;
	int i;
	char *colname;
	int colIdx;
	int len;
	union Tuple *p;
	
	if (nc->empty) {
		debugvf("Nodecrawler: empty list! (Doing nothing)\n");
		return ;
	}
	
	debugvf("Nodecrawler: Projecting columns\n");
	
	rowlist = list_new();
	nodecrawler_set_to_start(nc);
	while (nodecrawler_has_more(nc)) {
		/* Extract data from tuple */
		r = mem_alloc(sizeof(Rrow));
		r->cols = mem_alloc(results->ncols * sizeof(char*));
		
		for (i = 0; i < results->ncols; i++) {
			
			colname = results->colnames[i];
			
			colIdx = table_lookup_colindex(tn, colname);
			if (colIdx == -1)	/* was timestamp */
			    r->cols[i] = timestamp_to_string(nc->current->tstamp);
			else {
			    p = (union Tuple *)(nc->current->tuple);
			    debugvf("Sanity check: values[%d]=%s\n", colIdx, 
				p->ptrs[colIdx]);
			    len = strlen(p->ptrs[colIdx]) + 1;
			    r->cols[i] = mem_alloc(len);
			    strcpy(r->cols[i], p->ptrs[colIdx]);
			}
			debugvf("r->cols[%d]: %s\n", i, r->cols[i]);	
		}
		
		list_append(rowlist, r);
		
		nodecrawler_move_to_next(nc);
	}
	
	/* Build array from rowlist */
	results->nrows = list_len(rowlist);
	results->rows = (Rrow**) list_to_array(rowlist);
	list_free(rowlist);
}
