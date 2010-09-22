package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DenyServlet extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		String macAddress = request.getParameter("macAddress");

		System.out.println("Deny:" + macAddress);

		URL url = new URL("http://192.168.9.1/ws.v1/homework/deny/" + macAddress);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		String sinceString = request.getParameter("since");
		double since = 0;
		try
		{
			since = Double.parseDouble(sinceString);
		}
		catch (Exception e)
		{
		}

		LinkServlet.updatePermitted(conn.getInputStream(), since);

		LinkServlet.listLinks(response.getWriter(), since);
	}
}