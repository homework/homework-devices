package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Item;
import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class Device extends FlowPanel
{
	private boolean init = false;
	
	private Item item;

	private final Label text = new Label();

	private String sortString;

	private final Model model;
	
	private static final Transform transform = GWT.create(Transform.class);

	public Device(final Model model, final Item item)
	{
		this.model = model;
		this.item = item;

		setStyleName(DevicesClient.resources.style().device());

		text.setStyleName(DevicesClient.resources.style().deviceName());
		text.setText(item.getName());
		add(text);
		// updateStyle(null);

		setLeft(25);
		update(item);
	}
	
	public String getSortString()
	{
		if(sortString == null)
		{
			String mark = "d";
			if("old".equals(item.getChange()))
			{
				mark = "c";
			}
			else if("del".equals(item.getLeaseAction()))
			{
				mark = "b";
			}
			else if(item.getState() != null || item.getIPAddress() != null)
			{
				mark = "a";
			}
			sortString = model.getZone(item) + mark + item.getName().toLowerCase();
		}
		return sortString;
	}

	public void addClickHandler(final ClickHandler handler)
	{
		addDomHandler(handler, ClickEvent.getType());
	}

	public void addDoubleClickHandler(final DoubleClickHandler handler)
	{
		addDomHandler(handler, DoubleClickEvent.getType());
	}

	public void addMouseDownHandler(final MouseDownHandler handler)
	{
		addDomHandler(handler, MouseDownEvent.getType());
	}

	public void addTouchStartHandler(final TouchStartHandler handler)
	{
		addDomHandler(handler, TouchStartEvent.getType());
	}

	public Item getItem()
	{
		return item;
	}

	public void setLeft(final int left)
	{
		getElement().getStyle().setLeft(left, Unit.PX);
	}

	public void setSignalDevice(final boolean signal)
	{
		// isSignalDevice = signal;
		// updateStyle(item);
	}

	public void setTop(final int top)
	{
		if(!init)
		{
			init = true;
		}
		else
		{
			addStyleName(DevicesClient.resources.style().deviceAnim());			
		}
		transform.translateY(getElement(), top);
	}

	@Override
	public String toString()
	{
		return text.getText();
	}
	
	public void setOpacity(final double opacity)
	{
		if(opacity > 1)
		{
			setOpacity(1);
		}
		else
		{
			getElement().getStyle().setOpacity(opacity);
		}
	}

	public void update(final Item item)
	{
		sortString = null;
		this.item = item;		
		if (!text.getText().equals(item.getName()))
		{
			text.setText(item.getName());
		}

		final ZonePanel zone = (ZonePanel) getParent();
		if (zone != null)
		{
			setStyleName(zone.getZone().getDeviceStyle(item));
		}		
		
		if("old".equals(item.getChange()))
		{
			setOpacity(0.2);
		}
		else
		{
			setOpacity(item.getOpacity());
		}
	}

	public String getStateSource()
	{
		return item.getStateSource();
	}
}
