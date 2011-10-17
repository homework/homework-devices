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

	private final Map<String, Device> devices = new HashMap<String, Device>();
	private final Map<String, Item> items = new HashMap<String, Item>();

	private long mostRecentNoxStatus = 0;
	private long mostRecentLink = 0;
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

	public void add(final Link link)
	{
		mostRecentLink = Math.max(link.getTimestamp(), mostRecentLink);
		Device device = getDeviceByMac(link.getMacAddress());
		String oldID = device.getID();
		device.update(link);
		deviceUpdated(oldID, device);
	}
	
	public Device getDeviceByIP(String ipAddress)
	{
		for(Device device: devices.values())
		{
			if(ipAddress.equals(device.getIPAddress()))
			{
				return device;
			}
		}
		return null;		
	}
	
	public Device getDeviceByMac(String macAddress)
	{
		Device device = devices.get(macAddress);
		if(device == null)
		{
			Device newDevice = new Device(macAddress);
			newDevice.initCorporation();
			devices.put(macAddress, newDevice);
			return newDevice;
		}
		return device;
	}
	
	public void add(final NoxStatus status)
	{
		mostRecentNoxStatus = Math.max(status.getTimestamp(), mostRecentNoxStatus);
		final Device device = getDeviceByMac(status.getMacAddress());
		final String oldID = device.getID();
		device.update(status);
		deviceUpdated(oldID, device);
	}

	public void add(final Lease lease)
	{
		mostRecentLease = Math.max(lease.getTimestamp(), mostRecentLease);
		final Device device = getDeviceByMac(lease.getMacAddress());
		final String oldID = device.getID();
		device.update(lease);
		deviceUpdated(oldID, device);
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
					if(device.canRemove())
					{
						removals.add(device);
					}
					else
					{
						Item item = items.get(device.getID());
						if(item != null)
						{
							item.setOld(timestamp);
						}
					}
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
			else
			{
				final Item newItem = new Item(device);
				items.put(device.getID(), newItem);
			}
		}
		else
		{
			final Item oldItem = items.get(oldID);
			final Item newItem = items.get(device.getID());
			if(oldItem != null)
			{
				oldItem.remove(device, device.getTimestamp());
			}
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

	public long getMostRecentLink()
	{
		return Math.max(mostRecentLink, new Date().getTime() - timeout) + 1;
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
}