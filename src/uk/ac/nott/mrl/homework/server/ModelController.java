package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.io.PrintWriter;
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
import uk.ac.nott.mrl.homework.server.model.NoxStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelController
{
	public static final String hwdbHost = "localhost";
	private static final int hwdbPort = 987;
	private static final String hwdbService = "HWDB";

	private static SRPC srpc;

	private final static Logger logger = Logger.getLogger(ModelController.class.getName());

	public static Connection createRPCConnection() throws IOException
	{
		if (srpc == null)
		{
			srpc = new SRPC();
		}
		return srpc.connect(hwdbHost, hwdbPort, hwdbService);
	}

	public static void listGroupItems(final PrintWriter writer, final String group)
	{
		final Model model = Model.getModel();
		synchronized (model)
		{
			final Item item = model.getItem(group);
			final Collection<String> macAddresses = new ArrayList<String>();
			for (final Device device : item.getDevices())
			{
				macAddresses.add(device.getMacAddress());
			}
			final Gson gson = new Gson();

			final String result = gson.toJson(macAddresses);
			writer.println(result);
		}
		writer.flush();
	}

	public static void listItems(final PrintWriter writer, final long since)
	{
		final Model model = Model.getModel();
		synchronized (model)
		{
			final Collection<Item> result = new ArrayList<Item>();
			final Collection<Item> removals = new ArrayList<Item>();
			final long timeout = new Date().getTime() - Model.getTimeout();
			for (final Item item : model.getItems())
			{
				final long timestamp = item.getTimestamp();
				if (timestamp < timeout)
				{
					removals.add(item);
					continue;
				}

				if (timestamp > since)
				{
					result.add(item);
				}
			}

			final GsonBuilder builder = new GsonBuilder();
			builder.excludeFieldsWithoutExposeAnnotation();
			final Gson gson = builder.create();
			final String resultString = gson.toJson(result);
			writer.println(resultString);
			// logger.info(resultString);

			for (final Item item : removals)
			{
				model.remove(item);
			}
		}
		writer.flush();
	}

	private static void updateLeases(final Connection connection) throws Exception
	{
		String leaseQuery = "SQL:select * from Leases";
		if (Model.getModel().getMostRecentLease() != 0)
		{
			final String s = String.format("@%016x@", Model.getModel().getMostRecentLease() * 1000000);
			leaseQuery = String.format("SQL:select * from Leases [ since %s ]", s);
		}
		final String leaseResults = connection.call(leaseQuery);
		if (leaseResults != null)
		{
			Lease.parseResultSet(leaseResults, Model.getModel());
		}
	}

	private static void updateLinks(final Connection connection) throws Exception
	{
		final String s = String.format("@%016x@", Model.getModel().getMostRecentDevice() * 1000000);
		final String linkQuery = String.format("SQL:select * from Links [ since %s ]", s);
		final String linkResults = connection.call(linkQuery);
		if (linkResults != null)
		{
			Device.parseResultSet(linkResults, Model.getModel());
		}
	}

	public static void updateModel() throws IOException
	{
		final Connection connection = createRPCConnection();

		try
		{
			updateLeases(connection);
			updateLinks(connection);

			updatePermitted(connection);

			Model.getModel().clearOld();
		}
		catch (final Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		finally
		{
			connection.disconnect();
		}
	}

	private static void updatePermitted(final Connection connection) throws IOException
	{
		final String s = String.format("@%016x@", Model.getModel().getMostRecentNoxStatus() * 1000000);
		final String noxQuery = String.format("SQL:select * from NoxStatus [ since %s ]", s);
		final String noxResults = connection.call(noxQuery);
		if (noxResults != null)
		{
			NoxStatus.parseResultSet(noxResults, Model.getModel());
		}
	}

//	public static void updatePermitted(final InputStream is, final long timestamp)
//	{
//		final Gson gson = new Gson();
//		final Permitted permitted = gson.fromJson(new InputStreamReader(is), Permitted.class);
//		for (final String macAddress : permitted.permitted())
//		{
//			Model.getModel().setState(macAddress, State.permitted, timestamp);
//		}
//		for (final String macAddress : permitted.denied())
//		{
//			Model.getModel().setState(macAddress, State.denied, timestamp);
//		}
//	}
}