package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

/**
* @author Magnus Morton
*/
public class ConnectPayload extends ControlPayload {

    private String service;

    protected ConnectPayload(ByteBuffer buffer) throws IOException {
        super(buffer);
        service = CharBuffer.wrap(SRPC.decoder.decode(buffer)).toString().trim();
    }

    protected ConnectPayload(Command command, int subport, int seqNo, int fragment, int fragmentCount, String service) {
        super(command, subport, seqNo, fragment, fragmentCount);
        this.service = service;
    }

    protected String getService() {
        return service;
    }

    @Override
	protected ByteBuffer toBuffer()  {
        ByteBuffer control = super.toBuffer();
        ByteBuffer out = ByteBuffer.allocate(COMMAND_SIZE + service.length() );
        out.put(control);
        try {
            out.put(SRPC.encoder.encode(CharBuffer.wrap(service)));
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
        out.rewind();
        return out;
    }

    protected ConnectPayload(Command command, int subport, int seqNo, int fragment, int fragmentCount) {
        super(command, subport, seqNo, fragment, fragmentCount);
    }
}
