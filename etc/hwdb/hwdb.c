/*
 * The Homework Database
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */
#include "hwdb.h"
#include "mb.h"
#include "util.h"
#include "rtab.h"
#include "sqlstmts.h"
#include "parser.h"
#include "indextable.h"
#include "table.h"
#include "hashtable.h"
#include "pubsub.h"
#include "srpc.h"
#include "tslist.h"

#include <stdlib.h>
#include <pthread.h>

/*
 * static data used by the database
 */
static Indextable *itab;
static Hashtable *querytab;
#ifdef HWDB_PUBLISH_IN_BACKGROUND
static TSList workQ;
static pthread_t pubthr[NUM_THREADS];
#endif /* HWDB_PUBLISH_IN_BACKGROUND */

/* Used by sql parser */
sqlstmt stmt;

/*
 * forward declarations for functions in this file
 */
Rtab *hwdb_exec_stmt(void);
Rtab *hwdb_select(sqlselect *select);
int hwdb_create(sqlcreate *create);
int hwdb_insert(sqlinsert *insert);
Rtab *hwdb_showtables(void);
int hwdb_save_select(sqlselect *select, char *name);
int hwdb_delete_query(char *name);
Rtab *hwdb_exec_saved_select(char *name);
int hwdb_subscribe(sqlsubscribe *subscribe);
int hwdb_unsubscribe(sqlsubscribe *subscribe);
void hwdb_publish(char *tablename);
#ifdef HWDB_PUBLISH_IN_BACKGROUND
void *do_publish(void *args);
#endif /* HWDB_PUBLISH_IN_BACKGROUND */

int hwdb_init() {
    
    mb_init();
    itab = itab_new();
    querytab = ht_create(HT_QUERYTAB_BUCKETS);
#ifdef HWDB_PUBLISH_IN_BACKGROUND
    int i;
    workQ = tsl_create();
    for (i = 0; i < NUM_THREADS; i++) {
        pthread_create(&pubthr[i], NULL, do_publish, NULL);
        debugf("Publish thread launched.\n");
    }
#endif /* HWDB_PUBLISH_IN_BACKGROUND */

    return 1;
}

Rtab *hwdb_exec_query(char *query) {
    
    sql_parse(query);
#ifdef VDEBUG
    sql_print();
#endif /* VDEBUG */
    
    return hwdb_exec_stmt();
}


Rtab *hwdb_exec_stmt(void) {
    Rtab *results = NULL;
    
    switch (stmt.type) {
        case SQL_TYPE_SELECT:
            results = hwdb_select(&stmt.sql.select);
            if (!results)
                results = rtab_new_msg(RTAB_MSG_SELECT_FAILED);
            break;
        case SQL_TYPE_CREATE:
            if (!hwdb_create(&stmt.sql.create)) {
                results = rtab_new_msg(RTAB_MSG_CREATE_FAILED);
            } else {
                results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }
            break;
        case SQL_TYPE_INSERT:
            if (!hwdb_insert(&stmt.sql.insert)) {
                results = rtab_new_msg(RTAB_MSG_INSERT_FAILED);
            } else {
                results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }
            break;
        case SQL_SHOW_TABLES:
            results = hwdb_showtables();
            break;
        case SQL_TYPE_SAVE_SELECT:
            if (!hwdb_save_select(&stmt.sql.select, stmt.name)) {
                results = rtab_new_msg(RTAB_MSG_SAVE_SELECT_FAILED);
            } else {
                results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }
            break;
        case SQL_TYPE_DELETE_QUERY:
            if (!hwdb_delete_query(stmt.name)) {
                results = rtab_new_msg(RTAB_MSG_DELETE_QUERY_FAILED);
            } else {
                results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }
            break;
        case SQL_TYPE_EXEC_SAVED_SELECT:
            results = hwdb_exec_saved_select(stmt.name);
            if (!results)
                results = rtab_new_msg(RTAB_MSG_EXEC_SAVED_SELECT_FAILED);
            break;
        case SQL_TYPE_SUBSCRIBE:
            if (!hwdb_subscribe(&stmt.sql.subscribe)) {
                results = rtab_new_msg(RTAB_MSG_SUBSCRIBE_FAILED);
            } else {
                results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }
            break;
        case SQL_TYPE_UNSUBSCRIBE:
            if (!hwdb_unsubscribe(&stmt.sql.subscribe)) {
                results = rtab_new_msg(RTAB_MSG_UNSUBSCRIBE_FAILED);
            } else {
               results = rtab_new_msg(RTAB_MSG_SUCCESS);
            }       
            break;
        default:
            errorf("Error parsing query\n");
            results = rtab_new_msg(RTAB_MSG_PARSING_FAILED);
            break;
    }
    return results;
}


