package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

/**
* @author Magnus Morton
*/
public class DataPayload extends ControlPayload {
    private int dataLength;
    private int fragLength;
    private String      data;

    protected DataPayload(ByteBuffer buffer) throws IOException{
        super(buffer);
        dataLength = buffer.getShort();
        fragLength = buffer.getShort();
        data = CharBuffer.wrap(SRPC.decoder.decode(buffer)).toString().trim();
    }

    protected DataPayload(Command command, int subport, int seqNo, int fragment, int fragmentCount, int qlen,
                          String data){
        super(command, subport,seqNo, fragment, fragmentCount);
        this.dataLength = qlen;
        this.fragLength = data.length();
        this.data       = data;
    }

    protected DataPayload(Command command, int subport, int seqNo, int fragment, int fragmentCount) {
        super(command, subport, seqNo, fragment, fragmentCount);
    }

    @Override
	protected ByteBuffer toBuffer () {
        ByteBuffer header = super.toBuffer();
        ByteBuffer out = ByteBuffer.allocate(COMMAND_SIZE + data.length() + 4);
        out.put(header);

        // if there are problems with fragmentation that I can't figure out, try switching these
        out.putShort((short)dataLength);
        out.putShort((short)fragLength);
        try {
            out.put(SRPC.encoder.encode(CharBuffer.wrap(data)));
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
        out.rewind();
        return out;
    }

    protected int getDataLength() {
        return dataLength;
    }

    protected int getFragLength() {
        return fragLength;
    }

    protected String getData() {
        return data;
    }

    protected void setDataLength(short dataLength) {
        this.dataLength = dataLength;
    }

    protected void setFragLength(short fragLength) {
        this.fragLength = fragLength;
    }

    public void setData(String data) {
        this.data = data;
    }
}
