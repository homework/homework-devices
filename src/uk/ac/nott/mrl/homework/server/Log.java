package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.http.client.URL;

public class Log extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(Log.class.getName());

	public static void log(final String eventType, final String details) throws IOException
	{
		try
		{
			final JavaSRPC rpc = new JavaSRPC();
			rpc.connect(InetAddress.getByName(PollingThread.hwdbHost), 987);
			final String query = String.format(	"SQL:INSERT into UserEvents values (\"%s\", \"%s\", \"%s\")",
												"Control App", eventType, details);
			rpc.call(query);
			rpc.disconnect();
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");

		final String type = URL.decode(request.getParameter("type"));
		final String details = URL.decode(request.getParameter("details"));

		log(type, details);
		logger.info(type + ": " + details);
	}
}