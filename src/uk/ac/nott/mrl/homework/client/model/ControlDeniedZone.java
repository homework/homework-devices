package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlDeniedZone extends Zone
{
	public ControlDeniedZone(int index)
	{
		super(index, "Not Connected", null);
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}

	@Override
	public void add(DevicesService service, Link link)
	{
		service.deny(link.getMacAddress());
	}
}
