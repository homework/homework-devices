#ifndef A_HOSTMAP_H
#define A_HOSTMAP_H

#include <netinet/ip.h>

#define HOSTNAME_LEN 1025 /* Size of buffer when resolving ip addresses */

/*
 * initialize the hostmap
 */
void hostmap_init();

/*
 * resolve IP address
 */
char *hostmap_resolve(in_addr_t ip_addr);

/*
 * free storage associated with hostmap
 */
void  hostmap_free();

/*
 * dump contents of hostmap
 */
void hostmap_dump();

#endif