Rtab *hwdb_select(sqlselect *select) {
    Rtab *results;
    char *tablename;
    
    debugf("HWDB: Executing SELECT:\n");
    
    /* Only 1 table supported for now */
    tablename = select->tables[0];
    
    /* Check table exists */
    if (!itab_table_exists(itab, tablename)) {
        errorf("HWDB: no such table\n");
        return NULL;
    }
    
    /* Check column names match */
    if (!itab_colnames_match(itab, tablename, select)) {
        errorf("HWDB: Column names in SELECT don't match with this table.\n");
        return NULL;
    }
    
    /* Perform select */
    results = itab_build_results(itab, tablename, select);
        
    return results;
}


int  hwdb_create(sqlcreate *create) {
    debugf("Executing CREATE:\n");
    
    return itab_create_table(itab, create->tablename, create->ncols,
                             create->colname, create->coltype);
}


int  hwdb_insert(sqlinsert *insert) {
    int i;
    Table *tn;
    union Tuple *p;
    
    debugf("Executing INSERT:\n");
    
    if (! (tn = itab_table_lookup(itab, insert->tablename))) {
        errorf("Insert table name does not exist\n");
        return 0;
    }

    /* Check columns are compatible */
    if (!itab_is_compatible(itab, insert->tablename, 
            insert->ncols, insert->coltype)) {
        errorf("Insert not compatible with table\n");
        return 0;
    }
    
    /* allocate space for tuple, copy values into tuple, thread new
     * node to end of table */
    mb_insert_tuple(insert->ncols, insert->colval, tn);
    
    /* Tuple sanity check */
    debugvf("SANITY> tuple key: %s\n",insert->tablename);
    p = (union Tuple *)(tn->newest->tuple);
    for (i=0; i < insert->ncols; i++) {
        debugvf("SANITY> colval[%d] = %s\n", i, p->ptrs[i]);
    }
    
    /* Notify all subscribers */
    hwdb_publish(insert->tablename);

    return 1;
}

Rtab *hwdb_showtables(void) {
    debugf("Executing SHOW TABLES\n");
    return itab_showtables(itab);
}


int  hwdb_save_select(sqlselect *select, char *name) {
    sqlselect *new_select;
    
    debugf("Executing SAVE SELECT\n");
    
    /* Check if name already exists */
    if (ht_lookup(querytab, name) != NULL) {
        warningf("Query save-name already in use: %s\n", name);
        return 0;
    }
    
    /* Copy the select statement */
    new_select = mem_alloc(sizeof(sqlselect));
    new_select->ncols = select->ncols;
    new_select->cols = select->cols;
    new_select->colattrib = select->colattrib;
    new_select->ntables = select->ntables;
    new_select->tables = select->tables;
    new_select->windows = select->windows;
    new_select->nfilters = select->nfilters;
    new_select->filters = select->filters;
    new_select->filtertype = select->filtertype;
    new_select->orderby = select->orderby;
    new_select->isCountStar = select->isCountStar;
    new_select->containsMinMaxAvgSum = select->containsMinMaxAvgSum;
    select->ncols = 0;	/* all storage inherited by new select */
    select->ntables = 0;
    select->nfilters = 0;
    select->cols = NULL;
    select->colattrib = NULL;
    select->tables = NULL;
    select->windows = NULL;
    select->filters = NULL;
    select->orderby = NULL;
    
    /* Save the query in the query-table */
    ht_insert(querytab, str_dupl(name), new_select);
    
    return 1;
}

int  hwdb_delete_query(char *name) {
    sqlselect *select;
    int i;
    
    debugf("HWDB: Deleting query: %s\n", name);
    
    select = ht_lookup(querytab, name);
    if (!select) {
        warningf("No query stored under the name: %s\n", name);
        return 0;
    }
    
    ht_remove(querytab, name);
    /*
     * free dynamic storage in select, then free select
     */
    if (select->ncols > 0) {
        for (i = 0; i < select->ncols; i++)
            mem_free(select->cols[i]);
        mem_free(select->cols);
        mem_free(select->colattrib);
    }
    if (select->ntables > 0) {
        for (i = 0; i < select->ntables; i++) {
            mem_free(select->tables[i]);
            mem_free(select->windows[i]);
        }
        mem_free(select->tables);
        mem_free(select->windows);
    }
    if (select->nfilters > 0) {
        for (i = 0; i < select->nfilters; i++) {
            mem_free(select->filters[i]->varname);
            mem_free(select->filters[i]);
        }
        mem_free(select->filters);
    }
    if (select->orderby)
        mem_free(select->orderby);
    mem_free(select);
    
    /* Possibly unsubscribe all clients of this query at this point...
     *   -- foreach table
     *      -- foreach subscriber list
     *         -- if queryname matches
     *            -- hwdb_unsubscribe()
     *            NEEDS TO BE DONE ASAP
     */
    
    return 1;
}

