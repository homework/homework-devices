package uk.ac.nott.mrl.homework.server.model;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NoxStatus
{
	private static final Logger logger = Logger.getLogger(NoxStatus.class.getName());

	public static void parseResultSet(final String results, final Model model)
	{
		final ResultSet result = new ResultSet(results);
		if(result.getSize() != 0)
		{
			System.out.println("New statuses: " + result.getSize());
		}
		for(String[] columns: result)
		{			
			try
			{
				final NoxStatus status = new NoxStatus();
				final String time = columns[0].substring(1, columns[0].length() - 1);
				final long timeLong = Long.parseLong(time, 16);
				status.timestamp = timeLong / 1000000;
				status.macAddress = columns[1].toLowerCase();
				if(status.macAddress.startsWith("eth|"))
				{
					status.macAddress = status.macAddress.substring(4);
				}
				status.state = columns[2].toLowerCase();
				status.source = columns[3];

				model.add(status);
			}
			catch (final Exception e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private String macAddress;
	private String state;
	private String source;
	private long timestamp;

	public NoxStatus()
	{

	}

	public String getMacAddress()
	{
		return macAddress;
	}

	public String getState()
	{
		return state;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public String getSource()
	{
		return source;
	}

	@Override
	public String toString()
	{
		return timestamp + ": " + macAddress;
	}
}