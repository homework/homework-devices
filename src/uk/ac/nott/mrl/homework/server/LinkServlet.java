package uk.ac.nott.mrl.homework.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import uk.ac.nott.mrl.homework.server.model.Link;

public class LinkServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(LinkServlet.class.getName());

	private static final long OLD = 12000; // 12 seconds

	private static final Comparator<Link> linkComparator = new Comparator<Link>()
	{
		@Override
		public int compare(final Link o1, final Link o2)
		{
			if (o1.getRssi() > o2.getRssi())
			{
				return -1;
			}
			else if (o1.getRssi() < o2.getRssi()) { return 1; }
			return 0;
		}
	};

	private static final Map<String, Link> links = new HashMap<String, Link>();

	static Date last = null;
	
	private static final boolean allowCisco = false;

	public static Link getLink(String macAddress)
	{
		return links.get(macAddress);
	}
	
	public static void listLinks(final PrintWriter writer, final double since)
	{
		writer.println("[");
		boolean comma = false;
		synchronized (links)
		{
			final List<Link> sorted = new ArrayList<Link>(links.values());
			Collections.sort(sorted, linkComparator);
			long lastTime;
			if (last != null)
			{
				lastTime = last.getTime();
			}
			else
			{
				lastTime = 0;
			}

			for (final Link link : sorted)
			{
				final double timeStamp = link.getTimeStamp();
				if (!link.isResource() && lastTime - timeStamp > OLD)
				{
					continue;
				}

				if (!allowCisco && link.getCorporation() != null && link.getCorporation().startsWith("Cisco"))
				{
					continue;
				}

				if (timeStamp < since)
				{
					continue;
				}

				if (comma)
				{
					writer.print(",");
				}
				else
				{
					comma = true;
				}
				writer.println(link.toJSON());
			}
		}
		writer.println("]");
	}

	public static void addLink(final Link link)
	{
		synchronized (links)
		{
			links.put(link.getMacAddress(), link);
		}
	}
	
	public static void updatePermitted(final InputStream inputStream, final double since)
	{
		try
		{
			final JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream)));
			logger.info(jsonObject.toString());
			final JSONArray array = jsonObject.getJSONArray("permitted");
			for (int index = 0; index < array.length(); index++)
			{
				try
				{
					final String macAddress = array.getString(index);
					final Link link = links.get(macAddress);
					link.setPermitted(true, since);
				}
				catch (final JSONException e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);;
				}

			}
		}
		catch (final JSONException e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void init() throws ServletException
	{
		super.init();
		logger.info("Init");

		// System.setProperty("http.proxyHost", "wwwcache.cs.nott.ac.uk");
		// System.setProperty("http.proxyPort", "3128");
		System.setProperty("http.proxyHost", "proxy.nottingham.ac.uk");
		System.setProperty("http.proxyPort", "8080");
		System.setProperty("http.nonProxyHosts", "192.168.9.*");

		try
		{
			final URL url = getClass().getClassLoader().getResource("uk/ac/nott/mrl/homework/server/oui.txt");
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
					final String mac = line.substring(0, 8).trim();
					Link.macCorporations.put(mac, corporation);
				}
			}
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		new PollingThread().start();
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (final Exception e)
		{
			System.out.println(sinceString);
		}

		listLinks(response.getWriter(), since);
	}
}