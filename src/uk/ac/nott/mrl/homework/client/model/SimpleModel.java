package uk.ac.nott.mrl.homework.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Item.State;
import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

public class SimpleModel implements Model
{
	private static final int DEATH_TIMEOUT = 50000;
	private static final int INACTIVITY_TIMEOUT = 30000;

	private final Map<String, Item> itemMap = new HashMap<String, Item>();

	private final Map<String, Item> items = new HashMap<String, Item>();

	private long lastUpdated = 0;

	private ItemListener listener;

	private final Zone[] zones;

	public SimpleModel()
	{
		this(new Zone[] { new Zone(0, "Not Connected", null),
							new Zone(1, "Internet", DevicesClient.resources.webblue()) });
	}

	protected SimpleModel(final Zone[] zones)
	{
		this.zones = zones;
	}

	@Override
	public void addListener(final ItemListener listener)
	{
		this.listener = listener;
	}

	@Override
	public boolean canBeGrouped(final Link link)
	{
		if (link.getDeviceName() != null) { return false; }
		if (getZone(link) != 0) { return false; }
		return true;
	}

	@Override
	public Comparator<Device> getComparator()
	{
		return new Comparator<Device>()
		{
			@Override
			public int compare(final Device o1, final Device o2)
			{
				final int zone1 = o1.getItem().getZone().getIndex();
				final int zone2 = o2.getItem().getZone().getIndex();
				if (zone1 != zone2 && (zone1 == 0 || zone2 == 0)) { return zone2 - zone1; }

				return o1.getItem().getID().compareTo(o2.getItem().getID());
			}
		};
	}

	@Override
	public long getLastUpdated()
	{
		return lastUpdated;
	}

	@Override
	public String getName(final Link link)
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
	public State getState(final Link link)
	{
		final long difference = lastUpdated - (long) link.getTimestamp();
		if (difference > DEATH_TIMEOUT)
		{
			return State.dead;
		}
		else if (difference > INACTIVITY_TIMEOUT) { return State.inactive; }
		return State.active;
	}

	@Override
	public int getZone(final Link link)
	{
		if (link.getIPAddress() != null) { return 1; }
		return 0;
	}

	@Override
	public Zone[] getZones()
	{
		return zones;
	}

	public void removeItem(final Item item)
	{
		items.remove(item.getID());
		if (item instanceof LinkListItem)
		{
			for (final Link link : ((LinkListItem) item).getLinks())
			{
				itemMap.remove(link);
			}
		}
		else if (item instanceof LinkItem)
		{
			itemMap.remove(((LinkItem) item).getLink().getMacAddress());
		}
	}

	@Override
	public void removeLink(final Link link)
	{
		itemMap.remove(link.getMacAddress());
	}
	
	@Override
	public void updateLinks(final JsArray<Link> newLinks)
	{
		for (int index = 0; index < newLinks.length(); index++)
		{
			try
			{
				final Link link = newLinks.get(index);
				final Item item = getItem(link);
				if (item.getState() == State.added)
				{
					itemMap.put(link.getMacAddress(), item);
					items.put(item.getID(), item);
				}
			}
			catch (final Exception e)
			{
				GWT.log(e.getMessage(), e);
			}
		}

		GWT.log("Last Updated: " + lastUpdated);

		try
		{
			fireEvents();
			listener.itemUpdateFinished();
		}
		catch (final Exception e)
		{
			GWT.log(e.getMessage(), e);
		}
	}

	private void fireEvents()
	{
		// GWT.log("Last Updated: " + lastUpdated);
		if (lastUpdated > 0)
		{
			final Collection<Item> removals = new ArrayList<Item>();
			for (final Item item : items.values())
			{
				if (item.getState() == State.added)
				{
					listener.itemAdded(item);
					item.setState(State.active);
				}
				else if (item.getState() == State.updated)
				{
					listener.itemUpdated(item);
					item.setState(State.active);
				}
				else if (item.updateState(this))
				{
					if (item.getState() == State.dead)
					{
						removals.add(item);
					}
					else
					{
						listener.itemUpdated(item);
					}
				}
			}

			for (final Item remove : removals)
			{
				listener.itemRemoved(remove);
				removeItem(remove);
			}
		}
	}

	private Item getItem(final Link link)
	{
		lastUpdated = Math.max(lastUpdated, (long) link.getTimestamp());

		final Item existing = itemMap.get(link.getMacAddress());
		if (existing != null)
		{
			if (canBeGrouped(link))
			{
				existing.update(link);
				if (existing instanceof LinkListItem)
				{
					existing.setState(State.updated);
					return existing;
				}
			}
			else
			{
				if (existing instanceof LinkItem)
				{
					existing.update(link);
					final Zone zone = getZones()[getZone(link)];
					existing.setZone(zone);
					existing.setState(State.updated);
					return existing;
				}
				else if (existing instanceof LinkListItem)
				{
					final LinkListItem listItem = (LinkListItem) existing;
					listItem.remove(link);
					listItem.setState(State.updated);
				}
			}
		}

		final Zone zone = getZones()[getZone(link)];
		if (canBeGrouped(link))
		{
			final Item listItem = items.get(Item.getTypeID(link, zone));
			if (listItem != null && listItem instanceof LinkListItem)
			{
				((LinkListItem) listItem).add(link);
				listItem.setState(State.updated);
				return listItem;
			}
			else
			{
				final LinkListItem item = new LinkListItem(zone, Item.getShortCompanyName(link));
				item.add(link);
				item.setState(State.added);
				return item;
			}
		}
		else
		{
			final LinkItem item = new LinkItem(link, zone);
			item.setState(State.added);
			return item;
		}
	}

	@Override
	public void updateLink(Link link)
	{
		final Item item = getItem(link);
		if (item.getState() == State.added)
		{
			itemMap.put(link.getMacAddress(), item);
			items.put(item.getID(), item);
			listener.itemAdded(item);
		}
		else if(item.getState() == State.updated)
		{
			listener.itemUpdated(item);
		}
	}
}