package org.hwdb.srpc;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import org.junit.Before;
import org.junit.Test;

public class PayloadFactoryTest
{
	private ByteBuffer control;

	@Before
	public void setUp()
	{
		control = ByteBuffer.allocate(100);
	}

	@Test
	public void testConnectPayloadInstantiated() throws Exception
	{
		control.putShort(8, (short) 1);
		System.out.println(control.remaining());

		assertThat(PayloadFactory.create(control), instanceOf(ConnectPayload.class));

	}

	@Test
	public void testControlPayloadInstantiated() throws Exception
	{
		control.putShort(8, (short) 2);
		System.out.println(control.remaining());

		assertThat(PayloadFactory.create(control), instanceOf(ControlPayload.class));
		control.rewind();
		control.putShort(8, (short) 14);
		assertThat(PayloadFactory.create(control), instanceOf(ControlPayload.class));

	}

	@Test
	public void testDataPayloadInstantiated() throws Exception
	{
		control.putShort(8, (short) 3);
		System.out.println(control.remaining());

		assertThat(PayloadFactory.create(control), instanceOf(DataPayload.class));
		control.rewind();

		control.putShort(8, (short) 5);
		assertThat(PayloadFactory.create(control), instanceOf(DataPayload.class));

	}

	@Test
	public void testToBufferProducesCorrectBuffer()
	{
		final Payload controlPayload = PayloadFactory.create(Command.CACK, 1234, 1234, (byte) 1, (byte) 1);
		final ByteBuffer expected = ByteBuffer.allocate(Payload.COMMAND_SIZE);
		expected.putInt(1234).putInt(1234).putShort((short) 2).put((byte) 1).put((byte) 1);
		final ByteBuffer test = controlPayload.toBuffer();

		for (int i = 0; i < Payload.COMMAND_SIZE; i++)
		{
			assertEquals(expected.get(i), test.get(i));
		}
	}

	@Test
	public void testToBufferProducesCorrectConnectBuffer() throws CharacterCodingException
	{
		final int length = Payload.COMMAND_SIZE + "Test\0".length();
		final Payload connectPayload = PayloadFactory.createConnect(1234, 1234, (byte) 1, (byte) 1, "Test\0");
		final ByteBuffer expected = ByteBuffer.allocate(length);
		expected.putInt(1234).putInt(1234).putShort((short) 1).put((byte) 1).put((byte) 1)
				.put(SRPC.encoder.encode(CharBuffer.wrap("Test\0")));
		expected.rewind();
		final ByteBuffer test = connectPayload.toBuffer();

		for (int i = 0; i < length; i++)
		{
			assertEquals(expected.get(i), test.get(i));
		}
	}

	@Test
	public void testToBufferProducesCorrectDataBuffer() throws CharacterCodingException
	{

		final String data = "Test";
		final int length = Payload.COMMAND_SIZE + data.length() + 4;
		final short qlen = (short) (data.length());
		final Payload dataPayload = PayloadFactory
				.createData(Command.QUERY, 1234, 1234, (byte) 1, (byte) 1, qlen, data);
		final ByteBuffer expected = ByteBuffer.allocate(length);
		expected.putInt(1234).putInt(1234).putShort((short) 3).put((byte) 1).put((byte) 1).putShort(qlen)
				.putShort(qlen).put(SRPC.encoder.encode(CharBuffer.wrap("Test")));
		expected.rewind();
		final ByteBuffer test = dataPayload.toBuffer();

		for (int i = 0; i < length; i++)
		{
			assertEquals(expected.get(i), test.get(i));
		}
	}
}