package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlDeniedZone extends Zone
{
	public ControlDeniedZone(final int index)
	{
		super(index, "Not Permitted", DevicesClient.resources.denied());
	}

	@Override
	public void add(final DevicesService service, final Item item)
	{
		service.getModel().update(item.getMacAddress(), "deny");
		service.deny(item.getMacAddress());
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}
}
