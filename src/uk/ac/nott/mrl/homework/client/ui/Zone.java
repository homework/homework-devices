package uk.ac.nott.mrl.homework.client.ui;

import java.util.HashMap;
import java.util.Map;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class Zone extends FlowPanel
{
	private final Image image;
	private final Label label;

	private final SimplePanel line = new SimplePanel();

	private final Map<String, Link> resources = new HashMap<String, Link>();
	private int devices = 0;

	private int bandWidthMax = 0;
	private long bandWidthTime = 0;
	private long mostRecent = 0;
	private float bandWidthPercent = 0;

	public Zone(final String zoneName)
	{
		super();
		setStylePrimaryName("zoneBar");
		getElement().getStyle().setWidth(DevicesPanel.getZoneWidth(), Unit.PX);

		final FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("zone");
		panel.getElement().getStyle().setWidth(DevicesPanel.getZoneWidth(), Unit.PX);

		image = new Image();
		updateImage(zoneName);
		panel.add(image);

		label = new Label(zoneName);
		panel.add(label);
		add(panel);

		line.setStylePrimaryName("deviceLine");
		add(line);
		line.setVisible(false);
	}

	public void add(final Link resource)
	{
		resources.put(resource.getMacAddress(), resource);
	}

	public void addClickHandler(final ClickHandler clickHandler)
	{
		if (image != null)
		{
			image.addClickHandler(clickHandler);
		}
		label.addClickHandler(clickHandler);

	}

	public void addMouseOutHandler(final MouseOutHandler handler)
	{
		// addDomHandler(handler, MouseOutEvent.getType());
	}

	public void addMouseOverHandler(final MouseOverHandler handler)
	{
		// addDomHandler(handler, MouseOverEvent.getType());
	}

	public void decrementDevices(final Device device)
	{
		devices--;
		// GWT.log("Device removed from " + getName() + ":" + device + " (" + devices + ")");
		if (devices == 0)
		{
			GWT.log("Set Visible false", null);
			line.setVisible(false);
		}
	}

	public String getName()
	{
		return label.getText();
	}

	public Iterable<Link> getResources()
	{
		return resources.values();
	}

	public void incrementDevices(final Device device)
	{
		devices++;
		// GWT.log("Device added to " + getName() + ":" + device + " (" + devices + ")");
		if (devices == 1)
		{
			GWT.log("Set Visible true", null);
			line.setVisible(true);
		}
	}

	public void remove(final Link resource)
	{
		resources.remove(resource.getMacAddress());
	}

	public void update(final Link link)
	{
		if (link.isResource() && link.getDeviceName().equals("Router"))
		{
			if (link.getByteCount() > bandWidthMax)
			{
				bandWidthMax = link.getByteCount();
				bandWidthTime = (long) link.getTimestamp();
			}
			bandWidthPercent = ((float) link.getByteCount()) / bandWidthMax;
			// GWT.log("Zone update: " + label.getText() + " (" + Model.zoneManager.getZone(link) +
			// ") " + (bandWidthPercent * 100) + "%");
			mostRecent = (long) link.getTimestamp();

			if (mostRecent - bandWidthTime > Model.TIMEOUT)
			{
				bandWidthMax *= Model.DECAY;
			}
			updateImage(label.getText());
		}
	}

	private void updateImage(final String zoneName)
	{
		if (zoneName.equals("Internet") || zoneName.equals("Connected"))
		{
			if (bandWidthPercent >= 0.9)
			{
				image.setResource(DevicesClient.resources.webred());
			}
			else if (bandWidthPercent >= 0.25)
			{
				image.setResource(DevicesClient.resources.webgreen());
			}
			else
			{
				image.setResource(DevicesClient.resources.webblue());
			}
		}
		else if (zoneName.equals("Printer") || zoneName.equals("Shared Resources"))
		{
			image.setResource(DevicesClient.resources.printer());
		}
		else if (zoneName.equals("Network Storage") || zoneName.equals("Personal Devices"))
		{
			image.setResource(DevicesClient.resources.drive());
		}
		else if (zoneName.equals("Everything"))
		{
			image.setResource(DevicesClient.resources.all());
		}
		else if (zoneName.equals("Denied Access"))
		{
			image.setResource(DevicesClient.resources.denied());
		}
		else
		{
			image.setVisible(false);
		}
	}
}
