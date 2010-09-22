/*
 * Maps ip address to host names.
 */
#include "hostmap.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#include "tslist.h"
#include <pthread.h>

/*
 * Static definitions for the hash table.
 */
typedef struct ht_node *hostP;
typedef struct ht_node {
	hostP next;
	in_addr_t ip_addr;
	char *name;
} ht_node_t;

/* There are 4294967296 unique IPv4 addresses. */
#define NHOSTS 1001
/*
 * The hash table that contains {ip address, hostname} pairs is bounded.
 */
static hostP bin[NHOSTS];

static unsigned int hostmap_hash (in_addr_t ip_addr);
static hostP hostmap_lookup (in_addr_t ip_addr);
static int  hostmap_insert (in_addr_t ip_addr, char *name);
/*
 * Update is called by the thread handler; an entry for ip_addr should already exists.
 */
static void hostmap_update (in_addr_t ip_addr, char *name);

static int verbose = 0; /* For debugging purposes. */

/*
 * Static definitions for the worker thread. 
 * The pending_queue contains unresolved IP
 * addresses.
 */
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static TSList pending_queue;
static pthread_t thr;
static void *handler(void *args);


static void *handler (void *args) {
	in_addr_t addr;
	void *data = args;
	int size;
	
	struct sockaddr_in sa;
	int error;
	char hostname[HOSTNAME_LEN];

	for (;;) {
		tsl_remove (pending_queue, (void **)&addr, &data, &size);
		if (verbose) {
			char *s = strdup(inet_ntoa(*(struct in_addr *)&addr));
			printf("In thread: attempt to resolve address %s\n", s);
			free(s);
		}
		/* Now resolve addr. */
		memset (&sa, 0, sizeof(struct sockaddr_in));
		memcpy (&sa.sin_addr, &addr, sizeof(in_addr_t));
		sa.sin_family = AF_INET; // Only IPv4 support for now.
		error = getnameinfo( ((struct sockaddr*) (&sa)),
sizeof(struct sockaddr_in), hostname, HOSTNAME_LEN, NULL, 0, NI_NOFQDN);
		if (error != 0 && verbose) {
			fprintf( stderr, "Could not resolve name: %s\n", 
				gai_strerror(error));
		} else {
			if (verbose) 
				printf( "In thread: address resolved to %s\n", hostname);
			/* Update the value (name) of key (addr) in the hash table. */
			hostmap_update(addr, hostname);
		}
	}
	return (args ? NULL : args);
}

/*
 * Robert Jenkins' 32 bit integer hash function
 *
 * in_addr_t is an unsigned 32 bit.
 */

static unsigned int hostmap_hash (in_addr_t ip_addr) {
	uint32_t a = (uint32_t) ip_addr;
	// Shuffle...
	a = (a+0x7ed55d16) + (a<<12);
	a = (a^0xc761c23c) ^ (a>>19);
	a = (a+0x165667b1) + (a<< 5);
	a = (a+0xd3a2646c) ^ (a<< 9);
	a = (a+0xfd7046c5) + (a<< 3);
	a = (a^0xb55a4f09) ^ (a>>16);

	if (verbose) printf("Hash value is %u\n", ((unsigned int) a));

	return ((unsigned int) a);
}

/*
 * In hostmap_lookup and in hostmap_insert, the table is locked
 * by hostmap_resolve.
 */
static hostP hostmap_lookup (in_addr_t ip_addr) {
	hostP p;
	unsigned int hv; 
	hv = hostmap_hash(ip_addr) % NHOSTS;
	for (p = bin[hv]; p != NULL; p = p->next) {
		if (ip_addr == p->ip_addr) 
			return p;
	}
	return NULL;
}

static int hostmap_insert (in_addr_t ip_addr, char *name) {
	unsigned int hv = hostmap_hash(ip_addr) % NHOSTS;
	hostP p = malloc(sizeof(ht_node_t));
	if (p == 0) {
		/* Out of memory */
		return -1;
	} else {
		p->ip_addr = ip_addr;
		/*
		 * In any case, inserts should be performed 
		 * before an ip address is resolved, there-
		 * fore it can also be
		 * 
		 * p->name = strdup(inet_ntoa(*(struct in_addr *)&ip_addr));
		 *
		 */
		p->name = strdup(name);
		p->next = bin[hv];
		bin[hv] = p;
		if (verbose) printf("New entry added.\n");
		return 0;
	}
}

