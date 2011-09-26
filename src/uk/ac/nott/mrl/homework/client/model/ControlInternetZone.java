package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlInternetZone extends Zone
{
	public ControlInternetZone(final int index)
	{
		super(index, "Permitted", DevicesClient.resources.webblue());
	}

	@Override
	public void add(final DevicesService service, final String macAddress)
	{
		service.getModel().update(macAddress, "permit");
		service.permit(macAddress);
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}
}
