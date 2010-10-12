package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;

public class SetName extends HttpServlet
{
	private static final Logger logger  = Logger.getLogger(SetName.class.getName());
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");
		final String nameString = URLDecoder.decode(request.getParameter("name"), "UTF-8");

		System.out.println("Set Name :" + macAddress + " - " + nameString);

		final Link link = ListLinks.getLink(macAddress);
		if (link != null)
		{
			link.setUsername(nameString, ListLinks.last);
		}

		final JavaSRPC rpc = new JavaSRPC();
		rpc.connect(InetAddress.getByName("192.168.9.1"), 987);
		String query = String.format("SQL:INSERT into Leases values (\"upd\", \"%s\", \"%s\", \"%s\")", link.getMacAddress(), link.getIPAddress(), nameString);
		logger.info(query);
		String result = rpc.call(query);
		logger.info(result);
		
		final String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		Log.log("Rename Device", macAddress);		

		ListLinks.listLinks(response.getWriter(), since);
	}
}