/*
 * mb.h - publicly accessible entry points for the memory buffer
 */
#ifndef _MB_H_
#define _MB_H_

#include "tuple.h"
#include "table.h"

void mb_init();

int mb_insert(unsigned char *buf, long len, Table *table);

int mb_insert_tuple(int ncols, char *vals[], Table *table);

#ifndef EMBED_IN_KERNEL
void mb_dump();
#endif /* EMBED_IN_KERNEL */

#endif /* _MB_H_ */
