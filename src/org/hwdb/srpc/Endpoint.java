package org.hwdb.srpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Magnus Morton
 */
public class Endpoint
{

	private InetAddress address;
	private int port;
	private int subport;

	protected Endpoint(final InetAddress address, final int port, final int subport)
	{

		/**
		 * Describe subport here.
		 */
		this.address = address;
		this.port = port;
		this.subport = subport;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Endpoint)) { return false; }
		final Endpoint ep = (Endpoint) other;
		return address.equals(ep.address) && port == ep.port && subport == ep.subport;
	}

	protected SocketAddress getAddress()
	{
		return new InetSocketAddress(address, port);
	}

	/**
	 * Get the <code>Subport</code> value.
	 * 
	 * @return an <code>int</code> value
	 */
	public int getSubport()
	{
		return subport;
	}

	@Override
	public int hashCode()
	{
		return address.hashCode() ^ port ^ subport;
	}

}
