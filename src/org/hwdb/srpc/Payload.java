package org.hwdb.srpc;


import java.nio.ByteBuffer;

/**
* @author Magnus Morton
*/
public abstract class Payload {
    protected Command   command;
    protected int       subport;
    protected int       seqNo;
    protected int      fragment;
    protected int      fragmentCount;

    static final int COMMAND_SIZE = 12;

    protected Command getCommand () {
        return command;
    }

    protected int getSubport() {
        return subport;
    }

    protected int getSeqNo() {
        return seqNo;
    }

    protected int getFragment() {
        return fragment;
    }

    protected int getFragmentCount() {
        return fragmentCount;
    }


    @Override
	public String toString(){
       return String.format("Payload - Subport: %d  SeqNo: %d Command: %s Fragment: %d FragCount: %d", subport, seqNo,
         command, fragment, fragmentCount);
    }

    protected ByteBuffer toBuffer() {
        ByteBuffer out = ByteBuffer.allocate(COMMAND_SIZE);
        out.putInt(subport)
            .putInt(seqNo)
            .putShort((short) command.ordinal())
            .put((byte)fragment)
            .put((byte)fragmentCount);
        out.rewind();
        return out;
    }

}
