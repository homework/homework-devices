package org.hwdb.srpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

public class EndpointTest
{
	InetAddress addr;
	Endpoint ep;
	Endpoint ep2;
	Endpoint ep3;

	@Test
	public void hashCodesShouldBeDifferentIfObjectsNotEqual()
	{
		assertFalse(ep.hashCode() == ep3.hashCode());
	}

	@Before
	public void setUp() throws UnknownHostException
	{
		final byte[] localhost = { 127, 0, 0, 1 };
		addr = InetAddress.getByAddress(localhost);
		ep = new Endpoint(addr, 20000, 12344);
		ep2 = new Endpoint(addr, 20000, 12344);
		ep3 = new Endpoint(addr, 200, 1232);
	}

	@Test
	public void testEquality()
	{
		assertTrue(ep.equals(ep2));
	}

	@Test
	public void testHashCode()
	{
		assertTrue((addr.hashCode() ^ 20000 ^ 12344) == ep.hashCode());
	}

	@Test
	public void testHashCodesShouldBeEqualIfObjectEqual()
	{
		assertTrue(ep.hashCode() == ep2.hashCode());
	}

	@Test
	public void testNotEqual()
	{
		assertFalse(ep.equals(ep3));
	}
}
