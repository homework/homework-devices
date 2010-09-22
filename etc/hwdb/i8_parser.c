#include "i8_parser.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

uint64_t address_field (const uint8_t *data) {
	return ((((((((((((uint64_t) (data[0]))
		<< 8) | data[1])
		<< 8) | data[2])
		<< 8) | data[3])
		<< 8) | data[4])
		<< 8) | data[5]);
}

char *mac_to_string (uint64_t mac) {
	char *address;
	char buff[24];
	sprintf( buff, "%02x:%02x:%02x:%02x:%02x:%02x",
		(int) ((mac>>40) & 0xff),
		(int) ((mac>>32) & 0xff),
		(int) ((mac>>24) & 0xff),
		(int) ((mac>>16) & 0xff),
		(int) ((mac>> 8) & 0xff),
		(int) ((mac    ) & 0xff)
	);
	address = malloc(strlen(buff)+1);
	strcpy (address, buff);
	return address;
}

uint64_t string_to_mac(char *s) {
    int b[6], i;
    uint64_t ans;

    sscanf(s, "%02x:%02x:%02x:%02x:%02x:%02x",
	      &b[0], &b[1], &b[2], &b[3], &b[4], &b[5]);
    ans = 0LL;
    for (i = 0; i < 6; i++)
        ans = ans << 8 | (b[i] & 0xff);
    return ans;
}

void sample_control_frame (uint16_t fc) {
	switch (SUBTYPE(fc)) {
		case C_RTS:
			printf("RTS\n");
			break;
		case C_CTS:
			printf("CTS\n");
			break;
		case C_ACK:
			printf("ACK\n");
			break;
		default:
			break;
	}
	return;
}

void sample_management_frame (uint16_t fc, 
	uint64_t sa, uint64_t da) {
	char *sas = mac_to_string(sa); 
	char *das = mac_to_string(da);
	switch (SUBTYPE(fc)) {
		case ASSOCIATION_REQUEST:
printf("Association request from %s to %s\n", sas, das);
			break;
		case ASSOCIATION_RESPONSE:
printf("Association response from %s to %s\n", sas, das);
			break;
		case REASSOCIATION_REQUEST:
printf("Re-association request from %s to %s\n", sas, das);
			break;
		case REASSOCIATION_RESPONSE:
printf("Re-Association response from %s to %s\n", sas, das);
			break;
		case PROBE_REQUEST:
printf("Probe request from %s to %s\n", sas, das);
			break;
		case PROBE_RESPONSE:
printf("Probe response from %s to %s\n", sas, das);
			break;
		case BEACON:
printf("Beacon from %s to %s\n", sas, das);
			break;
		case ATIM:
printf("ATIM from %s to %s\n", sas, das);
			break;
		case DISASSOCIATION:
printf("Disassociation from %s to %s\n", sas, das);
			break;
		case AUTHENTICATION:
printf("Authentication from %s to %s\n", sas, das);
			break;
		case DEAUTHENTICATION:
printf("Deauthentication from %s to %s\n", sas, das);
			break;
		default:
			break;
	}
	free(sas);
	free(das);

	return;
}

