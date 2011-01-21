package uk.ac.nott.mrl.homework.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.LinkListener;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.touch.Touch;
import uk.ac.nott.mrl.homework.client.touch.TouchEvent;
import uk.ac.nott.mrl.homework.client.touch.TouchHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class DevicesPanel extends FlowPanel
{
	public static final native int getWinOffsetX() /*-{
													return $wnd.pageXOffset || 0;
													}-*/;

	public static final native int getWinOffsetY() /*-{
													return $wnd.pageYOffset || 0;
													}-*/;

	static int getZoneWidth()
	{
		return 1000 / Model.zoneManager.getZoneCount();
	}

	private final RequestCallback callback = new RequestCallback()
	{

		@Override
		public void onError(final Request request, final Throwable exception)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void onResponseReceived(final Request request, final Response response)
		{
			try
			{
				String macAddress = response.getText();
				if (macAddress.trim().equals(""))
				{
					macAddress = null;
				}
				if (signalDeviceMac != null)
				{
					if (signalDeviceMac.equals(macAddress)) { return; }
					final Device device = deviceMap.get(signalDeviceMac);
					if (device != null)
					{
						device.setSignalDevice(false);
					}
				}
				else if (macAddress == null) { return; }
				signalDeviceMac = macAddress;
				if (signalDeviceMac != null)
				{
					final Device device = deviceMap.get(signalDeviceMac);
					if (device != null)
					{
						device.setSignalDevice(true);
					}
				}
			}
			catch (final Exception e)
			{
				GWT.log(e.getMessage(), e);
			}
		}
	};

	private final Map<String, Device> deviceMap = new HashMap<String, Device>();
	private Object selected;
	private Device drag;
	private Device popupDevice;
	private final SimplePanel dragLine = new SimplePanel();
	private final PopupPanel popup = new PopupPanel(true);

	// private final AnimatedFloat popupOpacity;

	private final Timer fadeTimer = new Timer()
	{
		@Override
		public void run()
		{
			popup.getElement().getStyle().setOpacity(0);
		}
	};

	private final LinkListener linkListener = new LinkListener()
	{
		@Override
		public void linkAdded(final Link link, final int bandWidthMax)
		{
			final Zone zone = getZone(link);
			if (link.isResource())
			{
				zone.add(link);
			}
			else
			{
				final Device device = new Device(link, bandWidthMax);
				if (link.getMacAddress().equals(signalDeviceMac))
				{
					device.setSignalDevice(true);
				}
				deviceMap.put(link.getMacAddress(), device);
				add(device);

				device.addMouseDownHandler(new MouseDownHandler()
				{
					@Override
					public void onMouseDown(final MouseDownEvent event)
					{
						setDragWidget(device, -event.getRelativeX(device.getElement()));
					}
				});
				device.addClickHandler(new ClickHandler()
				{
					@Override
					public void onClick(final ClickEvent event)
					{
						setPopup(device);
						popup.setPopupPosition(	device.getAbsoluteLeft() + 10,
												device.getAbsoluteTop() + device.getOffsetHeight() - 3);
						popup.getElement().getStyle().setOpacity(1);
						popup.show();
						fadeTimer.schedule(5000);
						// event.preventDefault();
					}
				});
				device.addDoubleClickHandler(new DoubleClickHandler()
				{
					@Override
					public void onDoubleClick(final DoubleClickEvent event)
					{
						popup.hide();
						device.edit(service);
						event.preventDefault();
					}
				});
				device.addTouchStartHandler(new TouchHandler()
				{
					@Override
					public void onTouchEvent(final Device thingy, final TouchEvent event)
					{
						final Touch touch = event.changedTouches().get(0);
						Node node = touch.target();
						if (touch.target().getNodeType() == Node.TEXT_NODE)
						{
							node = node.getParentNode();
						}
						setDragWidget(	device,
										com.google.gwt.dom.client.Element.as(node).getAbsoluteLeft() - touch.pageX());
					}
				});

				zone.incrementDevices(device);
			}
		}

		@Override
		public void linkRemoved(final Link link)
		{
			final Zone zone = getZone(link);
			if (link.isResource())
			{
				zone.remove(link);
			}
			else
			{
				final Device device = deviceMap.get(link.getMacAddress());
				if (device != null)
				{
					remove(device);
					zone.decrementDevices(device);
					deviceMap.remove(link.getMacAddress());
				}
			}
		}

		@Override
		public void linkUpdated(final Link link, final int bandWidthMax)
		{
			final Zone newZone = getZone(link);
			final Device device = deviceMap.get(link.getMacAddress());
			if (link.isResource())
			{
				if (device != null)
				{
					remove(device);
					newZone.decrementDevices(device);
					deviceMap.remove(link.getMacAddress());
				}
				else
				{
					newZone.update(link);
				}
			}
			else if (device == null)
			{
				newZone.remove(link);
				linkAdded(link, bandWidthMax);
			}
			else
			{
				final Zone oldZone = getZone(device.getLink());
				if (newZone != oldZone)
				{
					oldZone.decrementDevices(device);
					newZone.incrementDevices(device);
				}
				device.update(link, bandWidthMax);
				if (device == popupDevice)
				{
					setPopup(device);
				}
			}
		}

		@Override
		public void linkUpdateFinished()
		{
			reflowDevices();
		}
	};

	private int offsetx;

	private int fullHeight;

	private final List<Zone> zones = new ArrayList<Zone>();

	private final DevicesService service;
	private final TrayPanel trayPanel;
	private String signalDeviceMac;

	public DevicesPanel(final DevicesService service)
	{
		this.service = service;

		popup.setStylePrimaryName("popup");
		// popupOpacity = new AnimatedFloat(1, 0.02f, 0, 1)
		// {
		// @Override
		// public void update(final float value)
		// {
		// popup.getElement().getStyle().setOpacity(value);
		// if (value == 0)
		// {
		// popup.hide();
		// }
		// }
		// };

		int left = 0;
		for (final String zoneName : Model.zoneManager.getZones())
		{
			final Zone zoneBar = new Zone(zoneName);
			zoneBar.addClickHandler(new ClickHandler()
			{
				@Override
				public void onClick(final ClickEvent event)
				{
					setSelected(zoneBar);
				}
			});
			zoneBar.getElement().getStyle().setLeft(left, Unit.PX);
			add(zoneBar);
			zones.add(zoneBar);
			left += getZoneWidth();
			zoneBar.addMouseOverHandler(new MouseOverHandler()
			{
				@Override
				public void onMouseOver(final MouseOverEvent event)
				{
					if (drag != null)
					{
						zoneBar.getElement().getStyle().setBackgroundColor("#DDF");
					}
				}
			});
			zoneBar.addMouseOutHandler(new MouseOutHandler()
			{
				@Override
				public void onMouseOut(final MouseOutEvent event)
				{
					if (drag != null)
					{
						zoneBar.getElement().getStyle().setBackgroundColor("transparent");
					}
				}
			});
		}

		dragLine.setStylePrimaryName("deviceLine");
		dragLine.setVisible(false);
		add(dragLine);

		setStylePrimaryName("devicePanel");
		// detailPanel.setStylePrimaryName("detailPanel");

		// window scroll handler
		Window.addWindowScrollHandler(new Window.ScrollHandler()
		{
			@Override
			public void onWindowScroll(final Window.ScrollEvent event)
			{
				updateLayout();
			}
		});

		Window.addResizeHandler(new ResizeHandler()
		{
			@Override
			public void onResize(final ResizeEvent event)
			{
				updateLayout();
			}
		});

		addDomHandler(new MouseMoveHandler()
		{
			@Override
			public void onMouseMove(final MouseMoveEvent event)
			{
				if (drag != null)
				{
					dragDeviceTo(drag, event.getX() + offsetx);
				}
			}
		}, MouseMoveEvent.getType());
		addDomHandler(new MouseUpHandler()
		{
			@Override
			public void onMouseUp(final MouseUpEvent event)
			{
				dragEnd(event.getX());
			}
		}, MouseUpEvent.getType());
		addDomHandler(new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				event.preventDefault();
			}
		}, ClickEvent.getType());

		registerDomTouchEvents();

		trayPanel = new TrayPanel(service);
		add(trayPanel);
	}

	public LinkListener getListener()
	{
		return linkListener;
	}

	public Object getSelected()
	{
		return selected;
	}

	public RequestCallback getTrayDeviceCallback()
	{
		return callback;
	}

	public RequestCallback getTrayModeCallback()
	{
		return trayPanel.getTrayModeCallback();
	}

	protected void onTouchEnd(final TouchEvent event)
	{
		dragEnd(event.changedTouches().get(0).pageX());
	}

	protected void onTouchMove(final TouchEvent event)
	{
		if (drag != null)
		{
			event.preventDefault();
			dragDeviceTo(drag, event.changedTouches().get(0).pageX() + offsetx);
		}
	}

	private void dragDeviceTo(final Device drag, final int left)
	{
		drag.setLeft(left);
		dragLine.getElement().getStyle().setLeft(left - 5, Unit.PX);
	}

	private void dragEnd(final int left)
	{
		if (drag != null)
		{
			int zone = left / getZoneWidth();
			if (zone >= zones.size())
			{
				zone = drag.getZone();
			}
			final Zone zonePanel = getZone(zone);

			drag.setLeft(zonePanel.getElement().getOffsetLeft() + 25);

			if (zone != drag.getZone())
			{
				final Zone oldZone = getZone(drag.getZone());
				oldZone.remove(drag);
				drag.getLink().setZone(zone);
				Model.zoneManager.setZone(service, drag.getLink(), zone);
			}
			zonePanel.incrementDevices(drag);
			drag = null;
			dragLine.setVisible(false);
		}
	}

	private Zone getZone(final int index)
	{
		if (index >= zones.size()) { return zones.get(zones.size() - 1); }
		return zones.get(index);
	}

	private Zone getZone(final Link link)
	{
		return getZone(Model.zoneManager.getZone(link));
	}

	private void reflowDevices()
	{
		int maxDevice = Model.zoneManager.reflowDevices(new ArrayList<Device>(deviceMap.values()));

		maxDevice = Math.max(maxDevice, getElement().getClientHeight());
		updateClientHeight(maxDevice);
	}

	private final native void registerDomTouchEvents()
	/*-{
		var instance = this;
		var element = this.@uk.ac.nott.mrl.homework.client.ui.DevicesPanel::getElement()();

		element.addEventListener("touchend", $entry(function(e){
		        instance.@uk.ac.nott.mrl.homework.client.ui.DevicesPanel::onTouchEnd(Luk/ac/nott/mrl/homework/client/touch/TouchEvent;)(e);
		}), false);

		element.addEventListener("touchmove", $entry(function(e){
		        instance.@uk.ac.nott.mrl.homework.client.ui.DevicesPanel::onTouchMove(Luk/ac/nott/mrl/homework/client/touch/TouchEvent;)(e);
		}), false);
	}-*/;

	private void setDragWidget(final Device device, final int offsetx)
	{
		if (selected != device)
		{
			setSelected(device);
		}
		if (!Model.zoneManager.allowDrag()) { return; }
		drag = device;
		final Zone zone = getZone(drag.getZone());
		zone.decrementDevices(drag);
		dragLine.getElement().getStyle().setLeft((drag.getZone() * getZoneWidth()) + 20, Unit.PX);
		dragLine.setVisible(true);
		this.offsetx = offsetx;
	}

	private void setPopup(final Device device)
	{
		final FlowPanel panel = new FlowPanel();

		if (device.getLink().getCorporation() == null || device.getLink().getCorporation().equals("Unknown"))
		{
			panel.add(new Label("Manufacturer: Unknown"));
		}
		else
		{
			final FlowPanel panel2 = new FlowPanel();
			panel2.add(new InlineLabel("Manufacturer: "));
			final Anchor companySearch = new Anchor(device.getLink().getCorporation(),
					"http://www.google.co.uk/search?q=" + URL.encodeQueryString(device.getLink().getCorporation()),
					"_blank");
			companySearch.addClickHandler(new ClickHandler()
			{
				@Override
				public void onClick(final ClickEvent event)
				{
					service.log("Search Manufacturer", device.getLink().getMacAddress());
				}
			});
			panel2.add(companySearch);
			panel.add(panel2);
		}
		panel.add(new Label("MAC Address: " + device.getLink().getMacAddress()));

		final Anchor renameLink = new Anchor("Rename Device");
		renameLink.setStylePrimaryName("popupLink");
		renameLink.addClickHandler(new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				popup.setVisible(false);
				device.edit(service);
			}
		});
		panel.add(renameLink);
		trayPanel.addTrayLinks(device, panel);

		popup.setWidget(panel);
		popupDevice = device;
	}

	private void setSelected(final Object object)
	{
		GWT.log("Selected " + object, null);
		this.selected = object;
	}

	private void updateClientHeight(final int newHeight)
	{
		if (newHeight != fullHeight)
		{
			fullHeight = newHeight;
		}
	}

	private void updateLayout()
	{
		final int top = getWinOffsetY();
		for (final Zone panel : zones)
		{
			panel.getElement().getStyle().setTop(top, Unit.PX);
		}
	}
}