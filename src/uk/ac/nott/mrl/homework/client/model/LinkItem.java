package uk.ac.nott.mrl.homework.client.model;

public class LinkItem extends Item
{
	private Link link;

	public LinkItem(final Link link, final Zone zone)
	{
		this.link = link;
		setZone(zone);
	}

	@Override
	public String getID()
	{
		return link.getMacAddress();
	}

	public Link getLink()
	{
		return link;
	}

	public String getMacAddress()
	{
		return link.getMacAddress();
	}

	@Override
	public String getName()
	{
		String deviceName = link.getDeviceName();
		if (deviceName == null)
		{
			deviceName = link.getCorporation();
			if (deviceName != null)
			{
				int cut = deviceName.indexOf(' ');
				if (cut != -1)
				{
					deviceName = deviceName.substring(0, cut);
				}

				cut = deviceName.indexOf(',');
				if (cut != -1)
				{
					deviceName = deviceName.substring(0, cut);
				}
				deviceName += " Device";
			}
			else
			{
				deviceName = "Unknown Device";
			}
		}
		return deviceName;
	}

	@Override
	public float getOpacity()
	{
		if (getState() == State.inactive) { return 0.2f; }

		// Opacity by Signal Strength
		return 1.3f + (link.getRssi() / 100);
	}

	@Override
	public void update(final Link link)
	{
		this.link = link;
	}

	public boolean update(final long currentUpdate)
	{
		return false;
	}

	@Override
	public boolean update(final Model model)
	{
		final State newState = model.getState(link);
		if (newState != getState())
		{
			setState(newState);
			return true;
		}
		return false;
	}
}