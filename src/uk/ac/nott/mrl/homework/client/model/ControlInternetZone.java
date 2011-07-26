package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

public class ControlInternetZone extends Zone
{
	public ControlInternetZone(int index)
	{
		super(index, "Internet", DevicesClient.resources.webblue());
		this.deviceStyle = DevicesClient.resources.style().device();
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}

	@Override
	public void add(DevicesService service, Link link)
	{
		link.setState("permitted");
		service.permit(link.getMacAddress());
	}
}
