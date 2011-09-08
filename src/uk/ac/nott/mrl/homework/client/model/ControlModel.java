package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;

public class ControlModel extends SimpleModel
{
	public ControlModel()
	{
		super(new Zone[] { new ControlDeniedZone(0),
							new Zone(1, "Requesting Permission", DevicesClient.resources.requesting()),
							new ControlInternetZone(2) });
	}

	@Override
	public int getZone(final Item item)
	{
		if ("permitted".equals(item.getState()))
		{
			return 2;
		}
		else if (item.getIPAddress() != null && item.getState().equals("unlisted")) { return 1; }
		return 0;
	}
}
