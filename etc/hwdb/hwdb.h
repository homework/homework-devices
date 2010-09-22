/*
 * The Homework Database
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */
#ifndef HWDB_HWDB_H
#define HWDB_HWDB_H

#include "rtab.h"

int hwdb_init();
Rtab *hwdb_exec_query(char *query);

#endif
