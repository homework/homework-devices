package uk.ac.nott.mrl.homework.server.model;

import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Device
{
	private static final Companies companies = new Companies();

	private static final Logger logger = Logger.getLogger(Device.class.getName());

	public static Device parseLink(final String logLine)
	{
		final Device link = new Device();
		final int start = logLine.indexOf('@');
		final int end = logLine.indexOf('@', start + 1);
		final String time = logLine.substring(start + 1, end);
		final long timeLong = Long.parseLong(time, 16);
		link.timestamp = timeLong / 1000000;
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

		return link;
	}

	public static void parseResultSet(final String results, final Model model)
	{
		final String[] lines = results.split("\n");
		if (!lines[0].endsWith("<|>0<|>0<|>"))
		{
			System.out.println("Links: " + lines[0]);
		}

		for (int index = 2; index < lines.length; index++)
		{
			try
			{
				final String[] columns = lines[index].split("<\\|>");
				final Device link = new Device();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				link.timestamp = timeLong / 1000000;
				link.macAddress = columns[1];
				link.rssi = Float.parseFloat(columns[2]);
				link.retryCount = Integer.parseInt(columns[3]);
				link.packetCount = Integer.parseInt(columns[4]);
				link.byteCount = Integer.parseInt(columns[5]);

				model.add(link);
			}
			catch (final Exception e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private int byteCount;
	private String corporation;
	private String deviceName;
	private String ipAddress;
	private String macAddress;
	private int packetCount;
	private int retryCount;

	private float rssi;

	private State state = State.unlisted;
	private long timestamp;

	public Device()
	{

	}

	public Device(final String name, final String mac, final String ip, final float rssi, final int byteCount)
	{
		this.deviceName = name;
		timestamp = new Date().getTime();
		this.rssi = rssi;
		this.ipAddress = ip;
		this.macAddress = mac;
		this.byteCount = byteCount;
	}

	public boolean canBeGrouped()
	{
		if (deviceName != null || ipAddress != null || state != State.unlisted) { return false; }
		return true;
	}

	public int getByteCount()
	{
		return byteCount;
	}

	public String getCompany()
	{
		return corporation;
	}

	public String getDeviceName()
	{
		return deviceName;
	}

	private String getGroupID()
	{
		return state + ":" + getShortCompanyName().toLowerCase();
	}

	public String getID()
	{
		if (canBeGrouped())
		{
			return getGroupID();
		}
		else
		{
			return macAddress;
		}
	}

	public String getIPAddress()
	{
		return ipAddress;
	}

	public String getMacAddress()
	{
		return macAddress;
	}

	public String getName()
	{
		if (deviceName != null) { return deviceName; }
		return getShortCompanyName() + " Device";
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

	public String getShortCompanyName()
	{
		String company = getCompany();
		if (company != null)
		{
			int cut = company.indexOf(' ');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}

			cut = company.indexOf(',');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}
			return company;
		}
		return "Unknown";
	}

	public State getState()
	{
		return state;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void initCorporation()
	{
		if (corporation == null)
		{
			corporation = companies.getCompany(getMacAddress());
		}
	}

	public void setDeviceName(final String name, final double since)
	{
		deviceName = name;
	}

	public void setState(final State state, final long since)
	{
		if (this.state != state)
		{
			// String oldID = getID();
			// State oldState = this.state;
			this.state = state;
			timestamp = since;
			// System.out.println("Set State of " + getName() + " from " + oldState + " to " + state
			// + ": " + oldID + "->" + getID());
		}
	}

	@Override
	public String toString()
	{
		return timestamp + ": " + macAddress;
	}

	public void update(final Device link)
	{
		timestamp = Math.max(link.getTimestamp(), timestamp);
		macAddress = link.macAddress;
		packetCount = link.packetCount;
		retryCount = link.retryCount;
		byteCount = link.byteCount;
		rssi = link.rssi;
	}

	public void update(final Lease lease)
	{
		ipAddress = lease.getIpAddress();
		deviceName = lease.getHostName();
		timestamp = Math.max(lease.getTimestamp(), timestamp);
	}
}