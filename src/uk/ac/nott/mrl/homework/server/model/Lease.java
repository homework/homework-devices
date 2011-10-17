package uk.ac.nott.mrl.homework.server.model;

import java.util.Date;

public class Lease
{
	// private static final Logger logger = Logger.getLogger(Lease.class.getName());

	public static void parseResultSet(final String results, final Model model)
	{
		final ResultSet result = new ResultSet(results);
		if(result.getSize() != 0)
		{
			System.out.println("New leases: " + result.getSize());
		}
		for(String[] columns: result)
		{			
			try
			{
				final Lease lease = new Lease();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				lease.timestamp = new Date(timeLong / 1000000).getTime();
				lease.action = columns[4].toLowerCase();
				lease.macAddress = columns[1].toLowerCase();
				lease.ipAddress = columns[2];
				lease.hostName = columns[3];
				if(lease.ipAddress.toLowerCase().equals("null"))
				{
					lease.ipAddress = null;
				}				
				if (lease.hostName.toLowerCase().equals("null"))
				{
					lease.hostName = null;
				}

				model.add(lease);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private String action;
	private String hostName;
	private String ipAddress;
	private String macAddress;
	private long timestamp;

	public String getAction()
	{
		return action;
	}

	public String getHostName()
	{
		return hostName;
	}

	public String getIpAddress()
	{
		return ipAddress;
	}

	public String getMacAddress()
	{
		return macAddress;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void update(final Lease lease)
	{
		if (lease.getIpAddress() != null)
		{
			ipAddress = lease.getIpAddress();
		}
		action = lease.getAction();
		timestamp = lease.getTimestamp();
	}
}