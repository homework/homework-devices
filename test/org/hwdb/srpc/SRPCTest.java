package org.hwdb.srpc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class SRPCTest
{

	private SRPC test;

	@Before
	public void setUp() throws IOException
	{
		test = new SRPC();
	}

	@Test
	public void testDetails() throws Exception
	{

	}

	@Test
	public void testConnect() throws Exception
	{

	}

	@Test
	public void testOffer() throws Exception
	{
		String name = "name";
		Service service = test.offer(name);

		assertEquals(name, service.getName());
		assertEquals(service, test.lookupService(name));
	}

	/**
	 * check that timedout records are detected
	 **/
	@Test
	public void testScanTO()
	{
		// ArrayList<Connection> timedOut = new ArrayList<Connection>();
	}

}
