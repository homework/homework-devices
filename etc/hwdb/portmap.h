#ifndef A_PORTMAP_H
#define A_PORTMAP_H

/*
 * initialize the portmap for `proto' from `file'
 */
void portmap_init(unsigned char proto, char *file);

/*
 * classify flow according to `proto', `sport' and 'dport'
 */
char *portmap_classify(unsigned char proto, unsigned short sport,
		       unsigned short dport);

/*
 * free storage associated with portmap for `proto'
 */
void portmap_free(unsigned char proto);

/*
 * dump contents of portmap associated with `proto'
 */
void portmap_dump(unsigned char proto);

/*
 * protocol values for TCP and UDP
 */
#define TCP (unsigned char)6
#define UDP (unsigned char)17

/*
 * files containing the definitions
 */
#define TCP_FILE "ports/tcp_port.list"
#define UDP_FILE "ports/udp_port.list"

#endif /* A portmap. */
