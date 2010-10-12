package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

public class WorkingTrayPanel extends TrayPanel
{
	private final String[] trayStates = { "Signal Strength Monitor", "Bandwidth Monitor", "Network Event Monitor" };

	private final ImageResource[] trayImages = { DevicesClient.resources.traySignal(),
												DevicesClient.resources.trayBandwidth(),
												DevicesClient.resources.trayEvents() };

	private int trayState = 0;

	private final Image trayIcon = new Image(DevicesClient.resources.traySignal());

	private final Label trayLabel = new Label(trayStates[0]);

	private DevicesService service;

	@Override
	public void addTrayLinks(final Device device, final Panel panel)
	{
		final Anchor trayLink = new Anchor("Monitor this Signal Strength");
		trayLink.setStylePrimaryName("popupLink");
		trayLink.addClickHandler(new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				service.setTrayDevice(device.getLink().getMacAddress());
			}
		});
		panel.add(trayLink);
	}

	@Override
	public void addTrayPanel(final Panel parent)
	{
		final FlowPanel trayPanel = new FlowPanel();
		trayPanel.setStylePrimaryName("trayButton");
		trayPanel.add(trayIcon);
		trayPanel.add(trayLabel);
		trayState = trayStates.length - 1;
		nextTrayState();

		final ClickHandler clickHandler = new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				nextTrayState();
			}
		};

		trayIcon.addClickHandler(clickHandler);
		trayLabel.addClickHandler(clickHandler);
		trayPanel.addDomHandler(clickHandler, ClickEvent.getType());

		parent.add(trayPanel);
	}

	@Override
	public void setService(final DevicesService service)
	{
		this.service = service;
	}

	private void nextTrayState()
	{
		trayState = (trayState + 1) % trayStates.length;
		GWT.log("Tray State: " + trayStates[trayState] + "(" + trayState + ")");
		service.setTrayMode(trayState);
		trayIcon.setResource(trayImages[trayState]);
		trayLabel.setText(trayStates[trayState]);
	}
}