package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;

public class NameServlet extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");
		final String nameString = URLDecoder.decode(request.getParameter("name"), "UTF-8");

		System.out.println("Set Name :" + macAddress + " - " + nameString);

		final Link link = LinkServlet.getLink(macAddress);
		if (link != null)
		{
			link.setUsername(nameString);
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