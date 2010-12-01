package uk.ac.nott.mrl.homework.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.nott.mrl.homework.server.ListLinks;
import uk.ac.nott.mrl.homework.server.model.Lease.Action;

public class Link
{
	private static final Companies companies = new Companies();
	
	private static final Logger logger = Logger.getLogger(Link.class.getName());
	
	public static Link parseLink(final String logLine)
	{
		final Link link = new Link();
		final int start = logLine.indexOf('@');
		final int end = logLine.indexOf('@', start + 1);
		final String time = logLine.substring(start + 1, end);
		final long timeLong = Long.parseLong(time, 16);
		link.timeStamp = timeLong / 1000000;
		final StringTokenizer tokenizer = new StringTokenizer(logLine.substring(end + 1).trim(), ";");
		link.macAddress = tokenizer.nextToken();
		while (link.macAddress.length() < 12)
		{
			link.macAddress = "0" + link.macAddress;
		}
		link.rssi = Float.parseFloat(tokenizer.nextToken());
		link.retryCount = Integer.parseInt(tokenizer.nextToken());
		link.packetCount = Integer.parseInt(tokenizer.nextToken());
		link.byteCount = Integer.parseInt(tokenizer.nextToken());

		if (ListLinks.routerMacAddresses.contains(link.macAddress))
		{
			link.resource = true;
			link.deviceName = "Router";
		}

		return link;
	}

	public static Iterable<Link> parseResultSet(final String results)
	{
		final Collection<Link> links = new ArrayList<Link>();

		final String[] lines = results.split("\n");
		for (int index = 2; index < lines.length; index++)
		{
			try
			{
				final String[] columns = lines[index].split("<\\|>");
				final Link link = new Link();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				link.timeStamp = timeLong / 1000000;
				link.macAddress = columns[1];
				link.rssi = Float.parseFloat(columns[2]);
				link.retryCount = Integer.parseInt(columns[3]);
				link.packetCount = Integer.parseInt(columns[4]);
				link.byteCount = Integer.parseInt(columns[5]);

				if (ListLinks.routerMacAddresses.contains(link.macAddress))
				{
					link.resource = true;
					link.deviceName = "Router";
				}

				links.add(link);
			}
			catch (final Exception e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return links;
	}

	private long timeStamp;
	private String macAddress;
	private String corporation;
	private float rssi;
	private int retryCount;
	private int packetCount;
	private int byteCount;
	private String ipAddress;
	private String deviceName;
	private boolean permitted = false;
	private boolean resource = false;
	private transient Action nameAction; 
	
	public Link()
	{

	}
	
	public String getDeviceName()
	{
		return deviceName;
	}
	
	public String getIPAddress()
	{
		return ipAddress;
	}

	public int getByteCount()
	{
		return byteCount;
	}

	public String getCorporation()
	{
		return corporation;
	}

	public String getMacAddress()
	{
		return macAddress;
	}

	public void initCorporation()
	{
		if (corporation == null)
		{
			corporation = companies.getCompany(getMacAddress());
		}
	}
	
	public int getPacketCount()
	{
		return packetCount;
	}

	public int getRetryCount()
	{
		return retryCount;
	}

	public float getRssi()
	{
		return rssi;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public boolean isResource()
	{
		return resource;
	}

	public void setPermitted(final boolean b, final long since)
	{
		if (permitted != b)
		{
			permitted = b;
			timeStamp = since;
		}
	}

	public void setResource(final boolean resource)
	{
		this.resource = resource;
	}

	public void setDeviceName(final String name, final double since)
	{
		deviceName = name;
	}

	@Override
	public String toString()
	{
		return timeStamp + ": " + macAddress;
	}

	public void update(final Lease lease)
	{
		if (lease.getAction() == Action.del)
		{
			ipAddress = null;
		}
		else
		{
			if(nameAction != Action.upd || lease.getAction() == Action.upd)
			{
				deviceName = lease.getHostName();
				nameAction = lease.getAction();
			}
				
			if(lease.getAction() != Action.upd)
			{
				ipAddress = lease.getIpAddress();
			}
		}
		timeStamp = Math.max(lease.getTimestamp(), timeStamp);
	}

	public void update(final Link link)
	{
		timeStamp = Math.max(link.getTimeStamp(), timeStamp);		
		macAddress = link.macAddress;
		packetCount = link.packetCount;
		retryCount = link.retryCount;
		byteCount = link.byteCount;
		rssi = link.rssi;
	}
}