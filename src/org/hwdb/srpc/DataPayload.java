package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

/**
 * @author Magnus Morton
 */
public class DataPayload extends ControlPayload
{
	private int dataLength;
	private int fragLength;
	private String data;

	protected DataPayload(final ByteBuffer buffer) throws IOException
	{
		super(buffer);
		dataLength = buffer.getShort();
		fragLength = buffer.getShort();
		data = CharBuffer.wrap(SRPC.decoder.decode(buffer)).toString().trim();
	}

	protected DataPayload(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount)
	{
		super(command, subport, seqNo, fragment, fragmentCount);
	}

	protected DataPayload(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount, final int qlen, final String data)
	{
		super(command, subport, seqNo, fragment, fragmentCount);
		this.dataLength = qlen;
		this.fragLength = data.length();
		this.data = data;
	}

	protected String getData()
	{
		return data;
	}

	protected int getDataLength()
	{
		return dataLength;
	}

	protected int getFragLength()
	{
		return fragLength;
	}

	public void setData(final String data)
	{
		this.data = data;
	}

	protected void setDataLength(final short dataLength)
	{
		this.dataLength = dataLength;
	}

	protected void setFragLength(final short fragLength)
	{
		this.fragLength = fragLength;
	}

	@Override
	protected ByteBuffer toBuffer()
	{
		final ByteBuffer header = super.toBuffer();
		final ByteBuffer out = ByteBuffer.allocate(COMMAND_SIZE + data.length() + 4);
		out.put(header);

		// if there are problems with fragmentation that I can't figure out, try switching these
		out.putShort((short) dataLength);
		out.putShort((short) fragLength);
		try
		{
			out.put(SRPC.encoder.encode(CharBuffer.wrap(data)));
		}
		catch (final CharacterCodingException e)
		{
			throw new RuntimeException(e);
		}
		out.rewind();
		return out;
	}
}
