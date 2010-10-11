package uk.ac.nott.mrl.homework.server;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.nott.mrl.homework.server.model.Lease;
import uk.ac.nott.mrl.homework.server.model.Lease.Action;
import uk.ac.nott.mrl.homework.server.model.Link;

public class PollingThread extends Thread
{
	private final Map<String, Lease> leases = new HashMap<String, Lease>();

	private static final Logger logger = Logger.getLogger(PollingThread.class.getName());

	private final int TIME_DELTA = 5000;
	private final JavaSRPC rpc = new JavaSRPC();
	private final boolean nox = true;

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				if (!rpc.isConnected())
				{
					try
					{
						rpc.connect(InetAddress.getByName("192.168.9.1"), 987);
					}
					catch (final Exception e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}

				while (rpc.isConnected())
				{
					try
					{
						updateLinks();
						updateLeases();
						if (nox)
						{
							updatePermitted();
						}
						ListLinks.last = new Date();
					}
					catch (final Exception e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}

					try
					{
						Thread.sleep(TIME_DELTA);
					}
					catch (final Exception e)
					{
					}
				}

				try
				{
					Thread.sleep(5000);
				}
				catch (final Exception e)
				{
				}
			}
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void updateLeases() throws Exception
	{
		String leaseQuery;
		if (ListLinks.last != null)
		{
			final String s = String.format("@%016x@", ListLinks.last.getTime() * 1000000);
			leaseQuery = String.format("SQL:select * from Leases [ since %s ]", s);
		}
		else
		{
			leaseQuery = String.format("SQL:select * from Leases");
		}
		final String leaseResults = rpc.call(leaseQuery);
		if (leaseResults != null)
		{
			final Iterable<Lease> newLeases = Lease.parseResultSet(leaseResults);
			for (final Lease lease : newLeases)
			{
				final Link existingLink = ListLinks.getLink(lease.getMacAddress());
				if (existingLink != null)
				{
					existingLink.update(lease);
				}

				if (lease.getAction() == Action.del)
				{
					final Lease oldLease = leases.get(lease.getMacAddress());
					if (oldLease != null)
					{
						oldLease.clearIPAddress();
					}
					// leases.remove(lease.getMacAddress());
				}
				else
				{
					leases.put(lease.getMacAddress(), lease);
				}
			}
		}
	}

	private void updateLinks() throws Exception
	{
		String linkQuery;
		if (ListLinks.last != null)
		{
			final String s = String.format("@%016x@", ListLinks.last.getTime() * 1000000);
			linkQuery = String.format("SQL:select * from Links [ since %s ]", s);
		}
		else
		{
			linkQuery = String.format("SQL:select * from Links");
		}

		final String linkResults = rpc.call(linkQuery);
		// logger.info(linkResults);

		if (linkResults != null)
		{
			final Iterable<Link> newLinks = Link.parseResultSet(linkResults);
			for (final Link link : newLinks)
			{
				final Link existingLink = ListLinks.getLink(link.getMacAddress());
				if (existingLink != null)
				{
					existingLink.update(link);
				}
				else
				{
					ListLinks.addLink(link);
					final Lease lease = leases.get(link.getMacAddress());
					if (lease != null)
					{
						link.update(lease);
					}
				}
			}
		}
	}

	private void updatePermitted()
	{
		try
		{
			System.out.println("Get Permitted");
			final URL url = new URL("http://192.168.9.1/ws.v1/homework/status");
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);

			ListLinks.updatePermitted(conn.getInputStream(), ListLinks.last.getTime());
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}