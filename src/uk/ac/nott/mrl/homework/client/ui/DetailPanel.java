package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Link;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class DetailPanel extends FlowPanel
{

	private final AnimatedInt width;

	private boolean open = false;

	// private final String[] traffic = { "Web Traffic", "Video Traffic", "BitTorrent Traffic",
	// "Game Traffic" };

	private final Label detailTitle = new Label();

	private final FlowPanel detailPanel = new FlowPanel();
	
	private final String[] trayStates = { "Wireless Signal Strength", "Bandwidth", "Network Events" };
	private final ImageResource[] trayImages = { DevicesClient.resources.traySignal(), DevicesClient.resources.trayBandwidth(), DevicesClient.resources.trayEvents() };

	private final DevicesService service;
	
	private int trayState = 0;
	
	private final Image trayIcon = new Image(DevicesClient.resources.traySignal());
	private final Label trayLabel = new Label(trayStates[0]);

	public DetailPanel(final SimplePanel buttonPanel, final DevicesService service)
	{
		super();

		this.service = service;
		add(detailTitle);
		detailTitle.setStylePrimaryName("detailTitle");
		add(detailPanel);
		detailPanel.setStylePrimaryName("detailItem");
		
		width = new AnimatedInt(0, 64, 0, 200)
		{
			@Override
			public void update(final int value)
			{
				getElement().getStyle().setWidth(value, Unit.PX);
				buttonPanel.getElement().getStyle()
						.setRight(getElement().getClientWidth() - DevicesPanel.getWinOffsetX(), Unit.PX);
			}
		};
	}


	public void expand()
	{
		width.setValue(200);
		open = true;
	}

	public void hide()
	{
		width.setValue(0);
		open = false;
	}

	public void setSelected(final Object object)
	{
		if (object instanceof Link)
		{
			setLink((Link) object);
		}
		else if (object instanceof Zone)
		{
			setZone((Zone) object);
		}
		else if (object instanceof Device)
		{
			setLink(((Device) object).getLink());
		}
	}

	public boolean toggleOpen()
	{
		if (open)
		{
			hide();
		}
		else
		{
			expand();
		}
		return open;

	}

	public void update()
	{
		width.update();
	}

	private void setLink(final Link link)
	{
		detailTitle.setText(link.getDeviceName());

		detailPanel.clear();
		// if(link.getRssi() <= -90)
		// {
		// Image image = new Image(DevicesClient.resources.signal0());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		// else if(link.getRssi() <= -80)
		// {
		// Image image = new Image(DevicesClient.resources.signal1());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		// else if(link.getRssi() <= -70)
		// {
		// Image image = new Image(DevicesClient.resources.signal2());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		// else if(link.getRssi() <= -60)
		// {
		// Image image = new Image(DevicesClient.resources.signal3());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		// else if(link.getRssi() <= -50)
		// {
		// Image image = new Image(DevicesClient.resources.signal4());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		// else
		// {
		// Image image = new Image(DevicesClient.resources.signal5());
		// image.getElement().getStyle().setFloat(Float.RIGHT);
		// detailPanel.add(image);
		// }
		detailPanel.add(new Label("Company: " + link.getCorporation()));

		// detailPanel.add(new Label("Signal Strength: " + link.getRssi()));
		// detailPanel.add(new Label("MAC Address: " + link.getMacAddress()));
		// detailPanel.add(new Label("IP Address: " + link.getIPAddress()));
		// detailPanel.add(new Label("Retry Count: " + link.getRetryCount()));
		// detailPanel.add(new Label("Packet Count: " + link.getPacketCount()));
		// detailPanel.add(new Label("Byte Count: " + link.getByteCount()));
		//
		// if(DevicesClient.zoneManager.allowDrag())
		// {
		// for (final String trafficName : traffic)
		// {
		// final FlowPanel panel = new FlowPanel();
		// panel.setStylePrimaryName("trafficPanel");
		// final Label label = new Label(trafficName);
		// label.setStylePrimaryName("trafficTitle");
		// final RadioButton button1 = new RadioButton(trafficName, "On");
		// button1.setStylePrimaryName("trafficButton");
		// final RadioButton button2 = new RadioButton(trafficName, "Throttle");
		// button2.setStylePrimaryName("trafficButton");
		// final RadioButton button3 = new RadioButton(trafficName, "Off");
		// button3.setStylePrimaryName("trafficButton");
		//
		// panel.add(label);
		// panel.add(button1);
		// panel.add(button2);
		// panel.add(button3);
		//
		// detailPanel.add(panel);
		// }
		// }
		//
		// if (link.getZone() > 1 && DevicesClient.isFullUI())
		// {
		// detailPanel.add(new Button("Add to " + DevicesPanel.getZoneNames()[link.getZone()], new
		// ClickHandler()
		// {
		// @Override
		// public void onClick(final ClickEvent event)
		// {
		// service.setResource(link.getMacAddress(), true);
		// }
		// }));
		// }
	}

	private void setZone(final Zone zone)
	{
		detailTitle.setText(zone.getName());
		detailPanel.clear();
		for (final Link resource : zone.getResources())
		{
			detailPanel.add(new Label(resource.getDeviceName()));
			detailPanel.add(new Button("Remove", new ClickHandler()
			{
				@Override
				public void onClick(final ClickEvent event)
				{
					service.setResource(resource.getMacAddress(), false);
				}
			}));
		}
	}
}