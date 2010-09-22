/*
 * table.h - define data structure for tables
 */
#ifndef _TABLE_H_
#define _TABLE_H_

#include "node.h"
#include "list.h"
#include "sqlstmts.h"
#include "rtab.h"
#include "srpc.h"
#ifndef EMBED_IN_KERNEL
#include <pthread.h>
#endif /* EMBED_IN_KERNEL */

typedef struct table {
    int ncols;			/* number of columns */
    char **colname;		/* names of columns */
    int **coltype;		/* types of columns */
    struct node *oldest;	/* oldest node in the table */
    struct node *newest;	/* newest node in the table */
    long count;			/* number of nodes in the table */
    List *sublist;		/* list of subscriptions */
#ifndef EMBED_IN_KERNEL
    pthread_mutex_t tb_mutex;	/* mutex for protecting the table */
#endif /* EMBED_IN_KERNEL */
} Table;

Table *table_new(int ncols, char **colname, int **coltype);

int table_colnames_match(Table *tn, sqlselect *select);

void table_lock(Table *tn);
void table_unlock(Table *tn);

void table_store_select_cols(Table *tn, sqlselect *select, Rtab *results);
void table_extract_relevant_types(Table *tn, Rtab *results);
void table_add_subscription(Table *tn, sqlsubscribe *subscribe, RpcConnection rpc);
int table_lookup_colindex(Table *tn, char *colname);

#endif /* _TABLE_H_ */
