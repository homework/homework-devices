package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class NoxZoneManager extends DefaultZoneManager
{
	public NoxZoneManager()
	{
		super(new String[] { "Not Allowed", "Requesting Permission", "Internet" });
		internet = 2;
	}

	@Override
	public String getStyleName(Link link)
	{
		int zone = getZone(link);
		if(zone == 0)
		{
			return DevicesClient.resources.style().denied();
		}
		else if(zone == 1)
		{
			return DevicesClient.resources.style().requesting();
		}
		else if(zone == 2)
		{
			return DevicesClient.resources.style().permitted();
		}
		return super.getStyleName(link);
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
		if (link.getState().equals("requesting") || (link.getState().equals("denied") && link.getIPAddress() != null)) { return 1; }
		if (link.getState().equals("permitted")) { return 2; }
		return 0;
	}

	@Override
	public void setZone(final DevicesService service, final Link link, final int zone)
	{
		if (zone == 2)
		{
			service.permit(link.getMacAddress());
		}
		else if (zone == 0)
		{
			service.deny(link.getMacAddress());
		}
	}
}