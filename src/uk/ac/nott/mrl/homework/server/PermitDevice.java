package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PermitDevice extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");

		System.out.println("Permit:" + macAddress);

		final URL url = new URL("http://192.168.9.1/ws.v1/homework/permit/" + macAddress);
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		final String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (final Exception e)
		{
		}

		ListLinks.updatePermitted(conn.getInputStream(), since);

		Log.log("Permit Device", macAddress);
		
		ListLinks.listLinks(response.getWriter(), since);
	}
}