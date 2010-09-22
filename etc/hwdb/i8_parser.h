#ifndef _MY_IEEE80211_PARSER_H_INCLUDED_
#define _MY_IEEE80211_PARSER_H_INCLUDED_

#include <stdint.h>

/* In a MAC frame, bits 3 and 2 represent its type. In particular,
 * 00 is management; 01 is control; 10 is data; and 11 is reserved.
 */

#define TYPE_MGMT 0x0
#define TYPE_CTRL 0x1
#define TYPE_DATA 0x2
#define TYPE_RESV 0x3
#define    TYPE(fc) (((fc) >> 2) & 0x3)
#define	SUBTYPE(fc) (((fc) >> 4) & 0xF)

/* Below are defined but a few of the packet subtypes for (a) management
 * and (b) control frames.
 */

#define	ASSOCIATION_REQUEST 		0x0	/* (a) */
#define ASSOCIATION_RESPONSE 		0x1
#define	REASSOCIATION_REQUEST 		0x2
#define	REASSOCIATION_RESPONSE 		0x3
#define	PROBE_REQUEST 			0x4
#define	PROBE_RESPONSE 			0x5
#define	BEACON 				0x8
#define	ATIM 				0x9
#define	DISASSOCIATION 			0xA
#define	AUTHENTICATION 			0xB
#define	DEAUTHENTICATION 		0xC

#define	C_PS_POLL 			0xA	/* (b) */
#define	C_RTS 				0xB
#define	C_CTS 				0xC
#define	C_ACK 				0xD
#define	C_CF_END 			0xE
#define	C_CF_END_AND_CF_ACK 		0xF

/* Bits 8 and 9 define the direction of packets; To DS; from DS.
 */

#define TDS(fc) 	((fc) & 0x0100)
#define FDS(fc)		((fc) & 0x0200)

/* Bit 11 defines whether a frame is a retransmission or not.
 */

#define	RETRY(fc)	((fc) & 0x0800)

/* Bit 14 defines whether a frame is encrypted or not. Note that
 * if the bit is set, then one CANNOT access the snap header and
 * subsequently the ip header of a packet.
 */

#define	PROTECTED(fc)	((fc) & 0x4000)

/* The length of data frames. */

#define	IEEE80211_DATA_HEADER_LENGTH 24

#define EXTRACT_LE_16BITS(p) \
	((uint16_t)((uint16_t)*((const uint8_t *)(p) + 1) << 8 | \
		(uint16_t)*((const uint8_t *)(p) + 0)))

#define EXTRACT_BE_16BITS(p) \
	((uint16_t)((uint16_t)*((const uint8_t *)(p) + 0) << 8 | \
		(uint16_t)*((const uint8_t *)(p) + 1)))

uint64_t address_field (const uint8_t *data);
char *mac_to_string (uint64_t mac);
uint64_t string_to_mac (char *s);

void sample_control_frame (uint16_t fc);
void sample_management_frame (uint16_t fc, uint64_t sa, uint64_t da);

#endif /*  _MY_IEEE80211_PARSER_H_INCLUDED_ */



