package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DenyDevice extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(PermitDevice.class.getName());	
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");

		logger.info("Deny:" + macAddress);

		final URL url = new URL("http://" + ModelController.hwdbHost + "/ws.v1/homework/deny/" + macAddress);
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		final String sinceString = request.getParameter("since");
		long since = 0;
		try
		{
			since = Long.parseLong(sinceString);
		}
		catch (final Exception e)
		{
		}

		ModelController.updatePermitted(conn.getInputStream(), new Date().getTime());

		Log.log("Deny Device", macAddress);

		ModelController.listItems(response.getWriter(), since);
	}	
}