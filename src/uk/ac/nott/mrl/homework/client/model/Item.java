package uk.ac.nott.mrl.homework.client.model;

import java.util.Date;

public abstract class Item
{
	public enum State
	{
		active, dead, inactive
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

	public abstract boolean update(final Model model);

	void setState(final State state)
	{
		this.state = state;
	}
}
