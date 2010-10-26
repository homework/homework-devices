package uk.ac.nott.mrl.homework.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;

public class SetResource extends HttpServlet
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

		final Link link = ListLinks.getLink(macAddress);
		if (link != null)
		{
			link.setResource(resource);
		}

		final String sinceString = request.getParameter("since");
		long since = 0;
		try
		{
			since = Long.parseLong(sinceString);
		}
		catch (final Exception e)
		{
		}

		ListLinks.listLinks(response.getWriter(), since);
	}
}