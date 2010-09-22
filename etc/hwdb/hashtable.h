#ifndef _HASHTABLE_INCLUDED_
#define _HASHTABLE_INCLUDED_

typedef struct hashtable Hashtable;

/* ht_create - create new hashtable - returns NULL if error (malloc) */
Hashtable *ht_create(unsigned long nbuckets);

/* ht_insert - add an entry to hashtable
 * returns 1 if successful, 0 if error (malloc)
 * returns 1 if if replaced value associated with key */
int ht_insert(Hashtable *ht, char *key, void *value);

/* ht_lookup - look up key in hashtable, returning value or NULL */
void *ht_lookup(Hashtable *ht, char *key);

/* ht_remove - remove entry associated with key from the table */
void ht_remove(Hashtable *ht, char *key);

/* ht_keylist - return list of keys in the table
 * upon return, N has number of items in array of pointers
 * if allocation failure of array of pointers, N will be -1
 * return value is array of pointers that must be freed after use */
char **ht_keylist(Hashtable *ht, int *N);

#endif /* _HASHTABLE_INCLUDED_ */
