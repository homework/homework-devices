package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;

public class DevicesServiceImpl implements DevicesService
{
	private final RequestCallback callback = new RequestCallback()
	{
		@Override
		public void onError(final Request request, final Throwable exception)
		{
			GWT.log(exception.getMessage(), exception);
		}

		@Override
		public void onResponseReceived(final Request request, final Response response)
		{
			if (200 == response.getStatusCode())
			{
				try
				{
					model.updateLinks(getLinks(response.getText()));
				}
				catch (final Exception e)
				{
					GWT.log(response.getText(), e);
				}
			}
			else
			{
				GWT.log("Response code: " + response.getStatusCode(), null);
				// Handle the error. Can get the status text from response.getStatusText()
			}
		}
	};

	private final Model model;

	public DevicesServiceImpl(final Model model)
	{
		this.model = model;
	}

	@Override
	public void deny(final String macAddress)
	{
		serverRequest(GWT.getModuleBaseURL() + "deny?macAddress=" + macAddress + "&since" + model.getMostRecent());
	}

	@Override
	public void getUpdates()
	{
		final String url = GWT.getModuleBaseURL() + "links?since=" + model.getMostRecent();
		serverRequest(url);
	}

	@Override
	public void permit(final String macAddress)
	{
		serverRequest(GWT.getModuleBaseURL() + "permit?macAddress=" + macAddress + "&since" + model.getMostRecent());
	}

	@Override
	public void setName(final String macAddress, final String name)
	{
		serverRequest(GWT.getModuleBaseURL() + "setName?macAddress=" + macAddress + "&name="
				+ URL.encodeQueryString(name) + "&since=" + model.getMostRecent());
	}

	@Override
	public void setResource(final String macAddress, final boolean resource)
	{
		serverRequest(GWT.getModuleBaseURL() + "setResource?macAddress=" + macAddress + "&resource=" + resource
				+ "&since=" + model.getMostRecent());
	}

	public void setZone(final String macAddress, final int zone)
	{
		serverRequest(GWT.getModuleBaseURL() + "setZone?macAddress=" + macAddress + "&zone=" + zone + "&since"
				+ model.getMostRecent());
	}

	private final native JsArray<Link> getLinks(final String json) /*-{
																	return eval('(' + json + ')');
																	}-*/;

	private void serverRequest(final String url)
	{
		final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
		try
		{
			builder.sendRequest(null, callback);
		}
		catch (final Exception e)
		{
			GWT.log(e.getMessage(), e);
		}
	}
}