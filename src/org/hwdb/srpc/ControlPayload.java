package org.hwdb.srpc;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.EnumSet;

/**
* @author Magnus Morton
*/
public class ControlPayload extends Payload {

    protected ControlPayload(ByteBuffer buffer) throws IOException{
        subport = buffer.getInt();
        seqNo   = buffer.getInt();
        int commandNo = buffer.getShort();

        for (final Command command : EnumSet.allOf(Command.class)) {
            if (command.ordinal() == commandNo) {
               this.command = command;
            }
        }


        fragment = buffer.get();
        fragmentCount = buffer.get();
    }

    protected ControlPayload(Command command, int subport, int seqNo, int fragment, int fragmentCount) {
        this.command       = command;
        this.subport       = subport;
        this.seqNo         = seqNo;
        this.fragment      = fragment;
        this.fragmentCount = fragmentCount;

    }


}

