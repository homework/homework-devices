package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

import com.google.gwt.resources.client.ImageResource;

public class Zone
{
	private ImageResource image;
	private int index;
	private String name;
	protected String deviceStyle;
	

	public Zone(final int index, final String name, final ImageResource image)
	{
		this.image = image;
		this.name = name;
		this.index = index;
		this.deviceStyle = DevicesClient.resources.style().deniedDevice();
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
	
	public String getDeviceStyle()
	{
		return deviceStyle;
	}
	
	public boolean canAdd()
	{
		return false;
	}
	
	public void add(DevicesService service, Link link)
	{
		
	}
}
