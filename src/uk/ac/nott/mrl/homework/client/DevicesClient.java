package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Item;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.ui.DevicesPanel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
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
				try
				{
					service.getUpdates(new RequestCallback()
					{	
						private final native JsArray<Item> getItems(final String json)
						/*-{
							return eval('(' + json + ')');
						}-*/;
						
						@Override
						public void onError(final Request request, final Throwable exception)
						{
							GWT.log(exception.getMessage(), exception);
							schedule(3000);
						}

						@Override
						public void onResponseReceived(final Request request, final Response response)
						{
							GWT.log("Response " + response.getStatusCode() + ": " + response.getText());

							if (200 == response.getStatusCode())
							{
								try
								{
									// update(getItems(DevicesClient.resources.testlinks().getText()));
									model.update(getItems(response.getText()));
								}
								catch (final Exception e)
								{
									GWT.log(response.getText(), e);
								}
							}
							schedule(3000);
						}
					});	
				}
				catch(Exception e)
				{
					GWT.log(e.getMessage(), e);
				}
			}
		};
		requestTimer.run();

		service.log("STARTED", "");
	}
}