#ifndef _MY_RADIOTAP_PARSER_H_INCLUDED_
#define _MY_RADIOTAP_PARSER_H_INCLUDED_

#include <stdint.h>
#include <pcap.h>

/*
 * According to www.radiotap.org, the radiotap header is
 */
typedef struct rt_header {
	uint8_t it_version; /* Currently, version is always 0. */
	uint8_t it_pad;
	uint16_t it_len;
	uint32_t it_present; /* The it_present bitmask; see rt_field_t. */
} __attribute__((packed)) rt_header_t;

typedef enum {
	RT_TSFT                    =  0,
	RT_FLAGS                   =  1,
	RT_RATE                    =  2,
	RT_CHANNEL                 =  3,
	RT_FHSS                    =  4,
	RT_DBM_ANTSIGNAL           =  5,
	RT_DBM_ANTNOISE            =  6,
	RT_LOCK_QUALITY            =  7,
	RT_TX_ATTENUATION          =  8,
	RT_DB_TX_ATTENUATION       =  9,
	RT_DBM_TX_POWER            = 10,
	RT_ANTENNA                 = 11,
	RT_DB_ANTSIGNAL            = 12,
	RT_DB_ANTNOISE             = 13,
	RT_RX_FLAGS                = 14,
	RT_TX_FLAGS                = 15,
	RT_RTS_RETRIES             = 16,
	RT_DATA_RETRIES            = 17,
	RT_EXT                     = 31
	// Extend as needed.	
} rt_field_t;

/*

According to radiotap.c (in-kernel version), the 
alignment and size of fields are as follows.

static const struct radiotap_align_size radiotap_namespace_sizes [] = {
	[RT_TSFT]                    = { .align = 8, .size = 8, },
	[RT_FLAGS]                   = { .align = 1, .size = 1, },
	[RT_RATE]                    = { .align = 1, .size = 1, },
	[RT_CHANNEL]                 = { .align = 2, .size = 4, },
	[RT_FHSS]                    = { .align = 2, .size = 2, },
	[RT_DBM_ANTSIGNAL]           = { .align = 1, .size = 1, },
	[RT_DBM_ANTNOISE]            = { .align = 1, .size = 1, },
	[RT_LOCK_QUALITY]            = { .align = 2, .size = 2, },
	[RT_TX_ATTENUATION]          = { .align = 2, .size = 2, },
	[RT_DB_TX_ATTENUATION]       = { .align = 2, .size = 2, },
	[RT_DBM_TX_POWER]            = { .align = 1, .size = 1, },
	[RT_ANTENNA]                 = { .align = 1, .size = 1, },
	[RT_DB_ANTSIGNAL]            = { .align = 1, .size = 1, },
	[RT_DB_ANTNOISE]             = { .align = 1, .size = 1, },
	[RT_RX_FLAGS]                = { .align = 2, .size = 2, },
	[RT_TX_FLAGS]                = { .align = 2, .size = 2, },
	[RT_RTS_RETRIES]             = { .align = 1, .size = 1, },
	[RT_DATA_RETRIES]            = { .align = 1, .size = 1, },
};

*/

// According to "litrace.h", do byte swap as follows.
uint16_t byteswap16(uint16_t a);
uint32_t byteswap32(uint32_t a);
uint64_t byteswap64(uint64_t a);

#if BYTE_ORDER == BIG_ENDIAN

#define bswap_host_to_be64(num) ((uint64_t) (num))
#define bswap_host_to_le64(num) (byteswap64 (num))
#define bswap_host_to_be32(num) ((uint32_t) (num))
#define bswap_host_to_le32(num) (byteswap32 (num))
#define bswap_host_to_be16(num) ((uint16_t) (num))
#define bswap_host_to_le16(num) (byteswap16 (num))

#define bswap_be_to_host64(num) ((uint64_t) (num))
#define bswap_le_to_host64(num) (byteswap64 (num))
#define bswap_be_to_host32(num) ((uint32_t) (num))
#define bswap_le_to_host32(num) (byteswap32 (num))
#define bswap_be_to_host16(num) ((uint16_t) (num))
#define bswap_le_to_host16(num) (byteswap16 (num))

#elif BYTE_ORDER == LITTLE_ENDIAN
 
#define bswap_host_to_be64(num) (byteswap64 (num))
#define bswap_host_to_le64(num) ((uint64_t) (num))
#define bswap_host_to_be32(num) (htonl (num))
#define bswap_host_to_le32(num) ((uint32_t) (num))
#define bswap_host_to_be16(num) (htons (num))
#define bswap_host_to_le16(num) ((uint16_t) (num))

#define bswap_be_to_host64(num) (byteswap64 (num))
#define bswap_le_to_host64(num) ((uint64_t) (num))
#define bswap_be_to_host32(num) (ntohl (num))
#define bswap_le_to_host32(num) ((uint32_t) (num))
#define bswap_be_to_host16(num) (ntohs (num))
#define bswap_le_to_host16(num) ((uint16_t) (num))

#else

#error "Error: unknown byte order."

#endif

// Radiotap requires that all fields are naturally aligned.
#define NATURALLY_ALIGN_16BITS(_i, _j) \
	while ( (_i - _j) % sizeof(uint16_t) ) _i++

int8_t get_rss (const u_char* data);

uint16_t get_radiotap_header_length (const u_char* data);

#endif /* _RT_PARSER_H_INCLUDED */
