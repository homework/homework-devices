package org.hwdb.srpc;


import java.nio.channels.DatagramChannel;
import java.io.IOException;
import java.util.EnumSet;
//import java.util.logging.Level;

/**
* @author Magnus Morton
*/
public class Connection {
    
    private Endpoint    source;
    private SRPC        context;
    private Service     service;
    private int         seqNo;
    private String      resp;
    private State       state;
    private int         ticks;
    private int         ticksLeft;
    private int         ticksTilPing;
    private int         nattempts;
    private int         pingsTilPurge;
    private String      data;
    private Payload     lastPayload;
    private byte        lastFrag;

    static final int    SEQNO_LIMIT = 1000000000;
    static final int    SEQNO_START = 0;
    static final int    MAX_LENGTH  = 65535;

    protected Connection(SRPC context, Endpoint source, Service service) {
        this.context       = context;
        this.source        = source;
        this.service       = service;
        this.ticks         = 0;
        this.ticksLeft     = 0;
        this.ticksTilPing  = SRPC.TICKS_BETWEEN_PINGS;
        this.pingsTilPurge = SRPC.PINGS_BEFORE_PURGE;
        this.nattempts     = SRPC.ATTEMPTS;
    }

    protected boolean isTimedOut() {
        return state == State.TIMEDOUT;
    }


    protected Endpoint getSource() {
        return source;
    }

    private void setState(State newState) {
        resetPings();
        this.state = newState;
        notify();
    }

    private void waitForState(EnumSet<State> set) {
        while (!set.contains(state)){
            try {
                this.wait();
            } catch (final InterruptedException e){
                throw new RuntimeException(e);
            }
        }
    }

    protected synchronized void connect(String serviceName) throws IOException{
        resetTicks();
        Payload payload = PayloadFactory.createConnect(source.getSubport(), seqNo, 1, 1, serviceName + '\0');
        send(payload);
        setState(State.CONNECT_SENT);
        this.waitForState(EnumSet.of(State.IDLE, State.TIMEDOUT));

        if (state == State.TIMEDOUT)
            throw new IOException("Connection request timed out");
    }

    /**
     * Calls query on a connected service
     * @param query the query to call
     * @return      a string containing the response
     * @throws IOException if failure occurs or if conncetion not IDLE
     */
    public synchronized String call(String query) throws IOException{
        int fragment, fragmentCount, qlen;
        if (state == State.IDLE) {
            if (seqNo >= SEQNO_LIMIT) {
                seqNo = SEQNO_START;
                send(Command.SEQNO);
                setState(State.SEQNO_SENT);
                waitForState(EnumSet.of(State.IDLE, State.TIMEDOUT));
                if (state == State.TIMEDOUT)
                    throw new IOException("Connection timed out");
            }
            seqNo ++;
            query = query + "\0";

            qlen = query.length();
            if (qlen > MAX_LENGTH)
                throw new IOException("query too long");
            fragmentCount =  (qlen - 1 )/ SRPC.FRAGMENT_SIZE + 1;

            Payload payload;
            for (fragment = 1; fragment < fragmentCount; fragment ++) {
                int index = SRPC.FRAGMENT_SIZE *(fragment -1);
                payload = PayloadFactory.createData(Command.FRAGMENT, source.getSubport(), seqNo, fragment,
                  fragmentCount, qlen, query.substring(index, index + SRPC.FRAGMENT_SIZE));
                lastFrag = (byte)fragment;
                send(payload);

                setState(State.FRAGMENT_SENT);
                waitForState(EnumSet.of(State.TIMEDOUT,State.FACK_RECEIVED));
                if (state == State.TIMEDOUT)
                    throw new IOException("Connection timed out");
            }
            int index = SRPC.FRAGMENT_SIZE *(fragment -1);
            payload = PayloadFactory.createData(Command.QUERY, source.getSubport(), seqNo, fragment, fragmentCount,
              qlen, query.substring(index));
            send(payload);
            resetTicks();
            setState(State.QUERY_SENT);
            waitForState(EnumSet.of(State.TIMEDOUT, State.IDLE));

        } else {
            throw new IOException("Connection not IDLE when call attempted");
        }
        return resp;

    }

