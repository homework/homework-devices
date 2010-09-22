#include "rt_parser.h"

uint16_t byteswap16(uint16_t a) { 
	return ( (a << 8) & 0xFF00 ) | ( (a >> 8) & 0x00FF );
}

uint32_t byteswap32(uint32_t a) {
	return 
		( (a & 0x000000FFU) << 24 ) | 
		( (a & 0x0000FF00U) <<  8 ) | 
		( (a & 0x00FF0000U) >>  8 ) | 
		( (a & 0xFF000000U) >> 24 );
}

uint64_t byteswap64(uint64_t a) {
	return 
		( byteswap32( (a & 0xFFFFFFFF00000000ULL) >> 32 ) ) | 
		( (uint64_t) byteswap32(a & 0x00000000FFFFFFFFULL) << 32 );
}

int8_t get_rss (const u_char* data) {
	rt_header_t* rt = (rt_header_t *) data;
	uint8_t *i;
	uint32_t bitmap = bswap_le_to_host32 (rt->it_present);
	if (! (bitmap & (1 << RT_DBM_ANTSIGNAL)) )
		return 0;
	i = (uint8_t *) &(rt->it_present);
	/* If bit 31 of the it_present field is set, an extended 
	 * it_present bitmask is present.
	 */
	while ( bswap_le_to_host32(*((uint32_t*) i)) & (1 << RT_EXT) ) {
		i += sizeof (uint32_t);
	}
	/* Skip to radiotap data. */
	i += sizeof(uint32_t);
	if (bitmap & (1 << RT_TSFT)) {
		// { return (void *) i; }
		i += sizeof(uint64_t);
	}
	if (bitmap & (1 << RT_FLAGS)) {
		// { return (void *) i; }
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_RATE)) {
		// { return (void *) i; }
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_CHANNEL)) {
		/* Align and
		return (void *) i; */
		i += sizeof(uint32_t);
	}
	if (bitmap & (1 << RT_FHSS)) {
		/* Align and
		return (void *) i; */
		i += sizeof(uint16_t);
	}
	if (bitmap & (1 << RT_DBM_ANTSIGNAL)) {
		return (int8_t) *i;
		/* Or continue looking through the rest of the data, hence
		i += sizeof(uint8_t); */
	}
	
	/*
	if (bitmap & (1 << RT_DBM_ANTNOISE)) {
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_LOCK_QUALITY)) {
		i += sizeof(uint16_t);
	}
	if (bitmap & (1 << RT_TX_ATTENUATION)) {
		i += sizeof(uint16_t);
	}
	if (bitmap & (1 << RT_DB_TX_ATTENUATION)) {
		i += sizeof(uint16_t);
	}
	if (bitmap & (1 << RT_DBM_TX_POWER)) {
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_ANTENNA)) {
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_DB_ANTSIGNAL)) {
		i += sizeof(uint8_t);
	}
	if (bitmap & (1 << RT_DB_ANTNOISE)) {
		i += sizeof(uint8_t);
	}
        if (bitmap & (1 << RT_RX_FLAGS)) {
                i += sizeof(uint16_t);
	}
        if (bitmap & (1 << RT_TX_FLAGS)) {
                i += sizeof(uint16_t);
	}
        if (bitmap & (1 << RT_RTS_RETRIES)) {
                i += sizeof(uint8_t);
	}
        if (bitmap & (1 << RT_DATA_RETRIES)) {
		return;
	}
	*/

	/* Unknown field */
	return 0;
}

uint16_t get_radiotap_header_length (const u_char* data) {
        rt_header_t* rt = (rt_header_t *) data;
        return bswap_le_to_host16 (rt->it_len);
}

