package uk.ac.nott.mrl.homework.server.model;


public class Device
{
	private static final Companies companies = new Companies();

	private String corporation;
	private String deviceName;
	private String hostName;
	private String ipAddress;
	private String macAddress;
	private String stateSource;
	private String type;
	private String owner;

	private String state;
	private long timestamp;
	private Float rssi;
	
	private boolean removable = true;

	public Device(String macAddress)
	{
		this.macAddress = macAddress;
	}

	public boolean canBeGrouped()
	{
		if (deviceName != null || ipAddress != null || state != null) { return false; }
		return true;
	}

	public String getType()
	{
		return type;
	}
	
	public Float getRssi()
	{
		return rssi;
	}
	
	public String getOwner()
	{
		return owner;
	}

	public String getStateSource()
	{
		return stateSource;
	}
	
	public String getCompany()
	{
		return corporation;
	}

	public String getDeviceName()
	{
		return deviceName;
	}

	private String getGroupID()
	{
		if(state != null)
		{
			return state + ":" + getShortCompanyName().toLowerCase();			
		}
		return getShortCompanyName().toLowerCase();
	}

	public String getID()
	{
		if (canBeGrouped())
		{
			return getGroupID();
		}
		else
		{
			return macAddress;
		}
	}

	public String getIPAddress()
	{
		return ipAddress;
	}

	public String getMacAddress()
	{
		return macAddress;
	}

	public String getName()
	{
		if (deviceName != null) { return deviceName; }
		if (hostName != null) { return hostName; }
		return getShortCompanyName() + " Device";
	}

	public String getShortCompanyName()
	{
		String company = getCompany();
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

	public String getState()
	{
		return state;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void initCorporation()
	{
		if (corporation == null)
		{
			corporation = companies.getCompany(getMacAddress());
		}
	}

	@Override
	public String toString()
	{
		return timestamp + ": " + macAddress;
	}
	
	public boolean canRemove()
	{
		return removable;
	}
	
	public void update(final Link link)
	{
		timestamp = Math.max(link.getTimestamp(), timestamp);
		rssi = link.getRssi();
	}
	
	public void updateName(final long time, final String name)
	{
		timestamp = Math.max(time, timestamp);
		this.deviceName = name;
	}

	public void updateType(final long time, final String type)
	{
		timestamp = Math.max(time, timestamp);
		this.type = type;
	}
	
	public void updateOwner(final long time, final String owner)
	{
		timestamp = Math.max(time, timestamp);
		this.owner = owner;		
	}
	
	public void update(final NoxStatus status)
	{
		timestamp = Math.max(status.getTimestamp(), timestamp);
		state = status.getState();
		stateSource = status.getSource();
		removable = false;
	}
	
	public void update(final Lease lease)
	{
		ipAddress = lease.getIpAddress();
		hostName = lease.getHostName();
		timestamp = Math.max(lease.getTimestamp(), timestamp);
		removable = false;
	}
}