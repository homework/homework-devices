package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public class NoxZoneManager extends DefaultZoneManager
{
	public NoxZoneManager()
	{
		super(new String[] { "Not Allowed", "Requesting Permission", "Allowed Internet" });
		internet = 2;
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
		if (link.getState().equals("requesting")) { return 1; }
		if (link.getState().equals("permitted")) { return 2; }
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