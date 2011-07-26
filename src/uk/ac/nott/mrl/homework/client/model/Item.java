package uk.ac.nott.mrl.homework.client.model;

import java.util.Date;

public abstract class Item
{
	public static final String getShortCompanyName(final Link link)
	{
		String company = link.getCorporation();
		if (company != null)
		{
			int cut = company.indexOf(' ');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}

			cut = company.indexOf(',');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}
			return company;
		}
		return "Unknown";
	}
	
	public static final String getTypeID(Link link, Zone zone)
	{
		final String company = getShortCompanyName(link).toLowerCase();		
		return zone.getName() + ":" + company;
	}
	
	public enum State
	{
		added, updated, active, dead, inactive
	}

	private Date lastUpdated;
	private State state;
	private Zone zone;

	public abstract String getID();

	public Date getLastUpdate()
	{
		return lastUpdated;
	}

	public abstract String getName();

	// public abstract boolean isResource();

	public abstract float getOpacity();

	public State getState()
	{
		return state;
	}

	public Zone getZone()
	{
		return zone;
	}

	public boolean isResource()
	{
		return false;
	}

	public void setLastUpdate()
	{
		this.lastUpdated = new Date();
	}

	// public abstract void addLink(final Link link);

	public void setZone(final Zone zone)
	{
		this.zone = zone;
	}

	public abstract void update(Link link);

	public abstract boolean updateState(final Model model);

	void setState(final State state)
	{
		this.state = state;
	}
}
