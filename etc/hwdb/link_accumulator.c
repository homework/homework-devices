/*
 * Accumulates link statistics.
 * Courtesy of accumulator.{h,c}
 */

#include "link_accumulator.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>

#define LINK_TABLE_SIZE 1001
#define NO_OF_LINKS 10000

static LinkTableElem *t0[LINK_TABLE_SIZE];
static LinkTableElem *t1[LINK_TABLE_SIZE];
static LinkTableElem **ptrs[2] = {t0, t1};
static int ndx;
static struct link_table {
    unsigned long nelems;
    LinkTableElem **ptrs;
} LinkTable;
static LinkTableElem elements[NO_OF_LINKS];
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static LinkTableElem *freel = NULL;

#define SHIFT 13

static unsigned int lt_hash(LinkRec *l) {
    unsigned int ans = 9;
    unsigned char *s = (unsigned char *)l;
    unsigned int i;
    for (i = 0; i < sizeof(LinkRec); i++)
	ans = (SHIFT * ans) + *s++;
    return ans;
}

static void lt_clear(LinkTableElem *elem) {
    elem->avg_rss = 0.0;
    elem->counter = 0;
    elem->retries = 0;
    elem->packets = 0;
    elem->nobytes = 0;
    memset(&(elem->id), 0, sizeof(LinkRec));
}

static void lt_free(LinkTableElem *elem) {
    lt_clear(elem);
    elem->next = freel;
    freel = elem;
}

static LinkTableElem *lt_alloc() {
    LinkTableElem *p = freel;
    if (p)
        freel = p->next;
    else if ((p = (LinkTableElem *)malloc(sizeof(LinkTableElem))))
        lt_clear(p);
    return p;
}

void lt_init() {
    int i;
    ndx = 0;
    LinkTable.ptrs = ptrs[ndx];
    LinkTable.nelems = 0;
    for (i = 0; i < LINK_TABLE_SIZE; i++)
        LinkTable.ptrs[i] = NULL;
    freel = NULL;
    for (i = 0; i < NO_OF_LINKS; i++) {
	lt_free(&elements[i]);
    }
}

LinkTableElem **lt_swap(int *size, unsigned long *nelems) {
    LinkTableElem **result;
    int i;

    pthread_mutex_lock(&mutex);
    result = LinkTable.ptrs;
    *nelems = LinkTable.nelems;
    *size = LINK_TABLE_SIZE;
    i = ++ndx % 2;
    LinkTable.ptrs = ptrs[i];
    LinkTable.nelems = 0;
    for (i = 0; i < LINK_TABLE_SIZE; i++)
        LinkTable.ptrs[i] = NULL;
    pthread_mutex_unlock(&mutex);
    return result;
}

static LinkTableElem *lt_lookup(LinkRec *f) {
    unsigned int ndx = lt_hash(f) % LINK_TABLE_SIZE;
    LinkTableElem *p;

    for (p = LinkTable.ptrs[ndx]; p != NULL; p = p->next)
        if (memcmp(f, &(p->id), sizeof(LinkRec)) == 0)
            return p;
    p = lt_alloc();
    if (p) {
        memcpy(&(p->id), f, sizeof(LinkRec));
	p->next = LinkTable.ptrs[ndx];
	LinkTable.ptrs[ndx] = p;
	LinkTable.nelems++;
    }
    return p;
}

void lt_update (uint64_t addr, int rss, int retry, unsigned long bytes) {
    LinkRec t;
    LinkTableElem *p;
    memset(&t, 0, sizeof(LinkRec));
    t.mac = addr;
    pthread_mutex_lock(&mutex);
    p = lt_lookup(&t);
    if (p) {
	if (rss != 0) {
		if (p->avg_rss == 0.0) p->avg_rss = (double) rss;
		else
		   (p->avg_rss) = ((double)rss + (double)(p->counter) * (p->avg_rss))/
			(double)(++(p->counter));
	}
	p->retries += retry;
	p->packets ++;
	p->nobytes += bytes;
    }
    pthread_mutex_unlock(&mutex);
}

static void lt_element_dump(LinkTableElem *p) {
    LinkRec *l = &(p->id);
    printf("%lld:%f:%5ld:%5ld:%10ld\n", 
	l->mac, p->avg_rss, p->retries, p->packets, p->nobytes);
}

void lt_dump() {
    int i;
    pthread_mutex_lock(&mutex);
    printf("%ld links in the accumulator:\n", LinkTable.nelems);
    for (i = 0; i < LINK_TABLE_SIZE; i++) {
        LinkTableElem *p;
	for (p = LinkTable.ptrs[i]; p != NULL; p++)
            lt_element_dump(p);
    }
    pthread_mutex_unlock(&mutex);
}

void lt_freeChain(LinkTableElem *elem) {
    LinkTableElem *p, *q;

    pthread_mutex_lock(&mutex);
    p = elem;
    while (p != NULL) {
        q = p->next;
	lt_free(p);
	p = q;
    }
    pthread_mutex_unlock(&mutex);
}

