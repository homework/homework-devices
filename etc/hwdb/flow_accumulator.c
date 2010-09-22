/*
 * source for flow accumulator
 */

#include "flow_accumulator.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <arpa/inet.h>

#define ATABLE_SIZE 1001
#define NO_OF_ELEMS 10000

static AccElem *t0[ATABLE_SIZE];	/* one table */
static AccElem *t1[ATABLE_SIZE];	/* the other table */
static AccElem **ptrs[2] = {t0, t1};	/* current table to use */
static int ndx;				/* index into ptrs[] */
static struct table {
    unsigned long nelems;
    AccElem **ptrs;
} theTable;
static AccElem elements[NO_OF_ELEMS];	/* initial elements on free list */
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static AccElem *freel = NULL;		/* pointer to free list */

#define SHIFT 13
static unsigned int acc_hash(FlowRec *f) {
    unsigned int ans = 9;
    unsigned char *s = (unsigned char *)f;
    unsigned int i;

    for (i = 0; i < sizeof(FlowRec); i++)
	ans = (SHIFT * ans) + *s++;
    return ans;
}

static void acc_clear(AccElem *elem) {
    elem->packets = 0;
    elem->bytes = 0;
    memset(&(elem->id), 0, sizeof(FlowRec));
}

static void acc_free(AccElem *elem) {
    acc_clear(elem);
    elem->next = freel;
    freel = elem;
}

static AccElem *acc_alloc() {
    AccElem *p = freel;
    if (p)
        freel = p->next;
    else if ((p = (AccElem *)malloc(sizeof(AccElem))))
        acc_clear(p);
    return p;
}

void acc_init() {
    int i;

    ndx = 0;
    theTable.ptrs = ptrs[ndx];
    theTable.nelems = 0;
    for (i = 0; i < ATABLE_SIZE; i++)
        theTable.ptrs[i] = NULL;
    freel = NULL;			/* build free list */
    for (i = 0; i < NO_OF_ELEMS; i++) {
	acc_free(&elements[i]);
    }
}

AccElem **acc_swap(int *size, unsigned long *nelems) {
    AccElem **result;
    int i;

    pthread_mutex_lock(&mutex);
    result = theTable.ptrs;
    *nelems = theTable.nelems;
    *size = ATABLE_SIZE;
    i = ++ndx % 2;
    theTable.ptrs = ptrs[i];
    theTable.nelems = 0;
    for (i = 0; i < ATABLE_SIZE; i++)
        theTable.ptrs[i] = NULL;
    pthread_mutex_unlock(&mutex);
    return result;
}

static AccElem *acc_lookup(FlowRec *f) {
    unsigned int ndx = acc_hash(f) % ATABLE_SIZE;
    AccElem *p;

    for (p = theTable.ptrs[ndx]; p != NULL; p = p->next)
        if (memcmp(f, &(p->id), sizeof(FlowRec)) == 0)
            return p;
    p = acc_alloc();
    if (p) {
        memcpy(&(p->id), f, sizeof(FlowRec));
	p->next = theTable.ptrs[ndx];
	theTable.ptrs[ndx] = p;
	theTable.nelems++;
    }
    return p;
}

void acc_update(struct in_addr src, struct in_addr dst,
		unsigned short sport, unsigned short dport,
		unsigned long proto, unsigned long bytes) {
    FlowRec t;
    AccElem *p;

    memset(&t, 0, sizeof(FlowRec));		/* clear the bytes */
    t.ip_src = src;
    t.ip_dst = dst;
    t.sport = sport;
    t.dport = dport;
    t.proto = proto;
    pthread_mutex_lock(&mutex);
    p = acc_lookup(&t);
    if (p) {
        p->packets++;
	p->bytes += bytes;
    }
    pthread_mutex_unlock(&mutex);
}

static void acc_element_dump(AccElem *p) {
    FlowRec *f = &(p->id);
    printf("%u:", f->proto);
    printf("%s:%05hu:", inet_ntoa(f->ip_src), f->sport);
    printf("%s:%05hu:", inet_ntoa(f->ip_dst), f->dport);
    printf("%5ld:%10ld\n", p->packets, p->bytes);
}

void acc_dump() {
    int i;
    pthread_mutex_lock(&mutex);
    printf("%ld flows in the accumulator\n", theTable.nelems);
    for (i = 0; i < ATABLE_SIZE; i++) {
        AccElem *p;
	for (p = theTable.ptrs[i]; p != NULL; p++)
            acc_element_dump(p);
    }
    pthread_mutex_unlock(&mutex);
}

void acc_freeChain(AccElem *elem) {
    AccElem *p, *q;

    pthread_mutex_lock(&mutex);
    p = elem;
    while (p != NULL) {
        q = p->next;
	acc_free(p);
	p = q;
    }
    pthread_mutex_unlock(&mutex);
}
    
