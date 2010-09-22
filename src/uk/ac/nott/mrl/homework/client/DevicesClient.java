package uk.ac.nott.mrl.homework.client;

import java.util.Collection;
import java.util.HashSet;

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
	public static final Collection<Updatable> updates = new HashSet<Updatable>();

	private final Model model = new Model();
	private final DevicesService service = new DevicesServiceImpl(model);

	public static final Resources resources = GWT.create(Resources.class);

	private final DevicesPanel panel;

	public DevicesClient()
	{
		super();
		panel = new DevicesPanel(service);
		model.add(panel.getListener());
	}

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad()
	{
		RootPanel.get().add(panel);

		final Timer requestTimer = new Timer()
		{
			@Override
			public void run()
			{
				service.getUpdates();
			}
		};
		requestTimer.scheduleRepeating(3000);
		service.getUpdates();

		final Timer animationTimer = new Timer()
		{
			@Override
			public void run()
			{
				final Collection<Updatable> removals = new HashSet<Updatable>();
				for (final Updatable update : updates)
				{
					if (update.update())
					{
						removals.add(update);
					}
				}
				updates.removeAll(removals);
			}
		};
		animationTimer.scheduleRepeating(100);
	}
}