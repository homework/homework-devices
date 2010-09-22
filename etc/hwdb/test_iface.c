/*
 * This is a simple program that checks the type of a given interface.
 * It is used by the hwdb.sh script to check whether the provided int-
 * erfaces are of type
 * - EN10MB or
 * - IEEE802_11_RADIO
 * 
 * for the
 * 
 * - flowlogger and
 * - linklogger
 * 
 * respectively.
 *
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <pcap.h>
#include <netinet/ether.h>

static int verbose = 0;
int main(int argc, char *argv[]) {
	if (argc != 3) {
		fprintf(stderr, "usage: test_iface <interface> <flow|link>\n");
		return EXIT_FAILURE;
	}
	char *dev = argv[1];
	char *opt = argv[2];
	pcap_t *task;
	int type;
	char err[PCAP_ERRBUF_SIZE];
	task = pcap_open_live(dev, BUFSIZ, 1, 1000, err);
	if (task == NULL) {
		if (verbose) 
			fprintf(stderr, "Open error: %s\n", dev);
		return EXIT_FAILURE;
	}
	type = pcap_datalink(task);
	if (verbose) printf("%s\n", pcap_datalink_val_to_name(type));
	int valid = 0;
	if (strcmp(opt, "flow") == 0) {
		if (type == DLT_EN10MB) valid = 1;
		else valid = 0;
	} else if (strcmp(opt, "link") == 0) {
		if (type == DLT_IEEE802_11_RADIO) valid = 1;
		else valid = 0;
	}
	pcap_close(task);
	if (valid)
	    return EXIT_SUCCESS;
	else
	    return EXIT_FAILURE;
}
