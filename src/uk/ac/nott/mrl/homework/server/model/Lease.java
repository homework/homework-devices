package uk.ac.nott.mrl.homework.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class Lease
{
	public enum Action
	{
		add, del, old
	}

	public static Iterable<Lease> parseResultSet(final String results)
	{
		final Collection<Lease> leases = new ArrayList<Lease>();

		// System.out.println(results);

		final String[] lines = results.split("\n");
		for (int index = 2; index < lines.length; index++)
		{
			try
			{
				final String[] columns = lines[index].split("<\\|>");
				final Lease lease = new Lease();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				lease.timeStamp = new Date(timeLong / 1000000);
				lease.action = Action.valueOf(columns[1]);
				lease.macAddress = columns[2];
				lease.ipAddress = columns[3];
				lease.hostName = columns[4];
				leases.add(lease);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		return leases;
	}

	private Date timeStamp;
	private Action action;
	private String macAddress;
	private String ipAddress;
	private String hostName;

	public void clearIPAddress()
	{
		ipAddress = null;
	}

	public Action getAction()
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

	public Date getTimestamp()
	{
		return timeStamp;
	}
}