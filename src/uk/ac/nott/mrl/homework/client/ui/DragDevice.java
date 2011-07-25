package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class DragDevice extends Label
{
	public enum DragState
	{
		dragging, dragInit, waiting,
	}

	private final static int DRAG_DISTANCE = 20;

	private int offsetx;
	private int offsety;

	private int originx;
	private int originy;
	
	private final DevicesService service;
	private DragState dragState = DragState.waiting;
	
	private Link link;
	
	private ZonePanel zone;
	
	private Widget dragWidget;
	
	public DragDevice(DevicesService service)
	{
		super();
		this.service = service;
		setStyleName(DevicesClient.resources.style().dragDevice());
		setVisible(false);
	}

	/**
	 * On mouse down
	 */
	public void setupDrag(final Link link, final Widget widget, final String text, final int clientX, final int clientY)
	{
		if (link == null) { return; }
		if (dragState == DragState.waiting)
		{
			dragState = DragState.dragInit;
			dragWidget = widget;
			this.link = link;
			originx = clientX;
			originy = clientY;
			setText(text);
		}
	}
	
	private void handleDrag(final int clientX, final int clientY)
	{
		if (dragState == DragState.dragInit)
		{
			final int distance = Math.abs(clientX - originx) + Math.abs(clientY - originy);
			if (distance > DRAG_DISTANCE)
			{
				GWT.log("Drag Start");

				dragWidget.removeFromParent();
				
				dragState = DragState.dragging;
				setVisible(true);
				setDragZone(zone);
				
				offsetx = getOffsetWidth() / 2;
				offsety = getOffsetHeight() / 2;
			}
		}

		if (dragState == DragState.dragging)
		{
			getElement().getStyle().setLeft(clientX - offsetx, Unit.PX);
			getElement().getStyle().setTop(clientY - offsety, Unit.PX);
			
		}
	}
	
	public void setDragZone(ZonePanel zone)
	{
		if(this.zone != null)
		{
			this.zone.getElement().getStyle().setBackgroundColor("transparent");
		}
		this.zone = zone;
		if(zone != null)
		{
			if(zone.getZone().canAdd() && dragState == DragState.dragging)
			{
				zone.getElement().getStyle().setBackgroundColor("#DDF");
			}
		}
	}

	private void handleDragEnd()
	{
		if (dragState == DragState.dragging)
		{
			GWT.log("Drag End");
			setVisible(false);

			if(zone != null)
			{
				zone.getZone().add(service, link);				
				setDragZone(null);
			}
		}
		dragState = DragState.waiting;
	}

	public void setupUIElements(final Panel panel)
	{
		panel.add(this);

		panel.addDomHandler(new MouseMoveHandler()
		{
			@Override
			public void onMouseMove(final MouseMoveEvent event)
			{
				handleDrag(event.getClientX(), event.getClientY());
				event.stopPropagation();
			}
		}, MouseMoveEvent.getType());

		panel.addDomHandler(new MouseUpHandler()
		{
			@Override
			public void onMouseUp(final MouseUpEvent event)
			{
				handleDragEnd();
			}
		}, MouseUpEvent.getType());
		
		panel.addDomHandler(new TouchEndHandler()
		{
			@Override
			public void onTouchEnd(final TouchEndEvent event)
			{
				handleDragEnd();				
			}
		}, TouchEndEvent.getType());
		
		panel.addDomHandler(new TouchMoveHandler()
		{
			@Override
			public void onTouchMove(final TouchMoveEvent event)
			{
				handleDrag(event.getChangedTouches().get(0).getClientX(), event.getChangedTouches().get(0).getClientY());
				event.stopPropagation();
			}
		}, TouchMoveEvent.getType());		
	}

	public DragState getState()
	{
		return dragState;
	}	
}