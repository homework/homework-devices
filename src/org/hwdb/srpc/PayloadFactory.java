package org.hwdb.srpc;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Magnus Morton
 */
// Simple factory idiom
public class PayloadFactory
{

	protected static Payload create(final ByteBuffer buffer) throws IOException
	{
		final int commandNo = buffer.getShort(8);
		switch (commandNo)
		{
			case 1:
				return new ConnectPayload(buffer);
			case 2:
			case 4:
			case 6:
			case 7:
			case 8:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				return new ControlPayload(buffer);
			case 3:
			case 5:
			case 9:
				return new DataPayload(buffer);
			default:
				return null;
		}

	}

	protected static Payload create(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount)
	{
		switch (command.ordinal())
		{
			case 1:
				return new ConnectPayload(command, subport, seqNo, fragment, fragmentCount);
			case 2:
			case 4:
			case 6:
			case 7:
			case 8:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				return new ControlPayload(command, subport, seqNo, fragment, fragmentCount);
			case 3:
			case 5:
			case 9:
				return new DataPayload(command, subport, seqNo, fragment, fragmentCount);
			default:
				return null;
		}

	}

	protected static Payload createConnect(final int subport, final int seqNo, final int fragment,
			final int fragmentCount, final String service)
	{
		return new ConnectPayload(Command.CONNECT, subport, seqNo, fragment, fragmentCount, service);
	}

	protected static Payload createData(final Command command, final int subport, final int seqNo, final int fragment,
			final int fragmentCount, final int qlen, final String data)
	{
		return new DataPayload(command, subport, seqNo, fragment, fragmentCount, qlen, data);
	}
}
