package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.ui.DevicesPanel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DevicesClient implements EntryPoint
{
	public static final Resources resources = GWT.create(Resources.class);
	private final Model model = GWT.create(Model.class);

	private final DevicesPanel panel;

	private final DevicesService service = new DevicesServiceImpl(model);

	public DevicesClient()
	{
		super();
		panel = new DevicesPanel(service);
	}

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad()
	{
		RootPanel.get().add(panel);
		resources.style().ensureInjected();

		final Timer requestTimer = new Timer()
		{
			@Override
			public void run()
			{
				service.getUpdates();
				// service.getTrayMode(panel.getTrayModeCallback());
				// service.getTrayDevice(panel.getTrayDeviceCallback());
			}
		};
		requestTimer.scheduleRepeating(3000);
		service.getUpdates();

		service.log("STARTED", "");
	}
}