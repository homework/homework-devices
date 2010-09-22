package uk.ac.nott.mrl.homework.server.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LinkDumpReader
{

	/**
	 * @param args
	 */
	public static void main(final String[] args)
	{
		System.setProperty("http.proxyHost", "wwwcache.cs.nott.ac.uk");
		System.setProperty("http.proxyPort", "3128");
		try
		{
			final BufferedReader reader = new BufferedReader(new InputStreamReader(LinkDumpReader.class
					.getClassLoader().getResource("linkdump.txt").openStream()));

			String line;
			while ((line = reader.readLine()) != null)
			{
				final Link link = Link.parseLink(line);
				System.err.println(link.toString());
			}
		}
		catch (final Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
