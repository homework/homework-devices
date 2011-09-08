package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlInternetZone extends Zone
{
	public ControlInternetZone(int index)
	{
		super(index, "Permitted", DevicesClient.resources.webblue());
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}

	@Override
	public void add(DevicesService service, String macAddress)
	{
		service.getModel().update(macAddress, "permitted");		
		service.permit(macAddress);
	}
}
