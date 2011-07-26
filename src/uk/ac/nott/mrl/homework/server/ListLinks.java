package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;
import uk.ac.nott.mrl.homework.server.model.Link.State;
import uk.ac.nott.mrl.homework.server.model.Permitted;

import com.google.gson.Gson;

public class ListLinks extends HttpServlet
{
	public static final Collection<String> routerMacAddresses = new HashSet<String>();

	static long last = 0;

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

	private static final Logger logger = Logger.getLogger(ListLinks.class.getName());

	private static final long OLD = 12000; // 12 seconds

	public static void addLink(final Link link)
	{
		synchronized (links)
		{
			links.put(link.getMacAddress(), link);
		}
	}

	public static Link getLink(final String macAddress)
	{
		return links.get(macAddress);
	}

	public static void listLinks(final PrintWriter writer, final long since)
	{
		writer.println("[");
		boolean comma = false;
		// createTestLinks();
		synchronized (links)
		{
			final List<Link> sorted = new ArrayList<Link>(links.values());
			Collections.sort(sorted, linkComparator);
			long lastTime;
			if (last > 0)
			{
				lastTime = last;
			}
			else
			{
				lastTime = 0;
			}

			for (final Link link : sorted)
			{
				final double timeStamp = link.getTimeStamp();
				if (lastTime - timeStamp > OLD)
				{
					continue;
				}

				link.initCorporation();

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

				final Gson gson = new Gson();
				writer.println(gson.toJson(link));
			}
		}
		writer.println("]");
		writer.flush();
	}

	// private static void createTestLinks()
	// {
	// synchronized (links)
	// {
	// links.clear();
	// Link link = new Link("Tom's Laptop", "00:23:76:0c:3d:94", "192.168.1.30", 0, 10);
	// link.initCorporation();
	// links.put(link.getMacAddress(), link);
	//
	// link = new Link(null, "00:23:76:0c:3d:93", null, 0, 5); // HTC
	// link.setState(State.requesting, new Date().getTime());
	// link.initCorporation();
	// links.put(link.getMacAddress(), link);
	//
	// link = new Link(null, "28:E7:CF:0c:3d:93", "192.168.1.10", -60, 3); // Apple
	// link.initCorporation();
	// links.put(link.getMacAddress(), link);
	//
	// link = new Link(null, "58:94:6B:0c:3d:93", "192.168.1.11", -55, 5); // Intel
	// link.initCorporation();
	// links.put(link.getMacAddress(), link);
	//
	// link = new Link(null, "00:0F:B5:0c:3d:93", null, -75, 5); // Netgear
	// link.initCorporation();
	// links.put(link.getMacAddress(), link);
	// }
	// }

	public static void updatePermitted(final InputStream inputStream, final long since)
	{
		try
		{
			final Gson gson = new Gson();
			final Permitted permitted = gson.fromJson(new InputStreamReader(inputStream), Permitted.class);
			for (final String macAddress : permitted)
			{
				final Link link = links.get(macAddress);
				if (link != null)
				{
					link.setState(State.permitted, since);
				}
			}
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void init() throws ServletException
	{
		super.init();
		logger.info("Init");

		try
		{
			final Enumeration<NetworkInterface> newIterfaces = NetworkInterface.getNetworkInterfaces();
			while (newIterfaces.hasMoreElements())
			{
				final NetworkInterface netiface = newIterfaces.nextElement();
				final byte[] mac = netiface.getHardwareAddress();
				if (mac != null && mac.length == 6)
				{
					/*
					 * Convert to string of form: 08:00:27:DC:4A:9E.
					 */
					final StringBuffer macString = new StringBuffer();
					for (int i = 0; i < mac.length; i++)
					{
						macString.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
					}
					final Enumeration<InetAddress> ipAddresses = netiface.getInetAddresses();
					while (ipAddresses.hasMoreElements())
					{
						final InetAddress ipAddress = ipAddresses.nextElement();
						if (ipAddress.getHostAddress().startsWith("128.243."))
						{
							System.setProperty("http.proxyHost", "proxy.nottingham.ac.uk");
							System.setProperty("http.proxyPort", "8080");
							System.setProperty("http.nonProxyHosts", "192.168.*");
						}
					}
					routerMacAddresses.add(macString.toString());
				}
			}
		}
		catch (final SocketException e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
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
		long since = 0;
		try
		{
			since = Long.parseLong(sinceString);
		}
		catch (final Exception e)
		{
			System.out.println(sinceString);
		}

		listLinks(response.getWriter(), since);
	}
}