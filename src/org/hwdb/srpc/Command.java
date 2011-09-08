package org.hwdb.srpc;

/**
* @author Magnus Morton
*/
public enum Command {
        ERROR,
        CONNECT,
        CACK,
        QUERY,
        QACK,
        RESPONSE,
        RACK,
        DISCONNECT,
        DACK,
        FRAGMENT,
        FACK,
        PING,
        PACK,
        SEQNO,
        SACK
}
