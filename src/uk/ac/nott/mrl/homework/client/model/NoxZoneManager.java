package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public class NoxZoneManager implements ZoneManager
{
	@Override
	public boolean allowDrag()
	{
		return true;
	}

	@Override
	public int getZone(final Link link)
	{
		if (link.isResource() && link.getDeviceName().equals("Router")) { return 2; }
		if (!link.isPermitted())
		{
			return 0;
		}
		else if (link.getIPAddress() != null) { return 2; }
		return 1;
	}

	@Override
	public int getZoneCount()
	{
		return 3;
	}

	@Override
	public String[] getZones()
	{
		return new String[] { "Denied Access", "Not Connected", "Internet" };
	}

	@Override
	public void setZone(final DevicesService service, final Link link, final int zone)
	{
		if (zone > 0)
		{
			service.permit(link.getMacAddress());
		}
		else
		{
			service.deny(link.getMacAddress());
		}
	}
}