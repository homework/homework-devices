package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesClient;


public class ControlModel extends SimpleModel
{
	public ControlModel()
	{
		super(new Zone[] { new ControlDeniedZone(0), new Zone(1, "Requesting Permission", DevicesClient.resources.requesting()),
							new ControlInternetZone(2) });
	}

	@Override
	public int getZone(Link link)
	{
		if(link.getState().equals("permitted"))
		{
			return 2;
		}
		else if (link.getIPAddress() != null && link.getState().equals("unlisted")) { return 1; }
		return 0;
	}
}
