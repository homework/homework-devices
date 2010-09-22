package uk.ac.nott.mrl.homework.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ZoneServlet extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		// response.setContentType("application/json");
		// //logger.info(request.getRequestURL().toString());
		//
		// String macAddress = request.getParameter("macAddress");
		// String zoneString = request.getParameter("zone");
		// int zone = Integer.parseInt(zoneString);
		//
		// System.out.println("Set Zone :" + macAddress + " - " + zoneString);
		//
		// synchronized (LinkServlet.links)
		// {
		// final Link link = LinkServlet.links.get(macAddress);
		// if(link != null)
		// {
		// link.setZone(zone);
		// }
		// }
		//
		// String sinceString = request.getParameter("since");
		// double since = 0;
		// try
		// {
		// since = Double.parseDouble(sinceString);
		// }
		// catch (Exception e)
		// {
		// }
		//
		// LinkServlet.listLinks(response.getWriter(), since);
	}
}