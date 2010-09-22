/*
 * The Homework Database
 *
 * Singly-Linked List
 *
 * Authors:
 *    Oliver Sharma and Joe Sventek
 *     {oliver, joe}@dcs.gla.ac.uk
 *
 * (c) 2009. All rights reserved.
 */

#include "list.h"
#include "mem.h"

List *list_new() {
	List *ll;
	ll = mem_alloc(sizeof(List));
	ll->head = NULL;
	ll->tail = NULL;
	ll->len = 0;
	return ll;
}

void list_free(List *ll) {	
	LNode *p, *q;

	p = ll->head;
	while (p != NULL) {
		q = p->next;
		mem_free(p);
		p = q;
	}
	mem_free(ll);
}

int list_empty(List *ll) {
	return ll->len==0;
}

int list_len(List *ll) {
	return ll->len;
}

void list_append(List *ll, void *val) {
	LNode *n;
	
	
	n = mem_alloc(sizeof(LNode));
	n->val = val;
	n->next = NULL;
	
	if (list_empty(ll)) {
		ll->head = n;
		ll->tail = n;
	} else {
		ll->tail->next = n;
		ll->tail = n;
	}
	
	ll->len++;
}

void list_add_back(List *ll, void *val) {
	list_append(ll, val);
}

void list_add_front(List *ll, void *val) {
	LNode *n;
	
	n = mem_alloc(sizeof(LNode));
	n->val = val;
	n->next = NULL;
	
	if (list_empty(ll)) {
		ll->head = n;
		ll->tail = n;
	} else {
		n->next = ll->head;
		ll->head = n;
	}
	
	ll->len++;
}

void *list_get_first(List *ll) {
	return ll->head->val;
}

void *list_get_last(List *ll) {
	return ll->tail->val;
}

void *list_remove_first(List *ll) {
	LNode *tmp;
	void *val;
	
	if (list_empty(ll)) {
		return NULL;
	}
	
	tmp = ll->head;
	val = ll->head->val;
	
	ll->head = ll->head->next;
	
	ll->len--;
	mem_free(tmp);
	
	return val;
}

void **list_to_array(List *ll) {
	void **array;
	LNode *ptr;
	int i,n;
	
	if (list_empty(ll))
		return NULL;
	
	n = ll->len;
	array = mem_alloc(n * sizeof(void*));
	
	ptr = ll->head;
	for (i=0; i < n; i++) {
		array[i] = ptr->val;
		ptr = ptr->next;
	}
	
	return array;
}
