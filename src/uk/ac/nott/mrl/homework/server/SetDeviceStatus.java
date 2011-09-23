package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hwdb.srpc.Connection;

public class SetDeviceStatus extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(SetDeviceStatus.class.getName());

	private String shortenMacAddress(final String macAddress)
	{
		String[] pairs = null;
		if(macAddress.indexOf(':') > -1)
		{
			pairs = macAddress.split(":");
		}
		else if(macAddress.indexOf('-') > -1)
		{
			pairs = macAddress.split("-");
		}
		
		String result = macAddress;
		if(pairs != null)
		{
			result = "";
			for(String pair: pairs)
			{
				result += pair;
			}
		}
		return result;
	}
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");
		final String command = request.getParameter("command");

		logger.info(command + " " + macAddress);
		
		try
		{
			final Connection connection = ModelController.createRPCConnection();
			final String query = String.format(	"SQL:INSERT into NoxCommand values (\"%s\", \"%s\", \"%s\", \"%s\")",
												UUID.randomUUID().toString(), command, "ETH|" + shortenMacAddress(macAddress), "User");
			connection.call(query);
			connection.disconnect();

			Log.log(command, macAddress);
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		final String sinceString = request.getParameter("since");
		long since = 0;
		try
		{
			since = Long.parseLong(sinceString);
		}
		catch (final Exception e)
		{
		}

		ModelController.listItems(response.getWriter(), since);
	}
}