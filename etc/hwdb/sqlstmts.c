/* sqlstmts.c
 * 
 * SQL statement definitions (used by parser)
 * 
 * Created by Oliver Sharma on 2009-05-03.
 * Copyright (c) 2009. All rights reserved.
 */
#include "sqlstmts.h"
#include "util.h"
#include "timestamp.h"
#include "y.tab.h"
#include <stdlib.h>

#include <string.h>

const int sql_colattrib_types[6] = {0,1,2,3,4,5};
const char *colattrib_name[6] = {"none", "count", "min", "max", "avg", "sum"};

sqlwindow *sqlstmt_new_stubwindow() {
	sqlwindow *win;
	
	win = mem_alloc(sizeof(sqlwindow));
	win->type = SQL_WINTYPE_NONE;
	win->num = -1;
	win->unit = -1;
	
	return win;
}


sqlwindow *sqlstmt_new_timewindow(int num, int unit) {
	sqlwindow *win;
	
	win = mem_alloc(sizeof(win));
	win->type = SQL_WINTYPE_TIME;
	win->num = num;
	win->unit = unit;
	
	return win;
}


sqlwindow *sqlstmt_new_timewindow_now() {
	sqlwindow *win;
	
	win = mem_alloc(sizeof(win));
	win->type = SQL_WINTYPE_TIME;
	win->num = -1;
	win->unit = SQL_WINTYPE_TIME_NOW;
	
	return win;
}


sqlwindow *sqlstmt_new_tuplewindow(int num) {
	sqlwindow *win;
	
	win = mem_alloc(sizeof(win));
	win->type = SQL_WINTYPE_TPL;
	win->num = num;
	win->unit = -1;
	
	return win;
}

sqlfilter *sqlstmt_new_filter(int ctype, char *name, int dtype, char *value) {
	sqlfilter *filter;

	filter = mem_alloc(sizeof(sqlfilter));
	filter->varname = name;
	switch(ctype) {
	    case EQUALS:
		filter->sign = SQL_FILTER_EQUAL; break;
	    case LESS:
		filter->sign = SQL_FILTER_LESS; break;
	    case GREATER:
		filter->sign = SQL_FILTER_GREATER; break;
	    case LESSEQ:
		filter->sign = SQL_FILTER_LESSEQ; break;
	    case GREATEREQ:
		filter->sign = SQL_FILTER_GREATEREQ; break;
	}
	switch(dtype) {
	    case INTEGER:
		filter->value.intv = atoi(value); break;
	    case REAL:
		filter->value.realv = strtod(value, NULL); break;
	    case CHARACTER:
		filter->value.charv = value[0]; break;
	    case TINYINT:
		filter->value.tinyv = atoi(value) & 0xff; break;
	    case SMALLINT:
		filter->value.smallv = atoi(value) & 0xffff; break;
	    case TSTAMP:
		filter->value.tstampv = string_to_timestamp(value); break;
	}
	return filter;
}

int sqlstmt_calc_len(sqlinsert *insert) {
	int total;
	int i;
	
	total = strlen(insert->tablename) + 1;
	total += insert->ncols * sizeof(char*);
	for (i=0; i < insert->ncols; i++) {
		total += strlen(insert->colval[i]) + 1;
	}
	
	debugvf("Calculated insert length to be %d\n", total);
	return total;
	
}
