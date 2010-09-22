#include  <stdio.h>
#include <stdlib.h>
#include <string.h>
#include  <ctype.h>

#include "protomap.h"

#define NPROTOCOLS 256

static int verbose = 0;

static char* bin[NPROTOCOLS];

static void test () {
	unsigned int i;
	for (i = 0; i < NPROTOCOLS; i++)
		if (bin[i] == NULL)
			fprintf( stderr, 
	"Error: protocol %d is not specified.\n", i );
}

static void add(char *filename) {
	unsigned int charndx, protondx, namendx;
	FILE *f;
	char line[256];
	char proto[128];
	char name[128];
	enum {IN_VOID, IN_PROTO, IN_BTWN, IN_NAME} pos;
	unsigned int NR = 0; 	// Line index (for error messages)
	unsigned int lines = 0;

	unsigned char protocol_num;
	
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
		protondx = 0;
		namendx = 0;
		pos = IN_VOID;
		while (charndx < strlen(line)) {
			int c = line[charndx];
			if (pos == IN_VOID && !isspace(c)) {
				// The first word is the protocol number.
				pos = IN_PROTO;
			} else if (pos == IN_PROTO && (isspace(c) || ispunct(c))) {
				pos = IN_BTWN;
			} else if (pos == IN_BTWN && !isspace(c)) {
				// From the second word onwards 
				// begins the protocol name.
				pos = IN_NAME;
			}
			if (pos == IN_PROTO) {
				if (! isdigit(c)) {
fprintf(stderr, "Error in %s (line %d): protocol number should be numerical.\n", 
	filename, NR);
					return;
				}
				proto[protondx] = line[charndx];
				protondx++;
			} else
			if (pos == IN_NAME) {
				if (! iscntrl(c)) {
					name[namendx] = line[charndx];
					namendx++;
				}
			}
			charndx++;
		}
		proto[protondx] = '\0';
		name[namendx] = '\0';
		if (pos == IN_VOID) // Skip empty line.
			continue;
		
		// Add to table.
		protocol_num = atoi(proto);
		bin[protocol_num] = strdup(name);
		if (verbose) printf ( "protocol %d is %s\n", protocol_num, bin[protocol_num] );
		// Added. 
		lines += 1;
	}
	fclose(f);
	if (verbose) printf("%d lines processed.\n", lines);
	test();
	return;
}

void protomap_init (char *file) {
	unsigned int i;

	for (i = 0; i < NPROTOCOLS; i++)
		bin[i] = NULL;
	add(file);
}

char *protomap_classify(unsigned char proto) {
	return ((bin[proto] != NULL) ? bin[proto] : "error");
}

void protomap_free() {
	unsigned int i;
	for (i = 0; i < NPROTOCOLS; i++) {
		if (bin[i]) free(bin[i]);
	}
}

void protomap_dump() {
	unsigned int i;
	for (i = 0; i < NPROTOCOLS; i++) {
		printf( "%d %s\n", i, bin[i] );
	}
}
