package uk.ac.nott.mrl.homework.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;

public class ResourceServlet extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");
		final String zoneString = request.getParameter("resource");
		final boolean resource = Boolean.parseBoolean(zoneString);

		System.out.println("Set Resource :" + macAddress + " - " + zoneString);

		synchronized (LinkServlet.links)
		{
			final Link link = LinkServlet.links.get(macAddress);
			if (link != null)
			{
				link.setResource(resource);
			}
		}

		final String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (final Exception e)
		{
		}

		LinkServlet.listLinks(response.getWriter(), since);
	}
}