static void hostmap_update (in_addr_t ip_addr, char *name) {
	hostP p;
	unsigned int hv = hostmap_hash(ip_addr) % NHOSTS;
	if (verbose) printf("Locking table.\n");
	pthread_mutex_lock(&mutex);
	if (verbose) printf("Table locked.\n");
	for (p = bin[hv]; p != NULL; p = p->next) {
		if (memcmp (&ip_addr, &p->ip_addr, sizeof(in_addr_t)) == 0) { 
			/* Entry exists, update. */
			if (strlen(p->name) < strlen(name)) {
				free(p->name);
				p->name = strdup(name);
			} else {
				strcpy(p->name, name);
			}
			/* Entry updated; exit. */
			if (verbose) printf("Entry updated.\n");
			break;
		}
	}
	pthread_mutex_unlock(&mutex);
	if (verbose) printf("Table unlocked.\n");
	return;
}


void hostmap_init() {
	unsigned int i;
	for (i = 0; i < NHOSTS; i++) 
		bin[i] = NULL;
	if (verbose) printf("Hash table initialized.\n");
	pending_queue = tsl_create();
	if (verbose) printf("Unbounded, thread-safe queue created.\n");
	if (pthread_create(&thr, NULL, handler, NULL)) {
		fprintf(stderr, "Failure to start hostmap thread.\n");
		exit(1);
	} else if (verbose) printf("Thread (resolver) created.\n");
	return;
}

/*
 * This is the method called from flowmonitor:
 * a) if IP exists in hash table, return name.
 * b) else
 *    b1) add to hash table; this avoids duplicate entries in the pending_queue;
 *    b2) append to pending_queue.
 *
 */
char *hostmap_resolve(in_addr_t ip_addr) {
	hostP p;
	int ret; /* 0 upon successful insert, otherwise -1 */
	char *ans;
	if (verbose) printf("Locking table.\n");
	pthread_mutex_lock(&mutex);
	if (verbose) printf("Table locked.\n");
	p = hostmap_lookup(ip_addr);
	if (p) {
		ans = strdup(p->name);
		// return ans;
	} else {
		/* If not found, return the ip address as a string. */
		ans = strdup(inet_ntoa(*(struct in_addr *)&ip_addr));
		/* Add new entry to avoid duplicates. */
		ret = hostmap_insert (ip_addr, ans);
		if (ret == 0) {
			/* Submit as unresolved. */
			tsl_append(pending_queue, (void *)ip_addr, NULL, 0);
		} else {
			fprintf(stderr, "Out of memory: will not resolve %s\n", ans);
		}
	}
	pthread_mutex_unlock(&mutex);
	if (verbose) printf("Table unlocked.\n");
	return ans;
}

void hostmap_free() {
	unsigned int i;
	hostP p, q;
	if (verbose) printf("Locking table.\n");
	pthread_mutex_lock(&mutex);
	if (verbose) printf("Table locked.\n");

	for (i = 0; i < NHOSTS; i++) {
		for (p = bin[i]; p != NULL; p = q) {
			q = p->next;
			free(p->name);
			free(p);
		}
	}
	
	pthread_mutex_unlock(&mutex);
	if (verbose) printf("Table unlocked.\n");
	
	return;
}

void hostmap_dump() {
	unsigned int i;
	/* Statistics */
	uint32_t chain_size, total_size;
	uint32_t max_chain = 0;
	hostP p;

	if (verbose) printf("Locking table.\n");
	pthread_mutex_lock(&mutex);
	if (verbose) printf("Table locked.\n");
	
	total_size = 0;
	for (i = 0; i < NHOSTS; i++) {
		chain_size = 0;
		for (p = bin[i]; p != NULL; p = p->next) {
			chain_size++;
			total_size++;
		}
		if (chain_size > 1) {
			printf( "chain of %ld is %ld\n", (unsigned long) i,
				(unsigned long) chain_size);
			if (chain_size > max_chain) max_chain = chain_size;
		}
	}
	printf( "max chain is %ld\n", (unsigned long) max_chain);
	printf( "total size is %ld\n", (unsigned long) total_size);
	
	pthread_mutex_unlock(&mutex);
	if (verbose) printf("Table unlocked.\n");
	
	return;
}

