/* parser.c
 * 
 * HWDB SQL Parser API
 * 
 * Created by Oliver Sharma on 2009-05-04.
 * Copyright (c) 2009. All rights reserved.
 */
#include "parser.h"

#include "sqlstmts.h"
#include "typetable.h"
#include "util.h"
#include <stdio.h>

extern int yyparse(void);
extern int yyrestart(void);
extern void *yy_scan_string(const char *);
extern void yy_delete_buffer(void*);
extern sqlstmt stmt;

void reset_statement() {
    int i;
	
	/* NB: possible memory leaks here !!! */
	
	switch (stmt.type) {
		
	    case SQL_TYPE_SUBSCRIBE:
		mem_free(stmt.sql.subscribe.queryname);
		mem_free(stmt.sql.subscribe.ipaddr);
		mem_free(stmt.sql.subscribe.port);
		mem_free(stmt.sql.subscribe.service);
		stmt.type = 0;
		break;
		
	    case SQL_TYPE_PUBLISH:
		mem_free(stmt.name);
		stmt.type = 0;
		break;
		
	    case SQL_TYPE_SAVE_SELECT:
		mem_free(stmt.name);	/* falls through to SELECT */
	    case SQL_TYPE_SELECT:
		if (stmt.sql.select.ncols > 0) {
		    for (i = 0; i < stmt.sql.select.ncols; i++)
		        mem_free(stmt.sql.select.cols[i]);
		    mem_free(stmt.sql.select.cols);
		    mem_free(stmt.sql.select.colattrib);
		}
		stmt.sql.select.ncols = 0;
		stmt.sql.select.cols = NULL;
		stmt.sql.select.colattrib = NULL;
		if (stmt.sql.select.ntables > 0) {
		    for (i = 0; i < stmt.sql.select.ntables; i++) {
		        mem_free(stmt.sql.select.tables[i]);
			mem_free(stmt.sql.select.windows[i]);
		    }
		    mem_free(stmt.sql.select.tables);
		    mem_free(stmt.sql.select.windows);
		}
		stmt.sql.select.ntables = 0;
		stmt.sql.select.tables = NULL;
		stmt.sql.select.windows = NULL;
		if (stmt.sql.select.nfilters > 0) {
		    for (i = 0; i < stmt.sql.select.nfilters; i++) {
			mem_free(stmt.sql.select.filters[i]->varname);
		        mem_free(stmt.sql.select.filters[i]);
		    }
		    mem_free(stmt.sql.select.filters);
		}
		stmt.sql.select.nfilters = 0;
		stmt.sql.select.filters = NULL;
		stmt.sql.select.filtertype = 0;
		if (stmt.sql.select.orderby)
		    mem_free(stmt.sql.select.orderby);
		stmt.sql.select.orderby = NULL;
		stmt.sql.select.isCountStar = 0;
		stmt.sql.select.containsMinMaxAvgSum = 0;
		stmt.type = 0;
		break;
		
	    case SQL_TYPE_EXEC_SAVED_SELECT:
		mem_free(stmt.name);
		stmt.type = 0;
		break;
		
	   case SQL_TYPE_CREATE:
		mem_free(stmt.sql.create.tablename);
		if (stmt.sql.create.ncols > 0) {
		    for (i = 0; i < stmt.sql.create.ncols; i++)
		        mem_free(stmt.sql.create.colname[i]);
		    mem_free(stmt.sql.create.colname);
		    mem_free(stmt.sql.create.coltype);
		}
		stmt.sql.create.ncols = 0;
		stmt.sql.create.colname = NULL;
		stmt.sql.create.coltype = NULL;
		stmt.type = 0;
		break;
		
	    case SQL_TYPE_INSERT:
		mem_free(stmt.sql.insert.tablename);
		stmt.sql.insert.tablename = NULL;
		if (stmt.sql.insert.ncols > 0) {
		    for (i = 0; i < stmt.sql.insert.ncols; i++)
		        mem_free(stmt.sql.insert.colval[i]);
		    mem_free(stmt.sql.insert.colval);
		    mem_free(stmt.sql.insert.coltype);
		}
		stmt.sql.insert.ncols = 0;
		stmt.sql.insert.colval = NULL;
		stmt.sql.insert.coltype = NULL;
		stmt.type = 0;
		break;
		
	    case SQL_SHOW_TABLES:
		stmt.type = 0;
		break;
		
	    default:
		stmt.type = 0;
	}
	
	stmt.type = 0;
}

/* Places parsed output in externally declared global variable:
 *     sqlstmt stmt
 */
void *sql_parse(char *query) {
	void *bufstate;
	reset_statement();
	bufstate = yy_scan_string(query);
	yyparse();
	sql_reset_parser(bufstate);
	return bufstate;
}

void sql_reset_parser(void *bufstate) {
	debugvf("resetting parser\n");
	yy_delete_buffer(bufstate);
}

