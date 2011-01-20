package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public class NoxZoneManager extends DefaultZoneManager
{
	public NoxZoneManager()
	{
		super(new String[] { "Not Allowed", "Requesting Permission", "Allowed" });
	}
	
	@Override
	public boolean allowDrag()
	{
		return true;
	}

	@Override
	public int getZone(final Link link)
	{
		if (link.isResource() && link.getDeviceName().equals("Router")) { return 2; }
		if(link.isRequestingPermission())
		{
			return 1;
		}
		if (link.isPermitted())
		{
			return 2;
		}
		return 0;
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