/*
 * The Homework Database
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */
#include "indextable.h"

#include "tuple.h"
#include "hashtable.h"
#include "table.h"
#include "sqlstmts.h"
#include "util.h"
#include "nodecrawler.h"
#include "typetable.h"
#include "rtab.h"
#include "srpc.h"
#include "pubsub.h"

#include <pthread.h>
#include <string.h>

struct indextable {
	Hashtable *ht;
	pthread_mutex_t *masterlock;
	//pthread_mutexattr_t attr;
};


Indextable *itab_new(void) {
	Indextable *itab;
	//pthread_mutexattr_t attr;
	
	itab = mem_alloc(sizeof(Indextable));
	itab->ht = ht_create(TT_BUCKETS);
	
	/* Init recursive lock */
	itab->masterlock = mem_alloc(sizeof(pthread_mutex_t));
	//pthread_mutexattr_init(&attr);
	//pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
	//pthread_mutex_init(itab->masterlock, &attr);
	pthread_mutex_init(itab->masterlock, NULL);
	
	return itab;
}


int itab_create_table(Indextable *itab, char *tablename, int ncols, char **colnames, int **coltypes) {

	Table *tn;

	itab_lock(itab);
	
	debugvf("Itab: creating table\n");
	
	tn = ht_lookup(itab->ht, tablename);
	
	if (NULL == tn) {
		
		debugf("Adding new table to master table\n");
		
		/* Create new table node */
		tn = table_new(ncols, colnames, coltypes);
		
		/* Add into hashtable */
		ht_insert(itab->ht, str_dupl(tablename), tn);
		
	} else {
		errorf("Table exists. Doing nothing.\n");
		itab_unlock(itab);
		return 0;
	}
	
	itab_unlock(itab);
	
	return 1;
}


int itab_is_compatible(Indextable *itab, char *tablename, int ncols, int **coltypes) {
	Table *tn;
	int i;
	
	tn = ht_lookup(itab->ht, tablename);
	
	/* Check table exists */
	if (NULL == tn) {
		errorf("Insert: No such table: %s\n", tablename);
		return 0;
	}
	
	/* Check number of columns */
	if (tn->ncols != ncols) {
		errorf("Insert: Not the same number of columns\n");
		return 0;
	}
	
	/* Check column types */
	for (i=0; i < tn->ncols; i++) {
		if (tn->coltype[i] != coltypes[i]) {
			errorf("Insert: Incompatible column type column num: %d\n", i);
			return 0;
		}
	}
	
	return 1;
	
}

Table *itab_table_lookup(Indextable *itab, char *tablename) {

	return (Table *)ht_lookup(itab->ht, tablename);
}

int itab_table_exists(Indextable *itab, char *tablename) {

	return (itab_table_lookup(itab, tablename) != NULL);
	
}


int itab_colnames_match(Indextable *itab, char *tablename, sqlselect *select) {
	Table *tn;
	
	tn = ht_lookup(itab->ht, tablename);
	
	/* Check table exists */
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return 0;
	}
	
	if (!table_colnames_match(tn, select)) {
		errorf("Column names in SELECT don't match with this table.\n");
		return 0;
	}
	
	return 1;
}


Rtab *itab_build_results(Indextable *itab, char *tablename, sqlselect *select) {
	Table *tn;
	Rtab *results;
	Nodecrawler *nc;
	
	tn = ht_lookup(itab->ht, tablename);
	
	/* Check table exists */
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return NULL;
	}
	
	/* Lock table */
	table_lock(tn);
	
	/* Build results */
	results = rtab_new();
	table_store_select_cols(tn, select, results);
	table_extract_relevant_types(tn, results);
		
	/* Now add all relevant rows
	 *   -- apply_window
	 *   -- apply_filter
	 *   -- project columns
	 *
	 * Note that the basic idea here is to manipulate a list
	 * of tuples, running over it and dropping tuples that
	 * don't pass the window or filter rules.
	 *
	 * The tuples that remain in the list are all ok and then
	 * the columns are projected from these tuples.
	 */
	nc = nodecrawler_new(tn->oldest, tn->newest);
	nodecrawler_apply_window(nc, select->windows[0]); /* NB only one window */
	nodecrawler_apply_filter(nc, tn, select->nfilters, select->filters, select->filtertype);
	nodecrawler_project_cols(nc, tn, results);
	
	/* order by */
	rtab_orderby(results, select->orderby);
	
	/* count, min, max, avg, sum */
	if (select->isCountStar) {
		rtab_countstar(results);
	} else if (select->containsMinMaxAvgSum) {
		rtab_processMinMaxAvgSum(results, select->colattrib);
	}
	
	/* Reset dropped markers */
	nodecrawler_reset_all_dropped(nc);
	
	nodecrawler_free(nc);
	
	
	/* Unlock table */
	table_unlock(tn);
	
	return results;
}

