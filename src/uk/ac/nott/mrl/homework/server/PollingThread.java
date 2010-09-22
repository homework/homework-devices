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

	private final int TIME_DELTA = 3000;
	private final JavaSRPC rpc = new JavaSRPC();

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
						// updatePermitted();
						LinkServlet.last = new Date();
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
		final String leaseResults = rpc.call("SQL:select * from Leases");
		if (leaseResults != null)
		{
			final Iterable<Lease> newLeases = Lease.parseResultSet(leaseResults);
			for (final Lease lease : newLeases)
			{
				final Link existingLink = LinkServlet.getLink(lease.getMacAddress());
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
		if (LinkServlet.last != null)
		{
			final String s = String.format("@%016x@", LinkServlet.last.getTime() * 1000000);
			linkQuery = String.format(	"SQL:select * from Links [ range %d seconds ] where timestamp > %s",
										TIME_DELTA + 1000, s);
		}
		else
		{
			linkQuery = String.format("SQL:select * from Links [ range %d seconds ]", TIME_DELTA + 1000);
		}

		final String linkResults = rpc.call(linkQuery);
		// logger.info(linkResults);

		if (linkResults != null)
		{
			final Iterable<Link> newLinks = Link.parseResultSet(linkResults);
			for (final Link link : newLinks)
			{
				final Link existingLink = LinkServlet.getLink(link.getMacAddress());
				if (existingLink != null)
				{
					existingLink.update(link);
				}
				else
				{
					LinkServlet.addLink(link);
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

			LinkServlet.updatePermitted(conn.getInputStream(), LinkServlet.last.getTime());
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}