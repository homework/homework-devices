package uk.ac.nott.mrl.homework.server.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;


public class Item
{
	public static final String getShortCompanyName(final Device device)
	{
		String company = device.getCompany();
		if (company != null)
		{
			int cut = company.indexOf(' ');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}

			cut = company.indexOf(',');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}
			return company;
		}
		return "Unknown";
	}
//	
//	public static final String getTypeID(Device device, Zone zone)
//	{
//		final String company = getShortCompanyName(device).toLowerCase();		
//		return zone.getName() + ":" + company;
//	}
//	

	public enum Change
	{
		added, updated, removed
	}

	@Expose
	private String company;
	@Expose	
	private String name;
	@Expose	
	private String id;
	@Expose	
	private String macAddress;	
	@Expose
	private String ipAddress;
	@Expose	
	private State state = State.unlisted;
	@Expose	
	private Change change = null;
	@Expose	
	private long timestamp;
	@Expose
	private Float rssi = null;
	
	private Map<String, Device> devices = new HashMap<String, Device>();
	
	public Item(Device device)
	{
		id = device.getID();		
		add(device);
	}
	
	public String getID()
	{
		return id;
	}
	
	public long getTimestamp()
	{
		return timestamp;
	}

	public void add(final Device device)
	{
		synchronized (devices)
		{
			devices.put(device.getMacAddress(), device);	
		}
		update(device.getTimestamp());	
	}
	
	public void update(final Device device)
	{
		synchronized (devices)
		{
			devices.put(device.getMacAddress(), device);	
		}
		update(device.getTimestamp());	
	}
	
	private void update(final long timestamp)
	{
		synchronized(devices)
		{
			int count = devices.size();
			if(count == 0)
			{
				name = null;
				company = null;
				change = Change.removed;
				macAddress = null;
				ipAddress = null;
				rssi = null;
			}
			else if(count == 1)
			{
				Device device = devices.values().iterator().next();
				if(device.canBeGrouped())
				{
					name = device.getShortCompanyName() + " Device";
				}
				else
				{
					name = device.getName();
				}
				change = null;
				company = device.getCompany();
				macAddress = device.getMacAddress();
				ipAddress = device.getIPAddress();
				state = device.getState();
				rssi = device.getRssi();
			}
			else
			{
				Device device = devices.values().iterator().next();			
				name = device.getShortCompanyName() + " Devices (" + count + ")";
				company = device.getCompany();
				change = null;
				macAddress = null;
				ipAddress = null;
				rssi = null;
			}
		}
		this.timestamp = Math.max(this.timestamp, timestamp);
	}

	public Iterable<Device> getDevices()
	{
		return devices.values();
	}
	
	public void remove(final Device device, final long timestamp)
	{
		synchronized (devices)
		{
			devices.remove(device.getMacAddress());
		}
		update(timestamp);
	}
	
	public String getCompany()
	{
		return company;
	}

	public String getName()
	{
		return name;
	}
}