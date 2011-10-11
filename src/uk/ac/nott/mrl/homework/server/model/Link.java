package uk.ac.nott.mrl.homework.server.model;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Link
{
	private static final Logger logger = Logger.getLogger(Link.class.getName());

	public static void parseResultSet(final String results, final Model model)
	{
		final ResultSet result = new ResultSet(results);
		if(result.getSize() != 0)
		{
			System.out.println("New links: " + result.getSize());
		}
		for(String[] columns: result)
		{			
			try
			{
				final Link link = new Link();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				link.timestamp = timeLong / 1000000;
				link.macAddress = columns[1].toLowerCase();
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
	private String macAddress;
	private int packetCount;
	private int retryCount;

	private float rssi;

	private long timestamp;

	public Link()
	{

	}
	
	public int getByteCount()
	{
		return byteCount;
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

	public long getTimestamp()
	{
		return timestamp;
	}

	public void update(final Link link)
	{
		timestamp = Math.max(link.getTimestamp(), timestamp);
		macAddress = link.macAddress;
		packetCount = link.packetCount;
		retryCount = link.retryCount;
		byteCount = link.byteCount;
		rssi = link.rssi;
	}
}