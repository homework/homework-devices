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
			deviceName = Item.getShortCompanyName(link) + " Device";
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
		setState(State.active);
	}

	public boolean update(final long currentUpdate)
	{
		return false;
	}

	@Override
	public boolean updateState(final Model model)
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