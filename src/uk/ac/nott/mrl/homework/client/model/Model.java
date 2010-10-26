package uk.ac.nott.mrl.homework.client.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

public class Model
{
	public static final int TIMEOUT = 360;
	public static final float DECAY = 0.9f;

	private long mostRecent = 0;

	private int bandWidthMax = 0;
	private long bandWidthTime = 0;

	private LinkListener listener;

	private final Map<String, Link> links = new HashMap<String, Link>();
	public static final ZoneManager zoneManager = GWT.create(ZoneManager.class);

	public void add(final LinkListener listener)
	{
		this.listener = listener;
	}

	public Link get(final String mac)
	{
		return links.get(mac);
	}

	public long getMostRecent()
	{
		return mostRecent;
	}

	public void updateLinks(final JsArray<Link> newLinks)
	{
		for (int index = 0; index < newLinks.length(); index++)
		{
			add(newLinks.get(index));
		}

		removeOld();
		listener.linkUpdateFinished();
	}

	private void add(final Link link)
	{
		final Link existing = links.get(link.getMacAddress());
		links.put(link.getMacAddress(), link);

		if (link.getByteCount() > bandWidthMax && !link.isResource() && link.getIPAddress() != null)
		{
			// GWT.log("New Bandwidth Max for " + link.getDeviceName() + " with " +
			// link.getByteCount());
			bandWidthMax = link.getByteCount();
			bandWidthTime = (long) link.getTimestamp();
		}

		if (existing != null)
		{
			listener.linkUpdated(link, bandWidthMax);
		}
		else
		{
			listener.linkAdded(link, bandWidthMax);
		}

		mostRecent = Math.max(mostRecent, (long)link.getTimestamp());
	}

	private void removeOld()
	{
		GWT.log("Most Recent: " + mostRecent);
		if (mostRecent > 0)
		{
			if (mostRecent - bandWidthTime > TIMEOUT)
			{
				bandWidthMax *= DECAY;
			}
			
			int removalTime = 50000;
			if(links.size() > 80)
			{
				removalTime = 10000;
			}
			else if(links.size() > 50)
			{
				removalTime = 20000;
			}
			else if(links.size() > 40)
			{
				removalTime = 30000;
			}
			else if(links.size() > 30)
			{
				removalTime = 40000;
			}			

			final Collection<Link> removals = new HashSet<Link>();
			for (final Link link : links.values())
			{
				final long difference = mostRecent - (long)link.getTimestamp();
				if (difference > removalTime)
				{
					removals.add(link);
				}
				else if (difference > 10000)
				{
					if (link.setOld())
					{
						listener.linkUpdated(link, bandWidthMax);
					}
				}
			}

			for (final Link remove : removals)
			{
				links.remove(remove);
				listener.linkRemoved(remove);
			}
		}
	}
}