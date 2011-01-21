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
import uk.ac.nott.mrl.homework.server.model.Permitted;

import com.google.gson.Gson;

public class ListLinks extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(ListLinks.class.getName());

	public static final Collection<String> routerMacAddresses = new HashSet<String>();

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

	private static boolean allowCisco = true;

	static long last = 0;

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
				if (!link.isResource() && lastTime - timeStamp > OLD)
				{
					continue;
				}

				link.initCorporation();
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

				final Gson gson = new Gson();
				writer.println(gson.toJson(link));
			}
		}
		writer.println("]");
		writer.flush();
	}

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
					link.setPermitted(true, since);
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
					System.out.println(netiface.getDisplayName());
					System.out.println(macString);
					final Enumeration<InetAddress> ipAddresses = netiface.getInetAddresses();
					while (ipAddresses.hasMoreElements())
					{
						final InetAddress ipAddress = ipAddresses.nextElement();
						if (ipAddress.getHostAddress().startsWith("128.243."))
						{
							allowCisco = false;

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