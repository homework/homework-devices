package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;

public class Zone
{
	private ImageResource image;
	private int index;
	private String name;

	public Zone(final int index, final String name, final ImageResource image)
	{
		this.image = image;
		this.name = name;
		this.index = index;
	}

	public void add(final DevicesService service, final Item item)
	{

	}

	public boolean canAdd()
	{
		return false;
	}

	public String getDeviceStyle(final Item item)
	{
		if ("deny".equals(item.getState()) || "blacklist".equals(item.getState()))
		{
			return DevicesClient.resources.style().deniedDevice();
		}
		else if ("add".equals(item.getLeaseAction()))
		{
			if ("permit".equals(item.getState()) || "whitelist".equals(item.getState()))
			{
				return DevicesClient.resources.style().device();
			}
			else
			{
				GWT.log("Style = requesting");
				return DevicesClient.resources.style().requestingDevice();
			}
		}
		return DevicesClient.resources.style().unlistedDevice();
	}

	public ImageResource getImage()
	{
		return image;
	}

	public int getIndex()
	{
		return index;
	}

	public String getName()
	{
		return name;
	}
}
