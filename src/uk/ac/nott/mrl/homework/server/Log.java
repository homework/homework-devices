package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Log extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(Log.class.getName());
	
	public static void log(final String eventType, final String details) throws IOException
	{
		final JavaSRPC rpc = new JavaSRPC();
		rpc.connect(InetAddress.getByName("192.168.9.1"), 987);
		String query = String.format("SQL:INSERT into UserEvents values (\"%s\", \"%s\", \"%s\")", "Control App", eventType, details);
		logger.info(query);
		String result = rpc.call(query);
		logger.info(result);
		
	}
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		// final String macAddress = request.getParameter("macAddress");
		final String type = request.getParameter("type");
		final String details = request.getParameter("details");

		log(type, details);
		System.out.println("Log: " +  type + " - " + details);
		
	}
}