Rtab *hwdb_exec_saved_select(char *name) {
    sqlselect *select;
    
    debugf("HWDB: Executing previously saved select: %s\n", name);
    
    select = ht_lookup(querytab, name);
    if (!select) {
        warningf("No query stored under the name: %s\n", name);
        return NULL;
    }
    
    return hwdb_select(select);
    
}


int  hwdb_subscribe(sqlsubscribe *subscribe) {
    sqlselect *select;
    unsigned short port;
    RpcConnection rpc = NULL;
    
    debugf("HWDB: Subscribing to a query\n");
    
    /* Check if query exists */
    select = ht_lookup(querytab, subscribe->queryname);
    if (!select) {
        errorf("Query doesn't exist. Not subscribing...\n");
        return 0;
    }
    
    /* Check query contains valid table */
    if (!itab_table_exists(itab, select->tables[0])) {
        errorf("Query doesn't contain valid table. Not subscribing...\n");
        return 0;
    }
    
    /* Open connection to callback process (a.k.a. subscriber)*/
    port = atoi(subscribe->port);
    rpc = rpc_connect(subscribe->ipaddr, port, subscribe->service, 1l);
    if (! rpc) {
        errorf("Can't connect to callback. Not subscribing...\n");
        return 0;
    }
    
    /* Register to notify callback on table updates */
    itab_subscribe(itab, select->tables[0], subscribe, rpc);
    
    return 1;
}

int   hwdb_unsubscribe(sqlsubscribe *subscribe) {
    sqlselect *select;
    
    debugf("HWDB: unsubscribing {%s:%s:%s} from query: %s.\n", 
            subscribe->ipaddr, subscribe->port,
            subscribe->service, subscribe->queryname);
    
    select = ht_lookup(querytab, subscribe->queryname);
    if (!select) {
        errorf("Query doesn't exist. Not unsubscribing...\n");
        return 0;
    }
    
    /* Check query contains valid table */
    if (!itab_table_exists(itab, select->tables[0])) {
        errorf("Query doesn't contain valid table. Not unsubscribing...\n");
        return 0;
    }
    
    /* Remove registered callback */
    return itab_unsubscribe(itab, select->tables[0], subscribe);

}


void hwdb_publish(char *tablename) {
    List *sublist;
    LNode *node;
    Subscription *sub;
    Rtab *results;
    
    debugf("HWDB: publishing to subscribers of table: %s\n", tablename);
        
    sublist = itab_get_subscriberlist(itab, tablename);
    
    if (!sublist) {
        debugf("HWDB: No subcribers to publish to.\n");
        return;
    }
    
    
    /* Foreach */
    node = sublist->head;
    while (!list_empty(sublist) && node) {
        
        if (node->val) {
            sub = (Subscription *) node->val;
            
            /* NB 2: This function could be optimized by caching results
             *       of same query for multiple subscribers.
             */
            results = hwdb_exec_saved_select(sub->queryname);
            
            /* Check if query still exists */
            if (!results) {
                node=node->next;
                continue;
            }
            
#ifdef HWDB_PUBLISH_IN_BACKGROUND
	    tsl_append(workQ, sub, results, 1);
#else
            /* Send results to subscriber */
            debugf("HWDB: Sending to subscriber: %s:%s:%s\n",
                sub->ipaddr, sub->port, sub->service);
            /* need to check return and unsubscribe if false */
            rtab_send(results, sub->rpc);
            rtab_free(results);
#endif /* HWDB_PUBLISH_IN_BACKGROUND */
        }
        
        node = node->next;
    }
    
}

#ifdef HWDB_PUBLISH_IN_BACKGROUND
void *do_publish(void *args) {
    Subscription *sub;
    Rtab *results = (Rtab *)args;  /* assignment eliminates unused warning */
    int size;
    
    for (;;) {
        tsl_remove(workQ, (void *)&sub, (void *)&results, &size);
        /* Send results to subscriber */
        debugf("HWDB: Sending to subscriber: %s:%s:%s\n",
            sub->ipaddr, sub->port, sub->service);
        /* need to check return and unsubscribe if false */
	rtab_send(results, sub->rpc);
	rtab_free(results);
    }
    return NULL;
}
#endif /* HWDB_PUBLISH_IN_BACKGROUND */
