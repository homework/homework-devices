package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Item;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.model.Zone;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ZonePanel extends FlowPanel
{
	private final Image image;

	private final Label label;
	private final SimplePanel line = new SimplePanel();
	private final Zone zone;

	public ZonePanel(final Model model, final Zone zone)
	{
		super();
		this.zone = zone;

		setStylePrimaryName(DevicesClient.resources.style().zoneBar());
		getElement().getStyle().setWidth(100 / model.getZones().length, Unit.PCT);
		GWT.log("Zone " + zone.getName() + ": " + zone.getIndex() + " left="
				+ (100 / model.getZones().length * zone.getIndex()));
		getElement().getStyle().setLeft((100 / model.getZones().length * zone.getIndex()), Unit.PCT);

		final FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName(DevicesClient.resources.style().zone());
		panel.setWidth("100%");

		image = new Image();
		if (zone.getImage() != null)
		{
			image.setResource(zone.getImage());
		}
		// DefaultModel.zoneManager.updateImage(zone, image, 0);
		panel.add(image);

		label = new Label(zone.getName());
		panel.add(label);
		add(panel);

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

	public String getName()
	{
		return label.getText();
	}

	@Override
	public void add(Widget w)
	{
		super.add(w);
		if(w instanceof Device)
		{
			w.setStyleName(zone.getDeviceStyle());				
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