    /**
     * sends a response to a previous query
     * used by servers
     * @param query          the response string
     * @throws IOException  if failure occurs
     */
    public synchronized void response(String query) throws IOException {
        int fragment, fragmentCount, qlen;
        if (state == State.QACK_SENT) {
            query = query + '\0';
            qlen = query.length();
            if (qlen > MAX_LENGTH)
                throw new IOException("query too long");
            fragmentCount =  ((qlen - 1 )/ SRPC.FRAGMENT_SIZE + 1);

            Payload payload;
            for (fragment = 1; fragment < fragmentCount; fragment ++) {
                int index = SRPC.FRAGMENT_SIZE *(fragment -1);
                payload = PayloadFactory.createData(Command.FRAGMENT, source.getSubport(), seqNo, fragment,
                  fragmentCount, qlen, query.substring(index, index + SRPC.FRAGMENT_SIZE));
                lastFrag = (byte)fragment;
                send(payload);

                setState(State.FRAGMENT_SENT);
                waitForState(EnumSet.of(State.TIMEDOUT,State.FACK_RECEIVED));
                if (state == State.TIMEDOUT)
                    throw new IOException("Connection timed out");
            }
            int index = SRPC.FRAGMENT_SIZE *(fragment -1);
            payload = PayloadFactory.createData(Command.RESPONSE, source.getSubport(), seqNo, fragment, fragmentCount,
              qlen,  query.substring(index));
            send(payload);
            resetTicks();
            setState(State.RESPONSE_SENT);
        }else {
            throw new IOException("Attempted to send response when query not acknowledged");
        }
        
    }

    /**
     * disconnects the connection
     * @throws IOException if failure occurs
     */
    public synchronized void disconnect() throws IOException {
        send(Command.DISCONNECT);
        waitForState(EnumSet.of(State.TIMEDOUT));

    }


    private void resetPings() {
        ticksTilPing  = SRPC.TICKS_BETWEEN_PINGS;
        pingsTilPurge = SRPC.PINGS_BEFORE_PURGE;
    }

    private void resetTicks() {
        ticks     = SRPC.TICKS;
        ticksLeft = SRPC.TICKS;
        nattempts = SRPC.ATTEMPTS;
    }


    protected synchronized void commandReceived(Payload payload) throws IOException {
        switch (payload.getCommand()) {
            case CONNECT :
                CONNECTReceived(payload);
                break;
            case CACK:
                CACKReceived(payload);
                break;
            case DISCONNECT:
                DISCONNECTReceived(payload);
                break;
            case DACK:
                DACKReceived(payload);
                break;
            case FRAGMENT:
                FRAGMENTReceived(payload);
                break;
            case FACK:
                FACKReceived(payload);
                break;
            case QUERY:
                QUERYReceived(payload);
                break;
            case QACK:
                QACKReceived(payload);
                break;
            case RESPONSE:
                RESPONSEReceived(payload);
                break;
            case RACK:
                RACKReceived(payload);
                break;
            case PING :
                send(Command.PACK);
                break;
            case PACK :
                resetPings();
                break;
            case SEQNO:
                SEQNOReceived(payload);
                break;
            case SACK:
                SACKReceived();
                break;
        }
    }

    private void SACKReceived() {
        if (state == State.SEQNO_SENT)
            setState(State.IDLE);
    }

    private void SEQNOReceived(Payload payload) throws IOException {
        if (state == State.IDLE || state == State.AWAITING_RESPONSE) {
            resetTicks();
            send(Command.SACK);
            seqNo = payload.getSeqNo();
            setState(State.IDLE);
        }
    }

    private void RACKReceived(Payload payload) {
        if (seqNo == payload.getSeqNo())
            setState(State.IDLE);
    }

    private void RESPONSEReceived(Payload payload) throws IOException {
        DataPayload dataPayload;
        dataPayload = (DataPayload) payload;
        if (payload.getSeqNo() != seqNo)
            return;
        if (state == State.QUERY_SENT || state == State.AWAITING_RESPONSE) {
            resp = dataPayload.getData();

        } else if (dataPayload.getSeqNo() == seqNo && state == State.FACK_SENT &&
                (payload.getFragment() - lastFrag) == 1 && payload.getFragmentCount() == payload.getFragment()) {
            resp = data + dataPayload.getData();
            data = "";
        } else {
            return;
        }
        resetTicks();
        send(Command.RACK, dataPayload.getFragment(), dataPayload.getFragmentCount());
        setState(State.IDLE);
    }

    private void QACKReceived(Payload payload) {
        if (payload.getSeqNo() == seqNo)
            setState(State.AWAITING_RESPONSE);
    }

    private void QUERYReceived(Payload payload) throws IOException {
        DataPayload dataPayload;
        dataPayload = (DataPayload) payload;
        String query;
        if ((payload.getSeqNo() - seqNo) == 1 && (state == State.IDLE || state == State.RESPONSE_SENT)) {
            seqNo = payload.getSeqNo();
            query = dataPayload.getData();
        } else if (dataPayload.getSeqNo() == seqNo && state == State.FACK_SENT &&
                (payload.getFragment() - lastFrag) == 1 && payload.getFragmentCount() == payload.getFragment()) {
            query = data + dataPayload.getData();
            data = "";

        } else if (dataPayload.getSeqNo() == seqNo && (state == State.QACK_SENT ||
               state == State.RESPONSE_SENT))  {
            retry();
            return;
        } else {
            return;
        }
        send(Command.QACK, payload.getFragment(), payload.getFragmentCount());
        setState(State.QACK_SENT);
        service.add(new Message(this, query));
    }

