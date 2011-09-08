package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hwdb.srpc.Connection;
import org.hwdb.srpc.SRPC;

import uk.ac.nott.mrl.homework.server.model.Device;
import uk.ac.nott.mrl.homework.server.model.Item;
import uk.ac.nott.mrl.homework.server.model.Lease;
import uk.ac.nott.mrl.homework.server.model.Model;
import uk.ac.nott.mrl.homework.server.model.Permitted;
import uk.ac.nott.mrl.homework.server.model.State;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelController
{
	public static final String hwdbHost = "localhost";
	private static final int hwdbPort = 987;
	private static final String hwdbService = "HWDB";
	
	private static SRPC srpc;
	
	private final static Logger logger = Logger.getLogger(ModelController.class.getName());
	
	public  static void updateModel() throws IOException
	{
		final Connection connection = createRPCConnection();
	
		try
		{
			updateLeases(connection);
			updateLinks(connection);
			
			updatePermitted();
			
			Model.getModel().clearOld();
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		finally
		{
			connection.disconnect();
		}
	}
	
	public static Connection createRPCConnection() throws IOException
	{
		if(srpc == null)
		{
			srpc = new SRPC();
		}
		return srpc.connect(hwdbHost, hwdbPort, hwdbService);
	}
	
	public static void updatePermitted(InputStream is, long timestamp)
	{
		final Gson gson = new Gson();
		final Permitted permitted = gson.fromJson(new InputStreamReader(is), Permitted.class);
		for (final String macAddress : permitted.permitted())
		{
			Model.getModel().setState(macAddress, State.permitted, timestamp);
		}
		for (final String macAddress : permitted.denied())
		{
			Model.getModel().setState(macAddress, State.denied, timestamp);
		}		
	}
	
	private static void updatePermitted()
	{
		try
		{
			final URL url = new URL("http://" + hwdbHost + "/ws.v1/homework/status");
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);

			updatePermitted(conn.getInputStream(), new Date().getTime());
		}
		catch (final ConnectException e)
		{
			logger.warning("Failed to connect to nox service. " + e.getMessage());
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private static void updateLinks(Connection connection) throws Exception
	{
		String s = String.format("@%016x@", Model.getModel().getMostRecentDevice() * 1000000);
		String linkQuery = String.format("SQL:select * from Links [ since %s ]", s);
		final String linkResults = connection.call(linkQuery);
		if (linkResults != null)
		{
			Device.parseResultSet(linkResults, Model.getModel());
		}
	}
	
	private static void updateLeases(Connection connection) throws Exception
	{
		String leaseQuery = "SQL:select * from Leases";
		if(Model.getModel().getMostRecentLease() != 0)
		{
			String s = String.format("@%016x@", Model.getModel().getMostRecentLease() * 1000000);
			leaseQuery = String.format("SQL:select * from Leases [ since %s ]", s);
		}
		final String leaseResults = connection.call(leaseQuery);
		if (leaseResults != null)
		{
			Lease.parseResultSet(leaseResults, Model.getModel());
		}
	}

	
	public static void listGroupItems(final PrintWriter writer, final String group)
	{
		Model model = Model.getModel();
		synchronized (model)
		{
			final Item item = model.getItem(group);
			final Collection<String> macAddresses = new ArrayList<String>();
			for(Device device: item.getDevices())
			{
				macAddresses.add(device.getMacAddress());
			}
			final Gson gson = new Gson();
			
			String result = gson.toJson(macAddresses);
			writer.println(result);			
		}
		writer.flush();
	}	
	
	public static void listItems(final PrintWriter writer, final long since)
	{
		Model model = Model.getModel();
		synchronized (model)
		{
			final Collection<Item> result = new ArrayList<Item>();
			final Collection<Item> removals = new ArrayList<Item>();
			final long timeout = new Date().getTime() - Model.getTimeout();
			for (final Item item: model.getItems())
			{
				final long timestamp = item.getTimestamp();
				if(timestamp < timeout)
				{
					removals.add(item);
					continue;
				}

				if(timestamp > since)
				{
					result.add(item);
				}				
			}

			final GsonBuilder builder = new GsonBuilder();
			builder.excludeFieldsWithoutExposeAnnotation();
			final Gson gson = builder.create();
			String resultString = gson.toJson(result);
			writer.println(resultString);			
			//logger.info(resultString);
			
			for(final Item item: removals)
			{
				model.remove(item);
			}			
		}
		writer.flush();
	}	
}