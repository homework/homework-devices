package uk.ac.nott.mrl.homework.server;

import java.net.ConnectException;
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
	public static final String hwdbHost = "localhost";
	// public static final String hwdbHost = "192.168.9.1";

	public static boolean trayPlugged = false;

	private static final Logger logger = Logger.getLogger(PollingThread.class.getName());

	private static final String searchString = "<|>Artifact App<|>USB<|>";

	private final Map<String, Lease> leases = new HashMap<String, Lease>();
	private final boolean nox = true;
	private final JavaSRPC rpc = new JavaSRPC();

	private final int TIME_DELTA = 5000;

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
						rpc.connect(InetAddress.getByName(hwdbHost), 987);
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
						updateUserEvent();
						if (nox)
						{
							updatePermitted();
						}
						ListLinks.last = new Date().getTime();
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
		if (ListLinks.last > 0)
		{
			final String s = String.format("@%016x@", ListLinks.last * 1000000);
			leaseQuery = String.format("SQL:select * from Leases [ since %s ]", s);
		}
		else
		{
			leaseQuery = String.format("SQL:select * from Leases");
		}
		final String leaseResults = rpc.call(leaseQuery);
		if(!leaseResults.startsWith("0<|>Success"))
		{
			logger.info(leaseResults);
		}
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
		if (ListLinks.last > 0)
		{
			final String s = String.format("@%016x@", ListLinks.last * 1000000);
			linkQuery = String.format("SQL:select * from Links [ since %s ]", s);
		}
		else
		{
			linkQuery = String.format("SQL:select * from Links");
		}
		final String linkResults = rpc.call(linkQuery);
		if(!linkResults.startsWith("0<|>Success"))
		{
			logger.info(linkResults);
		}		

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
			final URL url = new URL("http://" + hwdbHost + "/ws.v1/homework/status");
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);

			ListLinks.updatePermitted(conn.getInputStream(), ListLinks.last);
		}
		catch (final ConnectException e)
		{
			logger.warning("Failed to connect to nox service. " + e.getMessage());
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void updateUserEvent() throws Exception
	{
		String userQuery;
		if (ListLinks.last > 0)
		{
			final String s = String.format("@%016x@", ListLinks.last * 1000000);
			userQuery = String.format("SQL:select * from UserEvents [ since %s ]", s);
		}
		else
		{
			userQuery = String.format("SQL:select * from UserEvents");
		}
		final String result = rpc.call(userQuery);
		if(!result.startsWith("0<|>Success"))
		{
			logger.info(result);
		}	
		int index = result.lastIndexOf(searchString);
		if (index == -1) { return; }
		index = result.lastIndexOf(searchString) + searchString.length();
		if (result.substring(index).startsWith("Plugged"))
		{
			trayPlugged = true;
		}
		else if (result.substring(index).startsWith("Unplugged"))
		{
			trayPlugged = false;
		}
	}
}