    private void FACKReceived(Payload payload) {
        if (payload.getSeqNo() == seqNo && state == State.FRAGMENT_SENT
                && payload.getFragment() == lastFrag) {
            setState(State.FACK_RECEIVED);
        }
    }

    private void FRAGMENTReceived(Payload payload) throws IOException {
        DataPayload dataPayload;
        dataPayload = (DataPayload) payload;
        boolean isQ = (state == State.IDLE || state == State.RESPONSE_SENT) &&
                (payload.getSeqNo() - this.seqNo) == 1 && payload.getFragment() == 1;
        boolean isR = (state == State.QUERY_SENT || state == State.AWAITING_RESPONSE) &&
                payload.getSeqNo() == this.seqNo && payload.getFragment() == 1;
        if (isQ || isR) {
            //NEW
            data = dataPayload.getData();
            seqNo = dataPayload.getSeqNo();
        } else if (dataPayload.getSeqNo() == seqNo && state == State.FACK_SENT &&
                (payload.getFragment() - lastFrag) == 1) {
            data += dataPayload.getData();
        } else if (dataPayload.getSeqNo() == seqNo && state == State.FACK_SENT &&
                payload.getFragment() == lastFrag) {
            // OLD
            retry();
            return;
        } else {
            return;
        }
        lastFrag = (byte)dataPayload.getFragment();
        send(Command.FACK, payload.getFragment(), payload.getFragmentCount());
        setState(State.FACK_SENT);
    }

    private void DACKReceived(Payload payload) {
        if (payload.getSeqNo() == seqNo) {
            setState(State.TIMEDOUT);
        }
    }

    private void DISCONNECTReceived(Payload payload) throws IOException {
        if (payload.getSeqNo() == seqNo) {
            send(Command.DACK);
            setState(State.TIMEDOUT);
        }
    }

    private void CACKReceived(Payload payload) {
        if (payload.getSeqNo() == seqNo)
            setState(State.IDLE);
    }

    private void CONNECTReceived(Payload payload) throws IOException {
        seqNo = payload.getSeqNo();
        send(Command.CACK);
        state = State.IDLE;
        resetTicks();
    }

    private void send(Command command) throws IOException {
        Payload control = PayloadFactory.create(command, source.getSubport(), seqNo, (byte)1,(byte)1);
        send(control);
    }

    private void send(Command command, int frag, int fragCount) throws IOException {
        Payload control = PayloadFactory.create(command, source.getSubport(), seqNo, frag,fragCount);
        send(control);
    }


    private void send(Payload payload) throws IOException{
        DatagramChannel channel = context.getChannel();
        channel.send(payload.toBuffer(), source.getAddress());
        lastPayload = payload;
    }
    
    private void retry() throws IOException {
        switch (state) {
            case CONNECT_SENT:
            case QUERY_SENT:
            case RESPONSE_SENT:
            case DISCONNECT_SENT:
            case FRAGMENT_SENT:
            case SEQNO_SENT:
                send(lastPayload);
                break;
        }
    }

    private void ping() throws IOException {
        send(Command.PING);
    }

    protected synchronized void checkStatus() throws IOException {
        // retry or time out connections which are have not responded in a
        // timely fashion
        if ((state == State.CONNECT_SENT) || (state == State.QUERY_SENT)
                   || (state == State.RESPONSE_SENT) || (state == State.DISCONNECT_SENT)
                   || (state == State.FRAGMENT_SENT) || (state == State.SEQNO_SENT)) {
            if (--ticksLeft <= 0) {
                if (--nattempts <= 0) {
                    state = State.TIMEDOUT;
                    //SRPC.logger.log(Level.SEVERE, "TimedOut");
                }
                else {
                    ticks *= 2;
                    ticksLeft = ticks;
                    //SRPC.logger.severe("retrying");
                    this.retry();
                }
            }

        } else {
            // periodically ping idle connections
            if (--ticksTilPing <= 0) {
                if (--pingsTilPurge <= 0) {
                    state = State.TIMEDOUT;
//                    SRPC.logger.info("No pings");
                } else {
                    ticksTilPing = SRPC.TICKS_BETWEEN_PINGS;
                    this.ping();
                }
            }
        }
    }


}
