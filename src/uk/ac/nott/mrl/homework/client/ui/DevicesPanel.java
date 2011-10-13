package uk.ac.nott.mrl.homework.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Item;
import uk.ac.nott.mrl.homework.client.model.ItemListener;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.model.Zone;
import uk.ac.nott.mrl.homework.client.ui.DragDevice.DragState;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class DevicesPanel extends FlowPanel
{
	public static final native int getWinOffsetX() /*-{
													return $wnd.pageXOffset || 0;
													}-*/;

	public static final native int getWinOffsetY() /*-{
													return $wnd.pageYOffset || 0;
													}-*/;

	// static int getZoneWidth()
	// {
	// return 1000 / AbstractModel.zoneManager.getZoneCount();
	// }

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

	private final DragDevice dragDevice;

	private final Timer fadeTimer = new Timer()
	{
		@Override
		public void run()
		{
			popup.getElement().getStyle().setOpacity(0);
			popup.getElement().getStyle().setVisibility(Visibility.HIDDEN);
		}
	};
	private int fullHeight;

	// private final AnimatedFloat popupOpacity;

	private final Model model;

	private int offsetx;

	private final PopupPanel popup = new PopupPanel(true);

	private Device popupDevice;

	private final Timer removalTimer = new Timer()
	{
		@Override
		public void run()
		{
			for(Device device: removed)
			{
				device.removeFromParent();
			}		
		}
	};
	
	private Object selected;

	private final DevicesService service;
	private String signalDeviceMac;
	private final TrayPanel trayPanel;

	private final List<ZonePanel> zones = new ArrayList<ZonePanel>();
	private final List<ZoneDetail> zoneDetails = new ArrayList<ZoneDetail>();
	private final List<Device> removed = new ArrayList<Device>();

	public DevicesPanel(final DevicesService service)
	{
		this.service = service;
		dragDevice = new DragDevice(service);
		this.model = service.getModel();
		this.model.addListener(new ItemListener()
		{
			@Override
			public void itemAdded(final Item item)
			{
				// GWT.log("Item Added: " + item.getName());

				final Device device = new Device(model, item);
				if (item.getID().equals(signalDeviceMac))
				{
					device.setSignalDevice(true);
				}
				deviceMap.put(item.getID(), device);
				final ZonePanel zone = zones.get(model.getZone(item));
				zone.add(device);
				// add(device);

				device.addMouseDownHandler(new MouseDownHandler()
				{
					@Override
					public void onMouseDown(final MouseDownEvent event)
					{
						dragDevice.setupDrag(	device.getItem(), device, device.toString(),
												event.getClientX(), event.getClientY(), device.getStateSource());
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
						popup.getElement().getStyle().setOpacity(1);
						popup.getElement().getStyle().setVisibility(Visibility.VISIBLE);
						fadeTimer.schedule(10000);
					}
				});
				device.addDoubleClickHandler(new DoubleClickHandler()
				{
					@Override
					public void onDoubleClick(final DoubleClickEvent event)
					{
						popup.hide();
						if(device.getItem().getIPAddress() != null)
						{
							device.edit(service);
						}
						event.preventDefault();
					}
				});
				device.addTouchStartHandler(new TouchStartHandler()
				{
					@Override
					public void onTouchStart(final TouchStartEvent event)
					{
						final Touch touch = event.getChangedTouches().get(0);
						dragDevice.setupDrag(	device.getItem(), device, device.toString(),
												touch.getClientX(), touch.getClientY(), device.getStateSource());
					}
				});
			}

			@Override
			public void itemRemoved(final Item item)
			{
				// GWT.log("Item Removed: " + item.getID());
				final Device device = deviceMap.get(item.getID());
				if (device != null)
				{
					deviceMap.remove(item.getID());
					removed.add(device);
					device.getElement().getStyle().setOpacity(0);
					device.getElement().getStyle().setVisibility(Visibility.HIDDEN);
				}
			}

			@Override
			public void itemUpdated(final Item item)
			{
				// GWT.log("Item Updated: " + item.getName());
				final ZonePanel newZone = zones.get(model.getZone(item));
				final Device device = deviceMap.get(item.getID());
				if (device == null)
				{
					// newZone.remove(item);
					itemAdded(item);
				}
				else
				{
					final ZonePanel oldZone = (ZonePanel) device.getParent();
					if (newZone != oldZone)
					{
						if (oldZone != null)
						{
							oldZone.remove(device);
						}
						newZone.add(device);
					}
					device.update(item);
					//if (device == popupDevice)
					//{
					//		setPopup(device);
					//}
				}
			}

			@Override
			public void itemUpdateFinished()
			{
				if(!removed.isEmpty())
				{
					removalTimer.schedule(2000);
				}
				reflowDevices();
			}
		});

		popup.setStylePrimaryName(DevicesClient.resources.style().popup());

		for (final Zone zone : model.getZones())
		{
			final ZonePanel zonePanel = new ZonePanel(model, zone);

			final ZoneDetail detail = new ZoneDetail(model, zone);

			add(detail);
			add(zonePanel);
			zones.add(zonePanel);
			zoneDetails.add(detail);
		}

		setStylePrimaryName(DevicesClient.resources.style().devicePanel());

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

		dragDevice.setupUIElements(this);

		addDomHandler(new MouseMoveHandler()
		{
			@Override
			public void onMouseMove(final MouseMoveEvent event)
			{
				for (final ZonePanel zone : zones)
				{
					final int relX = event.getRelativeX(zone.getElement());
					final int relY = event.getRelativeY(zone.getElement());
					if (relX > 0 && relX < zone.getOffsetWidth() && relY > 0 && relY < zone.getOffsetHeight())
					{
						dragDevice.setDragZone(zone);
					}
				}

				dragDevice.handleDrag(event.getRelativeX(getElement()), event.getRelativeY(getElement()));
			}
		}, MouseMoveEvent.getType());

		RootPanel.get().addDomHandler(new TouchMoveHandler()
		{
			@Override
			public void onTouchMove(final TouchMoveEvent event)
			{
				for (final ZonePanel zone : zones)
				{
					final int relX = event.getChangedTouches().get(0).getRelativeX(zone.getElement());
					final int relY = event.getChangedTouches().get(0).getRelativeY(zone.getElement());
					if (relX > 0 && relX < zone.getOffsetWidth() && relY > 0 && relY < zone.getOffsetHeight())
					{
						dragDevice.setDragZone(zone);
					}
				}

				if (dragDevice.getState() != DragState.waiting)
				{
					event.stopPropagation();
					event.preventDefault();

					dragDevice.handleDrag(	event.getChangedTouches().get(0).getClientX(),
											event.getChangedTouches().get(0).getClientY());
				}
			}
		}, TouchMoveEvent.getType());

		// addDomHandler(new TouchMoveHandler()
		// {
		// @Override
		// public void onTouchMove(TouchMoveEvent event)
		// {
		// for(ZonePanel zone: zones)
		// {
		// int relX = event.getChangedTouches().get(0).getRelativeX(zone.getElement());
		// int relY = event.getChangedTouches().get(0).getRelativeY(zone.getElement());
		// if(relX > 0 && relX < zone.getOffsetWidth() && relY > 0 && relY < zone.getOffsetWidth())
		// {
		// dragDevice.setDragZone(zone);
		// }
		// }
		// zoneDetails.get(0).setText("T" + event.getChangedTouches().length() + ": " +
		// event.getChangedTouches().get(0).getClientX() +", " +
		// event.getChangedTouches().get(0).getClientY());
		// if(dragDevice.getState() != DragState.waiting)
		// {
		// event.stopPropagation();
		// event.preventDefault();
		//
		// dragDevice.handleDrag(event.getChangedTouches().get(0).getClientX(),
		// event.getChangedTouches().get(0).getClientY());
		// }
		// }
		// }, TouchMoveEvent.getType());

		addDomHandler(new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				event.preventDefault();
			}
		}, ClickEvent.getType());

		trayPanel = new TrayPanel(service);
		add(trayPanel);
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

	private ZonePanel getZone(final int index)
	{
		if (index >= zones.size()) { return zones.get(zones.size() - 1); }
		return zones.get(index);
	}

	private int getZoneWidth()
	{
		return 100;
	}

	private void reflowDevices()
	{
		final List<Device> devices = new ArrayList<Device>(deviceMap.values());
		Collections.sort(devices, model.getComparator());

		int top = 15;
		int maxDevice = 0;
		char currentZone = 0;
		for (final Device device : devices)
		{
			if (currentZone != device.getSortString().charAt(0))
			{
				currentZone = device.getSortString().charAt(0);
				top = 15;
			}

			device.setTop(top);
			top += device.getOffsetHeight() + 15;
			maxDevice = Math.max(maxDevice, top);
		}

		maxDevice = Math.max(maxDevice, getElement().getClientHeight());
		updateClientHeight(maxDevice);
	}

	private void setPopup(final Device device)
	{
		final FlowPanel panel = new FlowPanel();

		final Item item = device.getItem();

		if(item.getOwner() != null)
		{
			panel.add(new Label("Owner: " + item.getOwner()));
		}
		
		if(item.getType() != null)
		{
			panel.add(new Label("Device Type: " + item.getType()));
		}
		
		if(item.getStateSource() != null && !item.getStateSource().toLowerCase().equals("user") && item.getState() != null)
		{
			if(item.getState().toLowerCase().equals("deny") || item.getState().toLowerCase().equals("blacklist"))
			{
				panel.add(new Label("This Device has been denied by a policy. Check your policies if you want to allow it."));				
			}
			else
			{
				panel.add(new Label("This Device has been permitted by a policy. Check your policies if you want to deny it."));
			}
		}
			
		if (item.getCompany() == null || item.getCompany().equals("Unknown"))
		{
			panel.add(new Label("Manufacturer: Unknown"));
		}
		else
		{
			final FlowPanel panel2 = new FlowPanel();
			panel2.add(new InlineLabel("Manufacturer: "));
			final Anchor companySearch = new Anchor(item.getCompany(), "http://www.google.co.uk/search?q="
					+ URL.encodeQueryString(item.getCompany()), "_blank");
			companySearch.addClickHandler(new ClickHandler()
			{
				@Override
				public void onClick(final ClickEvent event)
				{
					service.log("Search Manufacturer", item.getCompany());
				}
			});
			panel2.add(companySearch);
			panel.add(panel2);
		}

		if (item.getMacAddress() != null)
		{
			if (item.getIPAddress() != null && item.getState() == null)
			{
				final Label label = new Label(
						"This machine is requesting permission to use your network. Drag it to the right to allow it or to the left to deny it access.");
				label.addStyleName(DevicesClient.resources.style().warning());
				panel.add(label);
			}

			panel.add(new Label("MAC Address: " + item.getMacAddress()));

//			final Anchor renameLink = new Anchor("Rename Device");
//			renameLink.addClickHandler(new ClickHandler()
//			{
//				@Override
//				public void onClick(final ClickEvent event)
//				{
//					popup.setVisible(false);
//					device.edit(service);
//				}
//			});
//			panel.add(renameLink);
			
			if(item.getIPAddress() != null)
			{
				Anchor editLink = new Anchor("Edit Details");
				editLink.setStylePrimaryName(DevicesClient.resources.style().popupLink());				
				editLink.addClickHandler(new ClickHandler()
				{	
					@Override
					public void onClick(ClickEvent event)
					{
						final PopupPanel popup = new PopupPanel(false, true);
						popup.setWidget(new DeviceMetadataDialog(item, service)
						{
							@Override
							protected void cancel()
							{
								popup.hide();
							}
							
							@Override
							protected void accept()
							{
								service.setStatus(item.getMacAddress(), null, getName(), getOwner(), getType());
									
								popup.hide();
							}
						});
						popup.center();
						popup.getElement().getStyle().setOpacity(1);
						popup.show();
						popup.getElement().getStyle().setOpacity(1);
						popup.getElement().getStyle().setVisibility(Visibility.VISIBLE);						
					}
				});
				panel.add(editLink);
			}			
		}
		else
		{
			service.getDevices(item.getID(), new RequestCallback()
			{
				@Override
				public void onError(final Request request, final Throwable exception)
				{
				}

				@Override
				public void onResponseReceived(final Request request, final Response response)
				{
					try
					{
						final JSONValue value = JSONParser.parseStrict(response.getText());
						final JSONArray array = value.isArray();
						if (array != null)
						{
							for (int index = 0; index < array.size(); index++)
							{
								final JSONString item = array.get(index).isString();
								if (item != null)
								{
									final String macAddress = item.stringValue();
									final Label label = new Label(macAddress);
									label.setStyleName(DevicesClient.resources.style().listDevice());
//									label.addMouseDownHandler(new MouseDownHandler()
//									{
//										@Override
//										public void onMouseDown(final MouseDownEvent event)
//										{
//											dragDevice.setupDrag(	label.getText(), label, label.getText(),
//																	event.getClientX(), event.getClientY(), null);
//										}
//									});
//									label.addTouchStartHandler(new TouchStartHandler()
//									{
//										@Override
//										public void onTouchStart(final TouchStartEvent event)
//										{
//											final Touch touch = event.getChangedTouches().get(0);
//											dragDevice.setupDrag(	label.getText(), label, label.getText(),
//																	touch.getClientX(), touch.getClientY(), null);
//										}
//									});

									panel.add(label);
								}
							}
						}
					}
					catch (final Exception e)
					{
						GWT.log(e.getMessage(), e);
					}
				}
			});
		}

		// List<Link> links = new ArrayList<Link>(device.getItem().getLinks());
		// Collections.sort(links, new Comparator<Link>()
		// {
		// @Override
		// public int compare(Link o1, Link o2)
		// {
		// return o1.getMacAddress().compareTo(o2.getMacAddress());
		// }
		// });
		// for (final Link link : links)
		// {
		// final Label label = new Label(link.getMacAddress());
		// label.setStyleName(DevicesClient.resources.style().listDevice());
		// label.addMouseDownHandler(new MouseDownHandler()
		// {
		// @Override
		// public void onMouseDown(MouseDownEvent event)
		// {
		// dragDevice.setupDrag(link, label, label.getText(), event.getClientX(),
		// event.getClientY());
		// }
		// });
		// label.addTouchStartHandler(new TouchStartHandler()
		// {
		// @Override
		// public void onTouchStart(TouchStartEvent event)
		// {
		// final Touch touch = event.getChangedTouches().get(0);
		// dragDevice.setupDrag( link, label, label.getText(), touch.getClientX(),
		// touch.getClientY());
		// }
		// });
		// panel.add(label);
		// }
		// }
		// trayPanel.addTrayLinks(device, panel);
		//
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

			for (final ZonePanel panel : zones)
			{
				panel.getElement().getStyle().setHeight(fullHeight, Unit.PX);
			}
		}
	}

	private void updateLayout()
	{
		final int bottom = -RootPanel.get().getElement().getScrollTop();
		for (final ZoneDetail detail : zoneDetails)
		{
			detail.getElement().getStyle().setBottom(bottom, Unit.PX);
			// panel.getElement().getStyle().setTop(top, Unit.PX);
		}
	}
}
