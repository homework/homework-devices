package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hwdb.srpc.Connection;

public class Log extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(Log.class.getName());

	public static void log(final String eventType, final String details) throws IOException
	{
		try
		{
			final Connection connection = ModelController.createRPCConnection();
			final String query = String.format(	"SQL:INSERT into UserEvents values (\"%s\", \"%s\", \"%s\")",
												"Control App", eventType, details);
			connection.call(query);
			connection.disconnect();
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

		final String type = URLDecoder.decode(request.getParameter("type"), "UTF-8");
		final String details = request.getRemoteAddr() + ": " + URLDecoder.decode(request.getParameter("details"), "UTF-8");

		log(type, details);
		//logger.info(type + ": " + details);
	}
}