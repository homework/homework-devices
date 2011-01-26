package uk.ac.nott.mrl.homework.client.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.user.client.ui.Image;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.ui.Device;

public class DefaultZoneManager implements ZoneManager
{
	private final String[] zones;
	protected int internet = 1;

	public DefaultZoneManager()
	{
		zones = new String[] { "Not Connected", "Internet" };
	}

	protected DefaultZoneManager(final String[] zones)
	{
		this.zones = zones;
	}

	@Override
	public boolean allowDrag()
	{
		return false;
	}

	@Override
	public Comparator<Device> getComparator()
	{
		return new Comparator<Device>()
		{
			@Override
			public int compare(final Device o1, final Device o2)
			{
				final int zone1 = o1.getZone();
				final int zone2 = o2.getZone();
				if (zone1 != zone2 && (zone1 == 0 || zone2 == 0)) { return zone2 - zone1; }

				return o1.getLink().getMacAddress().compareTo(o2.getLink().getMacAddress());
			}
		};
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
		return zones.length;
	}

	@Override
	public String[] getZones()
	{
		return zones;
	}

	@Override
	public int reflowDevices(final List<Device> devices)
	{
		Collections.sort(devices, Model.zoneManager.getComparator());

		int top = 15;
		int maxDevice = 0;
		 for (final Device device : devices)
		 {
		 device.setTop(top);
		 top = top + device.getOffsetHeight() + 15;
		
		 }
		 maxDevice = Math.max(maxDevice, top);

//		int zone = -1;
//		for (final Device device : devices)
//		{
//			if (zone != device.getZone())
//			{
//				zone = device.getZone();
//				top = 15;
//			}
//			device.setTop(top);
//			top = top + device.getOffsetHeight() + 15;
//			maxDevice = Math.max(maxDevice, top);
//		}

		return maxDevice;
	}

	@Override
	public void setZone(final DevicesService service, final Link link, final int zone)
	{
		// Do nothing
	}

	@Override
	public void updateImage(int zone, Image image, final float data)
	{
		if(zone == internet)
		{
			if (data >= 0.9)
			{
				image.setResource(DevicesClient.resources.webred());
			}
			else if (data >= 0.25)
			{
				image.setResource(DevicesClient.resources.webgreen());
			}
			else
			{
				image.setResource(DevicesClient.resources.webblue());
			}
		}
	}
}
