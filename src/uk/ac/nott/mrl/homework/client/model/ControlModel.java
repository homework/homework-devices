package uk.ac.nott.mrl.homework.client.model;


public class ControlModel extends SimpleModel
{
	public ControlModel()
	{
		super(new Zone[] { new ControlDeniedZone(0), new Zone(1, "Requesting Permission", null),
							new ControlInternetZone(2) });
	}
}
