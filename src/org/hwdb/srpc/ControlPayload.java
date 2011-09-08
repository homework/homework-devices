package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * @author Magnus Morton
 */
public class ControlPayload extends Payload
{

	protected ControlPayload(final ByteBuffer buffer) throws IOException
	{
		subport = buffer.getInt();
		seqNo = buffer.getInt();
		final int commandNo = buffer.getShort();

		for (final Command command : EnumSet.allOf(Command.class))
		{
			if (command.ordinal() == commandNo)
			{
				this.command = command;
			}
		}

		fragment = buffer.get();
		fragmentCount = buffer.get();
	}

	protected ControlPayload(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount)
	{
		this.command = command;
		this.subport = subport;
		this.seqNo = seqNo;
		this.fragment = fragment;
		this.fragmentCount = fragmentCount;

	}

}
