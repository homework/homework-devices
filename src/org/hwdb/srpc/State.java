package org.hwdb.srpc;

/**
* @author Magnus Morton
*/
public enum State {
        IDLE,
        QACK_SENT,
        RESPONSE_SENT,
        CONNECT_SENT,
        QUERY_SENT,
        AWAITING_RESPONSE,
        TIMEDOUT,
        DISCONNECT_SENT,
        FRAGMENT_SENT,
        FACK_RECEIVED,
        FACK_SENT,
        SEQNO_SENT
}