/* table.c
 * 
 * Represents a Table in the databse
 * 
 * Created by Oliver Sharma on 2009-05-05.
 * Copyright (c) 2009. All rights reserved.
 */
#include "table.h"

#include "util.h"
#include "typetable.h"
#include "sqlstmts.h"
#include "list.h"
#include "pubsub.h"
#include "srpc.h"

#include <string.h>
#include <pthread.h>

Table *table_new(int ncols, char **colname, int **coltype) {
	Table *tn;
	int i;
	
	tn = mem_alloc(sizeof(Table));
	tn->ncols = ncols;
	tn->colname = (char **)mem_alloc(ncols * sizeof(char *));
	tn->coltype = (int **)mem_alloc(ncols * sizeof(int *));
	for (i = 0; i < ncols; i++) {
	    tn->colname[i] = str_dupl(colname[i]);
	    tn->coltype[i] = coltype[i];
	}
	tn->oldest = NULL;
	tn->newest = NULL;
	tn->count = 0;
	tn->sublist = list_new();
	pthread_mutex_init(&tn->tb_mutex, NULL);
	
	return tn;
}

static int table_contains_col(Table *tn, char *colname) {
	int i;
	
	for (i = 0; i < tn->ncols; i++) {
		if (strcmp(tn->colname[i], colname) == 0) {
			debugvf("Col %s exists in table. Good.\n", colname);
			return 1;
		}
	}
	if (strcmp(colname, "timestamp") == 0)
		return 1;
	return 0;
}

int table_colnames_match(Table *tn, sqlselect *select) {
	int i;
	char *colname;
	
	/* Special case: SELECT * from TABLE */
	if (select->ncols == 1 && strcmp(select->cols[0],"*")==0){
		debugvf("Selecting *, so colums match by default.\n");
		return 1;
	}
	for (i=0; i < select->ncols; i++) {
		colname = select->cols[i];
		if (!table_contains_col(tn, colname)) {
			errorf("No such column: %s\n", colname);
			return 0;
		}
	}
	return 1;
}

void table_lock(Table *tn) {
	debugf("Table: Acquiring lock...\n");
	pthread_mutex_lock(&tn->tb_mutex);
}

void table_unlock(Table *tn) {
	debugf("Table: Releasing lock...\n");
	pthread_mutex_unlock(&tn->tb_mutex);
}

void table_store_select_cols(Table *tn, sqlselect *select, Rtab *results) {
	int i;
	int isselstar;
	int ncols;
	char *p;
	
	/* check if select *, if so duplicate column names from table
	 * otherwise, duplicate column names from select structure */
	isselstar = (select->ncols == 1 && strcmp(select->cols[0], "*") == 0);
	if (isselstar)
	    ncols = tn->ncols + 1;	/* +1 for timestamp */
	else
	    ncols = select->ncols;
	results->colnames = (char **)mem_alloc(ncols * sizeof(char *));
	results->ncols = ncols;
	for (i = 0; i < ncols; i++) {
		if (isselstar)
		    if (i == 0)
			p = "timestamp";
		    else
			p = tn->colname[i-1];
		else
			p = select->cols[i];
		results->colnames[i] = str_dupl(p);
	}
}

/* Returns NULL if not found */
static int *table_lookup_coltype(Table *tn, char *colname) {
	int i;
	
	for (i=0; i < tn->ncols; i++) {
		if (strcmp(tn->colname[i], colname) == 0) {
			debugvf("Col %s exists and has type: %s\n", 
				colname, primtype_name[*tn->coltype[i]]);
			return tn->coltype[i];
		}
	}
	if (strcmp(colname, "timestamp") == 0)
		return PRIMTYPE_TIMESTAMP;
	
	return NULL;
}

void table_extract_relevant_types(Table *tn, Rtab *results) {
	int i;

	if (results->ncols>0) {
		results->coltypes = mem_alloc(results->ncols * sizeof(int*));
		for (i=0; i<results->ncols; i++) {
			results->coltypes[i] = table_lookup_coltype(tn,
					                results->colnames[i]);
		}
	}
}

/* Returns -1 if no such colname */
int table_lookup_colindex(Table *tn, char *colname) {
	int i;

	for (i = 0; i < tn->ncols; i++) {
		if (strcmp(tn->colname[i], colname) == 0) {
			debugvf("Col %s exists and has index: %d\n", colname, i);
			return i;
		}
	}
	
	/* Not found */
	return -1;
}

void table_add_subscription(Table *tn, sqlsubscribe *subscribe, RpcConnection rpc) {
	Subscription *sub;
	
	sub = mem_alloc(sizeof(Subscription));
	sub->queryname = str_dupl(subscribe->queryname);
	sub->ipaddr = str_dupl(subscribe->ipaddr);
	sub->port = str_dupl(subscribe->port);
	sub->service = str_dupl(subscribe->service);
	sub->rpc = rpc;
	
	list_append(tn->sublist, sub);
	
}
