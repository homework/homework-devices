package uk.ac.nott.mrl.homework.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinkListItem extends Item
{
	private String company;

	private Map<String, Link> links = new HashMap<String, Link>();

	public LinkListItem(final Zone zone, final String company)
	{
		setZone(zone);
		this.company = company;
	}

	public void add(final Link link)
	{
		links.put(link.getMacAddress(), link);
	}

	@Override
	public String getID()
	{
		return getZone().getName() + ":" + company.toLowerCase();
	}

	@Override
	public String getName()
	{
		if (links.size() == 1) { return company + " Device"; }
		return company + " Devices (" + links.size() + ")";
	}
	
	public int getSize()
	{
		return links.size();
	}
	
	public Collection<Link> getLinks()
	{
		return links.values();
	}

	@Override
	public float getOpacity()
	{
		if (getState() == State.inactive) { return 0.2f; }

		return 1f;
	}

	@Override
	public void update(final Link link)
	{
		links.put(link.getMacAddress(), link);
		setState(State.active);		
	}

	@Override
	public boolean updateState(final Model model)
	{
		State bestState = State.dead;
		final Collection<Link> removals = new ArrayList<Link>();
		for (final Link link : links.values())
		{
			final State linkState = model.getState(link);
			if (linkState == State.active)
			{
				bestState = State.active;
			}
			else if (linkState == State.inactive && bestState == State.dead)
			{
				bestState = State.inactive;
			}
			else if (linkState == State.dead)
			{
				removals.add(link);
			}
		}

		for (final Link link : removals)
		{
			links.remove(link.getMacAddress());
			model.removeLink(link);
		}

		if (bestState != getState())
		{
			setState(bestState);
			return true;
		}
		return !removals.isEmpty() || getState() == State.dead;
	}

	public void remove(Link link)
	{
		links.remove(link.getMacAddress());
	}
}