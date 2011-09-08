package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.model.Zone;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ZonePanel extends FlowPanel
{
	private final SimplePanel line = new SimplePanel();
	private final Zone zone;

	public ZonePanel(final Model model, final Zone zone)
	{
		super();
		this.zone = zone;

		setStylePrimaryName(DevicesClient.resources.style().zoneBar());
		getElement().getStyle().setWidth(100 / model.getZones().length, Unit.PCT);

		line.setStylePrimaryName(DevicesClient.resources.style().deviceLine());
		add(line);
		line.setVisible(false);
	}

	public void addMouseOutHandler(final MouseOutHandler handler)
	{
		addDomHandler(handler, MouseOutEvent.getType());
	}

	public void addMouseOverHandler(final MouseOverHandler handler)
	{
		addDomHandler(handler, MouseOverEvent.getType());
	}

	@Override
	public void add(Widget w)
	{
		super.add(w);
		if(w instanceof Device)
		{
			Device device = (Device)w;
			w.setStyleName(zone.getDeviceStyle(device.getItem()));				
		}
		update();
	}

	@Override
	public boolean remove(Widget w)
	{
		boolean result = super.remove(w);
		update();
		return result;
	}

	public void update()
	{
		boolean visible = false;
		for(Widget widget: getChildren())
		{
			if(widget instanceof Device)
			{
				visible = true;;
				break;
			}
		}
		
		line.setVisible(visible);
	}	

	public Zone getZone()
	{
		return zone;		
	}
}