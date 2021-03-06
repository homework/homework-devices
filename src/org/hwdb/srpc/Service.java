package org.hwdb.srpc;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Magnus Morton
 */
public class Service
{

	private String name;
	private Queue<Message> messageQueue;

	protected Service(final String serviceName)
	{
		name = serviceName;
		messageQueue = new LinkedList<Message>();
	}

	protected synchronized void add(final Message message)
	{
		messageQueue.add(message);
		notify();
	}

	/**
	 * Returns the service name
	 * 
	 * @return the name of the Service
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gets queries received by this Service Equivalent to rpc_query() C API function
	 * 
	 * @return the the first query in the received queue null otherwise?
	 */
	public synchronized Message query()
	{
		Message out = null;
		if (messageQueue.isEmpty())
		{
			try
			{
				wait();
			}
			catch (final InterruptedException e)
			{
				return null;
			}
		}
		final Message msg = messageQueue.poll();
		if (msg != null)
		{
			out = msg;
		}
		return out;
	}
}
