package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public class SimpleZoneManager implements ZoneManager
{
	@Override
	public boolean allowDrag()
	{
		return false;
	}

	@Override
	public int getZone(final Link link)
	{
		if (link.isResource() && link.getDeviceName().equals("Router")) { return 2; }
		if (link.getIPAddress() != null) { return 1; }
		return 0;
	}

	@Override
	public int getZoneCount()
	{
		return 2;
	}

	@Override
	public String[] getZones()
	{
		return new String[] { "Not Connected", "Internet" };
	}

	@Override
	public void setZone(final DevicesService service, final Link link, final int zone)
	{
		// Do nothing
	}
}
