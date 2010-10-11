package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Link;

public class SetName extends HttpServlet
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

		final Link link = ListLinks.getLink(macAddress);
		if (link != null)
		{
			link.setUsername(nameString);
		}

		final JavaSRPC rpc = new JavaSRPC();
		rpc.connect(InetAddress.getByName("192.168.9.1"), 987);
		rpc.call(String.format("SQL:INSERT INTO Leases (UPD, %s, %s, %s)", link.getMacAddress(), link.getIPAddress(), nameString));
		
		final String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (final Exception e)
		{
		}
		
		Log.log("Rename Device", macAddress);		

		ListLinks.listLinks(response.getWriter(), since);
	}
}