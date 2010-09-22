/*
 * The Homework Database
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */
#ifndef HWDB_INDEXTABLE_H
#define HWDB_INDEXTABLE_H

#include "sqlstmts.h"
#include "rtab.h"
#include "list.h"
#include "srpc.h"
#include "table.h"

typedef struct indextable Indextable;

Indextable *itab_new(void);

int itab_create_table(Indextable *itab, char *tablename, 
	int ncols, char **colnames, int **coltypes);

int itab_is_compatible(Indextable *itab, char *tablename, 
	int ncols, int **coltypes);

int itab_table_exists(Indextable *itab, char *tablename);

Table *itab_table_lookup(Indextable *itab, char *tablename);

int itab_colnames_match(Indextable *itab, char *tablename, sqlselect *select);

Rtab *itab_build_results(Indextable *itab, char *tablename, sqlselect *select);

Rtab *itab_showtables(Indextable *itab);

void itab_subscribe(Indextable *itab, char *tablename, sqlsubscribe *subscribe, RpcConnection rpc);

int itab_unsubscribe(Indextable *itab, char *tablename, sqlsubscribe *subscribe);

List *itab_get_subscriberlist(Indextable *itab, char *tablename);

void itab_lock(Indextable *itab);
void itab_unlock(Indextable *itab);

void itab_lock_table(Indextable *itab, char *tablename);
void itab_unlock_table(Indextable *itab, char *tablename);

#endif
