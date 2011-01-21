package uk.ac.nott.mrl.homework.server.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Companies
{
	private final Map<Integer, String> macCompanies = new HashMap<Integer, String>();

	private static final Logger logger = Logger.getLogger(Companies.class.getName());

	private static final String ieeeURL = "http://standards.ieee.org/cgi-bin/ouisearch?";

	private long lastTried = 0;

	public Companies()
	{
		try
		{
			final URL url = Companies.class.getClassLoader().getResource("uk/ac/nott/mrl/homework/server/oui.txt");
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
					final String corporation = line.substring(index + 5).trim();
					final int macValue = getMacValue(line.substring(0, 8).trim());
					macCompanies.put(macValue, corporation);
				}
			}
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public synchronized String getCompany(final String macAddress)
	{
		final int macValue = getMacValue(macAddress);
		String result = macCompanies.get(macValue);
		if (result == null)
		{
			result = "Unknown";
			logger.info(macAddress + " corporation not found (" + macCompanies.size() + ")");
			if (lastTried == 0 || (new Date().getTime() - lastTried) > 360000)
			{
				try
				{
					final URL url = new URL(ieeeURL + getIEEEMacFormat(macAddress));
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
							result = line.substring(index + 5).trim();
							macCompanies.put(macValue, result);
							return result;
						}
					}
					lastTried = 0;
				}
				catch (final Exception e)
				{
					lastTried = new Date().getTime();
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return result;

	}

	private String getIEEEMacFormat(final String macAddress)
	{
		return macAddress.replaceAll(":", "-").trim().substring(0, 8);
	}

	private int getMacValue(final String macAddress)
	{
		final String mac = macAddress.replaceAll(":", "").replaceAll("-", "").trim().substring(0, 6);
		return Integer.parseInt(mac, 16);
	}
}
