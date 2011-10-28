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

import uk.ac.nott.mrl.homework.server.model.Device;
import uk.ac.nott.mrl.homework.server.model.Model;

public class SetDeviceStatus extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(SetDeviceStatus.class.getName());

	private String fixMacAddress(final String macAddress)
	{
		if(macAddress.indexOf(':') == -1)
		{
			return macAddress.substring(0,2) + ":" + macAddress.substring(2,4) + ":" + macAddress.substring(4,6) + ":" + macAddress.substring(6,8) + ":" + macAddress.substring(8,10) + ":" + macAddress.substring(10,12); 
		}
		return macAddress;
	}
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());
		

		final String macAddress = request.getParameter("macAddress");
		
		final Connection connection = ModelController.createRPCConnection();
		try
		{
			ModelController.updateLeases(connection);
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}

		final Device device = Model.getModel().getDeviceByMac(macAddress);
		final String owner = request.getParameter("owner");
		if(device != null && device.getIPAddress() != null)
		{
			if(owner != null)
			{
				try
				{
					final String query = String.format("SQL:INSERT into Users values (\"%s\", \"%s\") on duplicate key update",
							device.getIPAddress(), owner);
					String result = connection.call(query);
					if(!result.startsWith("0<|>Success"))
					{
						logger.warning("Failed query:" + query + ";" + result);
					}
					else
					{
						Log.log("Set Device User", request.getRemoteAddr() + ": " + device.getIPAddress() + " = " + owner);						
					}						
				}
				catch(Exception e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}			
			}
			
			final String name = request.getParameter("name");
			if(name != null)
			{
				try
				{
					final String query = String.format("SQL:INSERT into DeviceNames values (\"%s\", \"%s\") on duplicate key update",
							device.getIPAddress(), name);
					String result = connection.call(query);
					if(!result.startsWith("0<|>Success"))
					{
						logger.warning("Failed query:" + query + ";" + result);
					}
					else
					{
						Log.log("Set Device Name", request.getRemoteAddr() + ": " + device.getIPAddress() + " = " + name);						
					}					
				}
				catch(Exception e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			
			final String type = request.getParameter("type");	
			if(type != null)
			{
				try
				{
					final String query = String.format("SQL:INSERT into DeviceTypes values (\"%s\", \"%s\") on duplicate key update",
							device.getIPAddress(), type);
					String result = connection.call(query);
					if(!result.startsWith("0<|>Success"))
					{
						logger.warning("Failed query:" + query + ";" + result);
					}
					else
					{
						Log.log("Set Device Type", request.getRemoteAddr() + ": " + device.getIPAddress() + " = " + type);						
					}
				}
				catch(Exception e)
				{
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}

		final String command = request.getParameter("command");
		if(command != null)
		{
			logger.info(command + " " + macAddress);
			
			try
			{
				final String query = String.format(	"SQL:INSERT into NoxCommand values (\"%s\", \"%s\", \"%s\", \"%s\")",
													UUID.randomUUID().toString(), command, "ETH|" + fixMacAddress(macAddress), "User");
				final String result = connection.call(query);
				if(!result.startsWith("0<|>Success"))
				{
					logger.warning("Failed query:" + query + ";" + result);
				}
				else
				{
					Log.log(command, request.getRemoteAddr() + ": " + macAddress);					
				}
			}
			catch (final Exception e)
			{
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		
		connection.disconnect();

	}
}