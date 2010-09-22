#include "hashtable.h"
#include <stdlib.h>
#include <string.h>

#define TABLE_SIZE 101	/* default number of buckets */

typedef struct entrynode {
	struct entrynode *next;
	char *entry;
	void *value;
} EntryNode;

typedef struct htablenode {
	struct entrynode *first;
	unsigned long count;
} HTableNode;

struct hashtable {
	unsigned long nbuckets;
	unsigned long max_bucket;
	unsigned long nitems;
	HTableNode *table;
};

/* static internal function for computing hash value from string */
#define SHIFT 7		/* should be relatively prime to nbuckets */
static unsigned long hash(char *s, unsigned long nbuckets) {
	unsigned long hash = 0l;
	while (*s) {
		hash = ((SHIFT * hash) + *s++) % nbuckets;
	}
	return hash;
}

/* static internal function for locating entry in list
 * returns 1 if found, setting prev and cur appropriately
 * returns 0 if not found */
static int find_entry(Hashtable *t, char *key, EntryNode **prev, EntryNode **cur) {
	EntryNode *pr, *cu;
	unsigned long i;

	i = hash(key, t->nbuckets);
	pr = NULL;
	cu = (t->table[i]).first;
	while (cu != NULL) {
		if (strcmp(key, cu->entry) == 0) {
			*prev = pr;
			*cur = cu;
			return 1;
		}
		pr = cu;
		cu = pr->next;
	}
	return 0;
}

/* static internal function for duplicating string on heap */
static char *sdupl(char *s)
{
	int n = strlen(s) + 1;
	char *p = (char *)malloc(n);
	if (p)
		strcpy(p, s);
	return p;
}

/* ht_create - created a new hashtable */
Hashtable *ht_create(unsigned long nbuckets)
{
	Hashtable *p;
	HTableNode *htp;
	unsigned long i, N;

	N = ((nbuckets > 0) ? nbuckets : TABLE_SIZE);
	htp = (HTableNode *)calloc(N, sizeof(HTableNode));
	if (htp == NULL)
		return NULL;
	p = (Hashtable *)malloc(sizeof(Hashtable));
	if (p == NULL) {
		free((void *)htp);
		return NULL;
	}
	p->nbuckets = N;
	p->table = htp;
	p->max_bucket = 0L;
	p->nitems = 0L;
	for (i = 0; i < TABLE_SIZE; i++) {
		(p->table[i]).count = 0L;
		(p->table[i]).first = NULL;
	}
	return p;
}

/* ht_insert - adds an entry ot the hashtable
 * returns 1 if successful, 0 if error (malloc)
 * returns 1 if it is a duplicate */
int ht_insert(Hashtable *ht, char *key, void *value)
{
	EntryNode *prev, *cur;
	unsigned long i;
	char *s;

	if (find_entry(ht, key, &prev, &cur)) {	/* replace value */
		cur->value = value;
		return 1;
	}
	if ((cur = (EntryNode *)malloc(sizeof(EntryNode))) == NULL)
		return 0;
	s = sdupl(key);
	if (s == NULL) {
		free((void *)cur);
		return 0;
	}
	cur->entry = s;
	cur->value = value;
	i = hash(s, ht->nbuckets);
	cur->next = (ht->table[i]).first;
	(ht->table[i]).first = cur;
	(ht->table[i]).count++;
	if ((ht->table[i]).count > ht->max_bucket)
		ht->max_bucket = (ht->table[i]).count;
	ht->nitems++;
	return 1;
}

/* ht_lookup - lookup key in hashtable, returning value or NULL */
void *ht_lookup(Hashtable *ht, char *key)
{
	EntryNode *prev, *cur;

	if (find_entry(ht, key, &prev, &cur)) {	/* return value */
		return cur->value;
	}
	return NULL;
}

/* ht_remove - remove entry associated with key from the table */
void ht_remove(Hashtable *ht, char *key)
{
	EntryNode *prev, *cur;

	if (find_entry(ht, key, &prev, &cur)) {	/* remove it from list */
		unsigned long i = hash(key, ht->nbuckets);
		if (prev == NULL) {
			(ht->table[i]).first = cur->next;
		} else
			prev->next = cur->next;
		free((void *)(cur->entry));
		free((void *)cur);
		(ht->table[i]).count--;
		ht->nitems--;
	}
}

char **ht_keylist(Hashtable *ht, int *N)
{
	int n = ht->nitems;
	char **ans;
	unsigned long i;
	EntryNode *q;

	if (n > 0) {
		ans = (char **)malloc(n*sizeof(char *));
		if (! ans)
			*N = -1;
	} else {
		ans = NULL;
		*N = 0;
	}
	if (ans) {
		n = 0;
		for (i = 0; i < ht->nbuckets; i++) {
			for (q = (ht->table[i]).first; q != NULL; q = q->next) {
				ans[n] = q->entry;
				n++;
			}
		}
		*N = n;
	}
	return ans;
}
