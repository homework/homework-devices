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
import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.LinkListItem;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.model.Zone;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
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

	private final Map<Item, Device> deviceMap = new HashMap<Item, Device>();

	private final DragDevice dragDevice;

	private final Timer fadeTimer = new Timer()
	{
		@Override
		public void run()
		{
			popup.getElement().getStyle().setOpacity(0);
		}
	};
	private int fullHeight;

	// private final AnimatedFloat popupOpacity;

	private final Model model;

	private int offsetx;

	private final PopupPanel popup = new PopupPanel(true);

	private Device popupDevice;

	private Object selected;

	private final DevicesService service;
	private String signalDeviceMac;
	private final TrayPanel trayPanel;

	private final List<ZonePanel> zones = new ArrayList<ZonePanel>();
	private final List<ZoneDetail> zoneDetails = new ArrayList<ZoneDetail>();	

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
				//GWT.log("Item Added: " + item.getName());
				final ZonePanel zone = getZone(item.getZone().getIndex());
				if (item.isResource())
				{
					// zone.add(item);
				}
				else
				{
					final Device device = new Device(model, item);
					if (item.getID().equals(signalDeviceMac))
					{
						device.setSignalDevice(true);
					}
					deviceMap.put(item, device);
					zone.add(device);
					// add(device);

					device.addMouseDownHandler(new MouseDownHandler()
					{
						@Override
						public void onMouseDown(final MouseDownEvent event)
						{
							dragDevice.setupDrag(device.getLink(), device, device.toString(), event.getClientX(),
													event.getClientY());
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
					device.addTouchStartHandler(new TouchStartHandler()
					{
						@Override
						public void onTouchStart(final TouchStartEvent event)
						{
							final Touch touch = event.getChangedTouches().get(0);
							dragDevice.setupDrag(	device.getLink(), device, device.toString(), touch.getClientX(),
													touch.getClientY());
						}
					});
				}
			}

			@Override
			public void itemRemoved(final Item item)
			{
				//GWT.log("Item Removed: " + item.getName());
				final ZonePanel zone = getZone(item.getZone().getIndex());
				if (item.isResource())
				{
					// zone.remove(item);
				}
				else
				{
					final Device device = deviceMap.get(item);
					if (device != null)
					{
						zone.remove(device);
						deviceMap.remove(item);
					}
				}
			}

			@Override
			public void itemUpdated(final Item item)
			{
				
				//GWT.log("Item Updated: " + item.getName());
				final ZonePanel newZone = getZone(item.getZone().getIndex());
				final Device device = deviceMap.get(item);
				if (item.isResource())
				{
					if (device != null)
					{
						device.removeFromParent();
						deviceMap.remove(item);
					}
					else
					{
						// newZone.update(item);
					}
				}
				else if (device == null)
				{
					// newZone.remove(item);
					itemAdded(item);
				}
				else
				{
					final ZonePanel oldZone = getZone(device.getItem().getZone().getIndex());
					if (newZone != oldZone)
					{
						oldZone.remove(device);
						newZone.add(device);
					}
					device.update();
					if (device == popupDevice)
					{
						setPopup(device);
					}
				}
			}

			@Override
			public void itemUpdateFinished()
			{
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
			public void onMouseMove(MouseMoveEvent event)
			{
				// TODO get zone for 
				for(ZonePanel zone: zones)
				{
					int relX = event.getRelativeX(zone.getElement());
					int relY = event.getRelativeY(zone.getElement());
					if(relX > 0 && relX < zone.getOffsetWidth() && relY > 0 && relY < zone.getOffsetWidth())
					{
						dragDevice.setDragZone(zone);
					}
				}
			}
		}, MouseMoveEvent.getType());
		
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
		for (final Device device : devices)
		{
			device.setTop(top);
			top = top + device.getOffsetHeight() + 15;

		}
		maxDevice = Math.max(maxDevice, top);

		maxDevice = Math.max(maxDevice, getElement().getClientHeight());
		updateClientHeight(maxDevice);
	}

	private void setPopup(final Device device)
	{
		final FlowPanel panel = new FlowPanel();

		if (device.getLink() != null)
		{
			final Link link = device.getLink();

			if (link.getState() != null && link.getState().equals("requesting"))
			{
				final Label label = new Label(
						"This machine is requesting permission to use your network. Drag it to the right to allow it or to the left to deny it access.");
				label.addStyleName(DevicesClient.resources.style().warning());
				panel.add(label);
			}

			if (link.getCorporation() == null || link.getCorporation().equals("Unknown"))
			{
				panel.add(new Label("Manufacturer: Unknown"));
			}
			else
			{
				final FlowPanel panel2 = new FlowPanel();
				panel2.add(new InlineLabel("Manufacturer: "));
				final Anchor companySearch = new Anchor(link.getCorporation(), "http://www.google.co.uk/search?q="
						+ URL.encodeQueryString(link.getCorporation()), "_blank");
				companySearch.addClickHandler(new ClickHandler()
				{
					@Override
					public void onClick(final ClickEvent event)
					{
						service.log("Search Manufacturer", link.getMacAddress());
					}
				});
				panel2.add(companySearch);
				panel.add(panel2);
			}
			panel.add(new Label("MAC Address: " + link.getMacAddress()));

			final Anchor renameLink = new Anchor("Rename Device");
			renameLink.setStylePrimaryName(DevicesClient.resources.style().popupLink());
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
		}
		else
		{
			final LinkListItem item = (LinkListItem) device.getItem();
			final Link firstLink = item.getLinks().iterator().next();
			if (firstLink.getCorporation() == null || firstLink.getCorporation().equals("Unknown"))
			{
				panel.add(new Label("Manufacturer: Unknown"));
			}
			else
			{
				final FlowPanel panel2 = new FlowPanel();
				panel2.add(new InlineLabel("Manufacturer: "));
				final Anchor companySearch = new Anchor(firstLink.getCorporation(), "http://www.google.co.uk/search?q="
						+ URL.encodeQueryString(firstLink.getCorporation()), "_blank");
				companySearch.addClickHandler(new ClickHandler()
				{
					@Override
					public void onClick(final ClickEvent event)
					{
						service.log("Search Manufacturer", firstLink.getMacAddress());
					}
				});
				panel2.add(companySearch);
				panel.add(panel2);
			}
			for (final Link link : item.getLinks())
			{
				final Label label = new Label(link.getMacAddress());
				label.setStyleName(DevicesClient.resources.style().listDevice());
				label.addMouseDownHandler(new MouseDownHandler()
				{
					@Override
					public void onMouseDown(MouseDownEvent event)
					{
						dragDevice.setupDrag(link, label, label.getText(), event.getClientX(), event.getClientY());	
					}
				});
				panel.add(label);
			}
		}
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
			
			for (final ZonePanel panel : zones)
			{
				panel.getElement().getStyle().setHeight(fullHeight, Unit.PX);
			}					
		}
	}

	private void updateLayout()
	{
		final int bottom = RootPanel.get().getElement().getClientHeight() + RootPanel.get().getElement().getScrollTop();
		for (final ZoneDetail detail : zoneDetails)
		{
			detail.getElement().getStyle().setBottom(bottom, Unit.PX);
			//panel.getElement().getStyle().setTop(top, Unit.PX);
		}
	}
}
