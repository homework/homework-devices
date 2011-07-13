package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

public class TrayPanel extends FlowPanel
{
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
				if (!response.getText().equals(""))
				{
					final int trayState = Integer.parseInt(response.getText());
					trayIcon.setResource(trayImages[trayState]);
					trayLabel.setText(trayStates[trayState]);
					setVisible(true);
				}
				else
				{
					setVisible(false);
				}
			}
			catch (final Exception e)
			{
				GWT.log(e.getMessage(), e);
			}
		}
	};

	private final DevicesService service;

	private final Image trayIcon = new Image(DevicesClient.resources.traySignal());

	private final ImageResource[] trayImages = { DevicesClient.resources.traySignal(),
												DevicesClient.resources.trayBandwidth(),
												DevicesClient.resources.trayEvents() };

	private final String[] trayStates = { "Signal Strength Monitor", "Bandwidth Monitor", "Network Event Monitor" };
	
	private final Label trayLabel = new Label(trayStates[0]);

	public TrayPanel(final DevicesService service)
	{
		this.service = service;
		service.getTrayMode(callback);

		setStylePrimaryName("trayButton");
		add(trayIcon);
		add(trayLabel);

		final ClickHandler clickHandler = new ClickHandler()
		{
			@Override
			public void onClick(final ClickEvent event)
			{
				service.nextTrayMode(callback);
			}
		};

		trayIcon.addClickHandler(clickHandler);
		trayLabel.addClickHandler(clickHandler);
		addDomHandler(clickHandler, ClickEvent.getType());

		setVisible(false);
	}

	public void addTrayLinks(final Device device, final Panel panel)
	{
		if (isVisible())
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
	}

	public RequestCallback getTrayModeCallback()
	{
		return callback;
	}
}