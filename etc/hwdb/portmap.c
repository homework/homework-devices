#include  <stdio.h>
#include <stdlib.h>
#include <string.h>
#include  <ctype.h>

#include "portmap.h"

typedef struct ht_node *portP;
typedef struct ht_node {
	portP next;
	unsigned int port;
	char *name;
} ht_node_t;

// 65535 possible ports, IANA specifies ~5495 mappings, therefore
#define NPORTS 10000
// two maps, one for TCP, the other for UDP
#define MAPS 2

static int verbose = 0;

/* More definitions are to follow. Increase 
the size of the array accordingly. */
static portP tcpbin[NPORTS];
static portP udpbin[NPORTS];

static portP* bin[] = {tcpbin, udpbin};

/*
 * Robert Jenkins' 32 bit integer hash function
 *
 * obtained from www.concentric.net/~Ttwang/tech/inthash.htm
 */

static unsigned int hash(unsigned int a)
{
   a = (a+0x7ed55d16) + (a<<12);
   a = (a^0xc761c23c) ^ (a>>19);
   a = (a+0x165667b1) + (a<<5);
   a = (a+0xd3a2646c) ^ (a<<9);
   a = (a+0xfd7046c5) + (a<<3);
   a = (a^0xb55a4f09) ^ (a>>16);
   return a;
}

static void add(unsigned int i, char *filename) {
	unsigned int hv;
	portP p;
	unsigned int charndx, portndx, namendx;
	FILE *f;
	char line[256];
	char port[128];
	char name[128];
	enum {IN_VOID, IN_PORT, IN_BTWN, IN_NAME} pos;
	unsigned int NR = 0; 	// Line index (for error messages)
	unsigned int lines = 0;
	
	f = fopen (filename, "r");
	if (!f) {
		fprintf( stderr, "Error: failed to open file %s.\n", filename );
		return;
	}
	while (fgets(line, sizeof(line), f) != NULL) {
		NR++;
		// Ignore comments.
		if (line[0] == '#') continue;
		// Remove "end of line".
		if (line[strlen(line)-1] == '\n') line[strlen(line)-1] = '\0';
		charndx = 0;
		portndx = 0;
		namendx = 0;
		pos = IN_VOID;
		while (charndx < strlen(line)) {
			int c = line[charndx];
			if (pos == IN_VOID && !isspace(c)) {
				// The first word is the port number.
				pos = IN_PORT;
			} else if (pos == IN_PORT && (isspace(c) || ispunct(c))) {
				pos = IN_BTWN;
			} else if (pos == IN_BTWN && !isspace(c)) {
				// From the second word onwards 
				// begins the application name.
				pos = IN_NAME;
			}
			if (pos == IN_PORT) {
				if (! isdigit(c)) {
fprintf(stderr, "Error in %s (line %d): port number should be numerical.\n", 
	filename, NR);
					return;
				}
				port[portndx] = line[charndx];
				portndx++;
			} else
			if (pos == IN_NAME) {
				if (! iscntrl(c)) {
					name[namendx] = line[charndx];
					namendx++;
				}
			}
			charndx++;
		}
		port[portndx] = '\0';
		name[namendx] = '\0';
		if (pos == IN_VOID) // Skip empty line.
			continue;
		
		// Add to hashtable.
		p = malloc(sizeof(ht_node_t));
		p->port = atoi(port);
		p->name = strdup(name);
		hv = hash(p->port) % NPORTS;
		p->next = bin[i][hv];
		bin[i][hv] = p;
		if (verbose) printf ( "port %d is %s\n", p->port, p->name );
		// Added. 
		lines += 1;
	}
	fclose(f);
	if (verbose) printf("%d lines processed.\n", lines);
	return;
}

static unsigned int proto2index(unsigned char proto) {
	unsigned int ans;

	if (proto == TCP)
		ans = 0;
	else if (proto == UDP)
		ans = 1;
	else
		ans = 2;
	return ans;
}

void portmap_init (unsigned char proto, char *file) {
	unsigned int i, j;

	if ((i = proto2index(proto)) == 2)
		return;
	for (j = 0; j < NPORTS; j++) 
		bin[i][j] = NULL;
	add(i, file);
}

char *portmap_classify(unsigned char proto, unsigned short sport,
		       unsigned short dport) {
	unsigned int i;
	unsigned int port_number = 0;	/* Give preference to well-known ports;
				           small-values */
	unsigned int second_port = 0;
	unsigned int hv;
	portP p;

	if ((i = proto2index(proto)) != 2) {
		if (sport < 1024 && dport > 1024) {
			port_number = sport;
			second_port = dport;
		} else if (sport > 1024 && dport < 1024) {
			port_number = dport;
			second_port = sport;
		} else if (sport <= dport) {
			port_number = sport;
			second_port = dport;
		} else {
			port_number = dport;
			second_port = sport;
		}
		hv = hash(port_number) % NPORTS;
		for (p = bin[i][hv]; p != NULL; p = p->next) {
			if (port_number == p->port) {
				return p->name;
			}
		}
	// If the first port (port_number) failed, try the second port.
		hv = hash(second_port) % NPORTS;
		for (p = bin[i][hv]; p != NULL; p = p->next) {
			if (second_port == p->port) {
				return p->name;
			}
		}
	}
	return "unknown";
}

void portmap_free(unsigned char proto) {
	unsigned int i;
	
	i = proto2index(proto);
	if (i != 2) {
		unsigned int j;
		portP p, q;
		for (j = 0; j < NPORTS; j++) {
			for (p = bin[i][j]; p != NULL; p = q) { 
				q = p->next;
				free(p->name);
				free(p);
			}
		}
	}
}

void portmap_dump(unsigned char proto) {
	unsigned int i, k;
	portP p;

	if ((i = proto2index(proto)) == 2)
		return;
	for (k = 0; k < NPORTS; k++) {
		for (p = bin[i][k]; p != NULL; p = p->next) {
			printf( "%5d %s\n", p->port, p->name );
		}
	}
}