Rtab *itab_showtables(Indextable *itab) {
	Rtab *results;
	List *rowlist;
	char **tnames;
	Rrow *r;
	int j;
	int N;
	
	itab_lock(itab);
		
	tnames = ht_keylist(itab->ht, &N);
	if (! tnames) {
		results = rtab_new_msg(RTAB_MSG_NO_TABLES_DEFINED);
		itab_unlock(itab);
		return results;
	}
	results = rtab_new();
	results->ncols = 1;
	results->nrows = N;
	results->colnames = (char **)mem_alloc(sizeof(char *));
	results->colnames[0] = str_dupl("Tablename");
	results->coltypes = (int **)mem_alloc(sizeof(int *));
	results->coltypes[0] = PRIMTYPE_VARCHAR;
	rowlist = list_new();
	for (j = 0; j < results->nrows; j++) {
		r = mem_alloc(sizeof(Rrow));
		r->cols = mem_alloc(sizeof(char *));
		r->cols[0] = str_dupl(tnames[j]);
		list_append(rowlist, r);
	}
	results->rows = (Rrow **)list_to_array(rowlist);
	list_free(rowlist);
	mem_free(tnames);
	itab_unlock(itab);

	return results;
}


void itab_subscribe(Indextable *itab, char *tablename, sqlsubscribe *subscribe, RpcConnection rpc) {
	Table *tn;
	
	tn = ht_lookup(itab->ht, tablename);
	
	/* Check table exists */
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return;
	}
	
	table_lock(tn);
	table_add_subscription(tn, subscribe, rpc);
	table_unlock(tn);
}

int itab_unsubscribe(Indextable *itab, char *tablename, sqlsubscribe *subscribe) {
	
	List *sublist;
	LNode *prev, *node;
	
	Subscription *sub;
	Subscription *tmpsub;
	
	debugvf("Itab: unsubscribing.\n");
	
	sublist = itab_get_subscriberlist(itab, tablename);
	if (!sublist) {
		errorf("itab: No such table: %s\n", tablename);
		return 0;
	}
	
	itab_lock_table(itab, tablename);
	
	/*--->>*/
	
	/* Foreach */
	prev = NULL;
	node = sublist->head;
	while (!list_empty(sublist) && node) {
		
		if (node->val) {
			sub = (Subscription *) node->val;

			/* Remove this node if subscription matches */
			if ((strcmp(sub->queryname, subscribe->queryname) == 0)
				&& (strcmp(sub->ipaddr, subscribe->ipaddr) == 0)
				&& (strcmp(sub->port, subscribe->port) == 0)
				&& (strcmp(sub->service, subscribe->service) == 0)) {
					
					debugf("Found matching subscription. Removing form list\n");
					
					/* Is this the first node in the list ? */
					if (!prev) {
						debugvf("Removing first node in list.\n");
						tmpsub = list_remove_first(sublist);
						rpc_disconnect(tmpsub->rpc);
						mem_free(tmpsub->queryname);
						mem_free(tmpsub->ipaddr);
						mem_free(tmpsub->port);
						mem_free(tmpsub->service);
						mem_free(tmpsub);
						break;
					} else if (!node->next) {
						debugvf("Removing last node in list.\n");
						tmpsub = (Subscription *) node->val;
						rpc_disconnect(tmpsub->rpc);
						mem_free(tmpsub->queryname);
						mem_free(tmpsub->ipaddr);
						mem_free(tmpsub->port);
						mem_free(tmpsub->service);
						mem_free(tmpsub);
						prev->next = NULL;
						mem_free(node);
						sublist->len--;
						break;
					} else {
						
						debugvf("Removing node from middle.\n");
						
						/* Point over the node to next one */
						prev->next = node->next;
						
						/* Free subscription */
						tmpsub = (Subscription *) node->val;
						
						/* Send close message to callback */
						rpc_disconnect(tmpsub->rpc);
						
						mem_free(tmpsub->queryname);
						mem_free(tmpsub->ipaddr);
						mem_free(tmpsub->port);
						mem_free(tmpsub->service);
						mem_free(tmpsub);
						
						/* Assumption: there are no duplicate entries */
						mem_free(node);
						sublist->len--;
						break;
						
					}
					
			}
		
		}
		
		debugvf("Moving to next node\n");
		prev = node;
		node = node->next;
	}
	
	/*<<---*/
	
	itab_unlock_table(itab, tablename);
	return 1;
}

List *itab_get_subscriberlist(Indextable *itab, char *tablename) {
	Table *tn;
	
	tn = ht_lookup(itab->ht, tablename);
		
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return NULL;
	}
	
	return tn->sublist;
	
}

void itab_lock(Indextable *itab) {
	debugf("Itab: Acquiring masterlock...\n");
	pthread_mutex_lock(itab->masterlock);
}

void itab_unlock(Indextable *itab) {
	debugf("Itab: Releasing masterlock...\n");
	pthread_mutex_unlock(itab->masterlock);
}

void itab_lock_table(Indextable *itab, char *tablename) {
	Table *tn;
	
	debugf("Itab locking table: %s\n", tablename);
	
	tn = ht_lookup(itab->ht, tablename);
		
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return;
	}
	
	table_lock(tn);
}

void itab_unlock_table(Indextable *itab, char *tablename) {
	Table *tn;

	debugf("Itab unlocking table: %s\n", tablename);
	
	tn = ht_lookup(itab->ht, tablename);
		
	if (NULL == tn) {
		errorf("itab: No such table: %s\n", tablename);
		return;
	}
	
	table_unlock(tn);
}
