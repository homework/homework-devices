package uk.ac.nott.mrl.homework.server.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import uk.ac.nott.mrl.homework.server.model.Lease.Action;

public class Link
{
	// private static final String ROUTERMAC = "00:25:d3:72:b5:26";
	private static final String ROUTERMAC = "00:25:d3:72:b5:1e";

	public static final Map<String, String> macCorporations = new HashMap<String, String>();

	private static final String ieeeURL = "http://standards.ieee.org/cgi-bin/ouisearch?";

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

		if (link.macAddress.equals(ROUTERMAC))
		{
			link.resource = true;
			link.userName = "Router";
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

				if (link.macAddress.equals(ROUTERMAC))
				{
					link.resource = true;
					link.userName = "Router";
				}

				links.add(link);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		return links;
	}

	private long lastTried = 0;

	private double timeStamp;
	private String macAddress;
	private String corporation;
	private float rssi;
	private int retryCount;
	private int packetCount;
	private int byteCount;
	private String networkName;
	private String ipAddress;
	private String userName;
	private boolean permitted = false;
	private boolean resource = false;

	public Link()
	{

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
		if (corporation == null)
		{
			// Get corporation part of mac address
			// First three hex pairs, separated by '-'
			String mac = macAddress.replace(':', '-').substring(0, 8);
			if (mac.indexOf('-') < 0)
			{
				mac = macAddress.substring(0, 2) + "-" + macAddress.substring(2, 4) + "-" + macAddress.substring(4, 6);
			}

			corporation = macCorporations.get(mac);

			if (corporation == null)
			{
				corporation = "Unknown";
				if (lastTried == 0 || (new Date().getTime() - lastTried) > 360000)
				{
					try
					{
						final URL url = new URL(ieeeURL + mac);
						final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
						while (true)
						{
							final String line = reader.readLine();
							if (line == null)
							{
								break;
							}
							if (line.contains("(hex)"))
							{
								final int index = line.indexOf("(hex)");
								corporation = line.substring(index + 5).trim();
								macCorporations.put(macAddress, corporation);
								return corporation;
							}
						}
						lastTried = 0;
					}
					catch (final Exception e)
					{
						lastTried = new Date().getTime();
						e.printStackTrace();
					}
				}
			}
		}

		return corporation;
	}

	public String getMacAddress()
	{
		return macAddress;
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

	public double getTimeStamp()
	{
		return timeStamp;
	}

	public boolean isResource()
	{
		return resource;
	}

	public void setPermitted(final boolean b, final double since)
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

	public void setUsername(final String name)
	{
		userName = name;
	}

	public String toJSON()
	{
		final StringBuffer buffer = new StringBuffer();
		buffer.append("{");

		buffer.append("macAddress:\"");
		buffer.append(macAddress);
		buffer.append("\"");

		buffer.append(",");

		buffer.append("timeStamp:");
		buffer.append((int) (timeStamp / 1000));

		buffer.append(",");

		buffer.append("corporation:\"");
		buffer.append(getCorporation());
		buffer.append("\"");

		buffer.append(",");

		buffer.append("rssi:");
		buffer.append(rssi);

		buffer.append(",");

		buffer.append("packetCount:");
		buffer.append(packetCount);

		buffer.append(",");

		buffer.append("retryCount:");
		buffer.append(retryCount);

		buffer.append(",");

		buffer.append("byteCount:");
		buffer.append(byteCount);

		buffer.append(",");

		buffer.append("permitted:");
		buffer.append(permitted);

		// buffer.append("zone:");
		// //if(ipAddress != null)
		// //{
		// buffer.append(zone);
		// //}
		// //else
		// //{
		// // buffer.append(0);
		// //}

		buffer.append(",");

		buffer.append("deviceName:\"");
		if (userName != null && !userName.trim().isEmpty())
		{
			buffer.append(userName);
		}
		else if (networkName != null && !networkName.trim().isEmpty())
		{
			buffer.append(networkName);
		}
		else if (corporation != null)
		{
			String text = corporation;
			int cut = text.indexOf(' ');
			if (cut != -1)
			{
				text = text.substring(0, cut);
			}

			cut = text.indexOf(',');
			if (cut != -1)
			{
				text = text.substring(0, cut);
			}
			buffer.append(text + " Device");
		}
		else
		{
			buffer.append("Unknown Device");
		}
		buffer.append("\"");

		if (ipAddress != null)
		{
			buffer.append(",");

			buffer.append("ipAddress:\"");
			buffer.append(ipAddress);
			buffer.append("\"");
		}

		if (resource)
		{
			buffer.append(",resource:true");
		}

		buffer.append("}");
		return buffer.toString();
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
			networkName = lease.getHostName();
			ipAddress = lease.getIpAddress();
		}
	}

	public void update(final Link link)
	{
		timeStamp = link.timeStamp;
		macAddress = link.macAddress;
		packetCount = link.packetCount;
		retryCount = link.retryCount;
		byteCount = link.byteCount;
		rssi = link.rssi;

	}
}