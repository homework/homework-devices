package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

public class SimpleModel implements Model
{
	private final Map<String, Item> items = new HashMap<String, Item>();

	private double lastUpdated = 0;

	private ItemListener listener;

	private final Zone[] zones;
	private RequestCallback callback;

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
	public RequestCallback getCallback()
	{
		callback = new RequestCallback()
		{
			private final native JsArray<Item> getItems(final String json)
			/*-{
				return eval('(' + json + ')');
			}-*/;

			@Override
			public void onError(final Request request, final Throwable exception)
			{
				GWT.log(exception.getMessage(), exception);
			}

			@Override
			public void onResponseReceived(final Request request, final Response response)
			{
				GWT.log("Response " + response.getStatusCode() + ": " + response.getText());

				if (200 == response.getStatusCode() && !response.getText().trim().equals(""))
				{
					try
					{
						// update(getItems(DevicesClient.resources.testlinks().getText()));
						update(getItems(response.getText()));
					}
					catch (final Exception e)
					{
						GWT.log(response.getText(), e);
					}
				}
			}

			// private final native JsArray<Link> getLinks(final String json)
			// /*-{
			// return eval('(' + json + ')');
			// }-*/;
		};
		return callback;
	}

	@Override
	public Comparator<Device> getComparator()
	{
		return new Comparator<Device>()
		{
			@Override
			public int compare(final Device o1, final Device o2)
			{
				try
				{
					return o1.getSortString().compareTo(o2.getSortString());
				}
				catch (final Exception e)
				{
					GWT.log(e.getMessage(), e);
				}
				return 0;
			}
		};
	}

	@Override
	public double getLastUpdated()
	{
		return lastUpdated;
	}

	@Override
	public int getZone(final Item item)
	{
		if ("deny".equals(item.getState()) || "blacklist".equals(item.getState()))
		{
			return 0;
		}
		else if ("permit".equals(item.getState()) || "whitelist".equals(item.getState())) { return 1; }
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
		// if (item instanceof LinkListItem)
		// {
		// for (final Link link : ((LinkListItem) item).getLinks())
		// {
		// itemMap.remove(link);
		// }
		// }
		// else if (item instanceof LinkItem)
		// {
		// itemMap.remove(((LinkItem) item).getLink().getMacAddress());
		// }
	}

	@Override
	public void update(final JsArray<Item> itemList)
	{
		for (int index = 0; index < itemList.length(); index++)
		{
			final Item item = itemList.get(index);
			if ("removed".equals(item.getChange()))
			{
				items.remove(item.getID());
				listener.itemRemoved(item);
			}
			else
			{
				final Item existingItem = items.get(item.getID());
				items.put(item.getID(), item);
				if (existingItem == null)
				{
					listener.itemAdded(item);
				}
				else
				{
					listener.itemUpdated(item);
				}
			}
			lastUpdated = Math.max(lastUpdated, item.getTimestamp());
		}
		listener.itemUpdateFinished();
	}

	@Override
	public void update(final String id, final String state)
	{
		final Item item = items.get(id);
		if (item != null)
		{
			item.setState(state);
			listener.itemUpdated(item);
			listener.itemUpdateFinished();
		}
	}
}