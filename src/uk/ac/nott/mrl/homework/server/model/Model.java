package uk.ac.nott.mrl.homework.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Model
{
	private static final Model model = new Model();
	// private static final Logger logger = Logger.getLogger(Model.class.getName());

	private final Map<String, Lease> leases = new HashMap<String, Lease>();
	private final Map<String, NoxStatus> statuses = new HashMap<String, NoxStatus>();	
	private final Map<String, Device> devices = new HashMap<String, Device>();
	private final Map<String, Item> items = new HashMap<String, Item>();

	private long mostRecentNoxStatus = 0;
	private long mostRecentDevice = 0;
	private long mostRecentLease = 0;

	// private static final int inactive = 5000;
	private static final int timeout = 20000;

	public static Model getModel()
	{
		return model;
	}

	public static int getTimeout()
	{
		return timeout;
	}

	public void add(final Device device)
	{
		device.initCorporation();
		mostRecentDevice = Math.max(device.getTimestamp(), mostRecentDevice);
		final Lease lease = leases.get(device.getMacAddress());
		if (lease != null)
		{
			device.update(lease);
		}
		
		final NoxStatus status = statuses.get(device.getMacAddress());
		if(status != null)
		{
			device.update(status);
		}

		final Device existingDevice = devices.get(device.getMacAddress());
		if (existingDevice != null)
		{
			final String oldID = existingDevice.getID();
			existingDevice.update(device);
			deviceUpdated(oldID, existingDevice);
		}
		else
		{
			final String id = device.getID();
			Item item = items.get(id);
			if (item == null)
			{
				item = new Item(device);
				items.put(id, item);
			}
			else
			{
				item.add(device);
			}
			synchronized (devices)
			{
				devices.put(device.getMacAddress(), device);
			}
		}
	}
	
	public void add(final NoxStatus status)
	{
		mostRecentNoxStatus = Math.max(status.getTimestamp(), mostRecentNoxStatus);
		final Device device = devices.get(status.getMacAddress());
		statuses.put(status.getMacAddress(), status);
		if (device != null)
		{
			final String oldID = device.getID();
			device.update(status);
			deviceUpdated(oldID, device);
		}		
	}

	public void add(final Lease lease)
	{
		mostRecentLease = Math.max(lease.getTimestamp(), mostRecentLease);
		final Device device = devices.get(lease.getMacAddress());
		final Lease oldLease = leases.get(lease.getMacAddress());
		if (oldLease != null)
		{
			oldLease.update(lease);
			if (device != null)
			{
				final String oldID = device.getID();
				device.update(oldLease);
				deviceUpdated(oldID, device);
			}
		}
		else
		{
			leases.put(lease.getMacAddress(), lease);
			if (device != null)
			{
				final String oldID = device.getID();
				device.update(lease);
				deviceUpdated(oldID, device);
			}
		}
	}

	public void clearOld()
	{
		final long timestamp = new Date().getTime();
		final long timeout = timestamp - getTimeout();
		final Collection<Device> removals = new ArrayList<Device>();
		synchronized (devices)
		{
			for (final Device device : devices.values())
			{
				if (timeout > device.getTimestamp())
				{
					removals.add(device);
				}
			}
		}

		for (final Device device : removals)
		{
			remove(device, timestamp);
		}
	}

	public void deviceUpdated(final String oldID, final Device device)
	{
		if (oldID.equals(device.getID()))
		{
			final Item item = items.get(device.getID());
			if (item != null)
			{
				item.update(device);
			}
		}
		else
		{
			final Item oldItem = items.get(oldID);
			final Item newItem = items.get(device.getID());
			oldItem.remove(device, device.getTimestamp());
			if (newItem == null)
			{
				final Item item = new Item(device);
				items.put(device.getID(), item);
			}
			else
			{
				newItem.add(device);
			}
		}
	}

	public Device getDevice(final String macAddress)
	{
		return devices.get(macAddress);
	}

	public int getDeviceCount()
	{
		return devices.size();
	}

	public Iterable<Device> getDevices()
	{
		return devices.values();
	}

	public Item getItem(final String id)
	{
		return items.get(id);
	}

	public Iterable<Item> getItems()
	{
		return items.values();
	}

	public long getMostRecentDevice()
	{
		return Math.max(mostRecentDevice, new Date().getTime() - timeout) + 1;
	}

	public long getMostRecentNoxStatus()
	{
		return mostRecentNoxStatus + 1;
	}
	
	public long getMostRecentLease()
	{
		return mostRecentLease + 1;
	}

	public void remove(final Device device, final long timestamp)
	{
		synchronized (devices)
		{
			devices.remove(device.getMacAddress());
		}

		final Item item = items.get(device.getID());
		item.remove(device, timestamp);
	}

	public void remove(final Item item)
	{
		items.get(item.getID());
	}

//	public void setState(final String macAddress, final State state, final long timestamp)
//	{
//		final Device device = devices.get(macAddress);
//		if (device != null)
//		{
//			final String oldID = device.getID();
//			device.setState(state, timestamp);
//			deviceUpdated(oldID, device);
//		}
//	}
}