void sql_dup_stmt(sqlstmt *dup) {
	
	dup->type = stmt.type;
	
	switch (dup->type) {
		
		case SQL_TYPE_SELECT:
		dup->sql.select.ncols = stmt.sql.select.ncols;
		dup->sql.select.cols = stmt.sql.select.cols;
		dup->sql.select.ntables = stmt.sql.select.ntables;
		dup->sql.select.tables = stmt.sql.select.tables;
		dup->sql.select.filtertype = stmt.sql.select.filtertype;
		dup->sql.select.orderby = stmt.sql.select.orderby;
		break;
		
		case SQL_TYPE_CREATE:
		dup->sql.create.tablename = stmt.sql.create.tablename;
		dup->sql.create.ncols = stmt.sql.create.ncols;
		dup->sql.create.colname = stmt.sql.create.colname;
		dup->sql.create.coltype = stmt.sql.create.coltype;
		break;
		
		case SQL_TYPE_INSERT:
		dup->sql.insert.tablename = stmt.sql.insert.tablename;
		dup->sql.insert.ncols = stmt.sql.insert.ncols;
		dup->sql.insert.colval = stmt.sql.insert.colval;
		dup->sql.insert.coltype = stmt.sql.insert.coltype;
		break;
		
		default:
		errorf("can't duplicate unknown sqlstmt type\n");
				
	}
}

/* Prints externally declared global variable
 *     sqlstmt stmt
 * to standard output
 */
void sql_print() {
	int i;
	
	printf("-----[SQL statement]-------\n");
	
	switch(stmt.type) {
		
		case SQL_TYPE_SAVE_SELECT:
		printf("Save select statment: %s\n", stmt.name);
		break;
		
		case SQL_TYPE_EXEC_SAVED_SELECT:
		printf("Exec saved select: %s\n", stmt.name);
		break;
		
		case SQL_TYPE_DELETE_QUERY:
		printf("Deleting query %s\n", stmt.name);
		break;
		
		case SQL_TYPE_SUBSCRIBE:
		printf("Subscribed %s:%s to query %s\n", 
		stmt.sql.subscribe.ipaddr, stmt.sql.subscribe.port, stmt.sql.subscribe.queryname);
		break;
		
		case SQL_TYPE_UNSUBSCRIBE:
		printf("Unsubscribed %s:%s from query %s\n", 
		stmt.sql.subscribe.ipaddr, stmt.sql.subscribe.port, stmt.sql.subscribe.queryname);
		break;
		
		case SQL_TYPE_PUBLISH:
		printf("Publishing query %s\n", stmt.name);
		break;
	
		case SQL_TYPE_SELECT:
		printf("Select statement\n");
		if (stmt.sql.select.orderby != NULL) {
			printf("{Ordered by: %s}\n", stmt.sql.select.orderby);
		}
		for (i = 0; i < stmt.sql.select.ncols; i++) {
			printf("col[%d]: %s (colattrib: %s)\n", i, stmt.sql.select.cols[i], colattrib_name[*stmt.sql.select.colattrib[i]]);
		}
		for (i = 0; i < stmt.sql.select.ntables; i++) {
			printf("table[%d]: %s ", i, stmt.sql.select.tables[i]);
			switch(stmt.sql.select.windows[i]->type) {
				case SQL_WINTYPE_NONE:
				printf("\n");
				break;
				
				case SQL_WINTYPE_TIME:
				printf("[time window: %d (unitcode: %d)]\n", 
					stmt.sql.select.windows[i]->num,
					stmt.sql.select.windows[i]->unit);
				break;
				
				case SQL_WINTYPE_TPL:
				printf("[tuple window: %d]\n", stmt.sql.select.windows[i]->num);
				break;
				
				default:
				errorf("[Unknown window type]\n");
			}
		}
		break;
		
		case SQL_TYPE_CREATE:
		printf("Create statement\n");
		printf("tablename: %s\n", stmt.sql.create.tablename);
		for (i = 0; i < stmt.sql.create.ncols; i++) {
			printf("name: %s, type %s\n", 
				stmt.sql.create.colname[i],
				primtype_name[*stmt.sql.create.coltype[i]]);
		}
		break;
		
		case SQL_TYPE_INSERT:
		printf("Insert statement\n");
		printf("tablename: %s\n", stmt.sql.insert.tablename);
		for (i = 0; i < stmt.sql.insert.ncols; i++) {
			printf("val: %s, type %s\n", 
				stmt.sql.insert.colval[i],
				primtype_name[*stmt.sql.insert.coltype[i]]);
		}
		break;
		
		case SQL_SHOW_TABLES:
		printf("Show tables\n");
		break;
		
		default:
		errorf("Unknown statement\n");
	}
	
	printf("--------------------\n");
}
