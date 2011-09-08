package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

/**
 * @author Magnus Morton
 */
public class ConnectPayload extends ControlPayload
{

	private String service;

	protected ConnectPayload(final ByteBuffer buffer) throws IOException
	{
		super(buffer);
		service = CharBuffer.wrap(SRPC.decoder.decode(buffer)).toString().trim();
	}

	protected ConnectPayload(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount)
	{
		super(command, subport, seqNo, fragment, fragmentCount);
	}

	protected ConnectPayload(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount, final String service)
	{
		super(command, subport, seqNo, fragment, fragmentCount);
		this.service = service;
	}

	protected String getService()
	{
		return service;
	}

	@Override
	protected ByteBuffer toBuffer()
	{
		final ByteBuffer control = super.toBuffer();
		final ByteBuffer out = ByteBuffer.allocate(COMMAND_SIZE + service.length());
		out.put(control);
		try
		{
			out.put(SRPC.encoder.encode(CharBuffer.wrap(service)));
		}
		catch (final CharacterCodingException e)
		{
			throw new RuntimeException(e);
		}
		out.rewind();
		return out;
	}
}
