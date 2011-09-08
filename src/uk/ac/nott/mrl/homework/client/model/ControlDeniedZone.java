package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlDeniedZone extends Zone
{
	public ControlDeniedZone(int index)
	{
		super(index, "Not Permitted", DevicesClient.resources.denied());
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}

	@Override
	public void add(DevicesService service, String macAddress)
	{	
		service.getModel().update(macAddress, "denied");
		service.deny(macAddress);
	}
}
