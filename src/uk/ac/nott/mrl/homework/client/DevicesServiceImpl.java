package uk.ac.nott.mrl.homework.client;

import java.util.Date;

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
			GWT.log("Response " + response.getStatusCode() + ": " + response.getText());

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
				// Handle the error. Can get the status text from response.getStatusText()
				//model.updateLinks(getLinks("[{\"timeStamp\":"
				//							+ new Date().getTime()
				//							+ ",\"macAddress\":\"00:0b:85:92:66:af\",\"rssi\":-86.70968,\"retryCount\":0,\"packetCount\":94,\"byteCount\":11750,\"permitted\":false,\"resource\":false}]"));
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
		serverRequest(GWT.getModuleBaseURL() + "deny?macAddress=" + macAddress + "&since" + model.getLastUpdated());
	}

	@Override
	public Model getModel()
	{
		return model;
	}

	@Override
	public void getTrayDevice(final RequestCallback callback)
	{
		serverRequest(GWT.getModuleBaseURL() + "getTrayDevice", callback);
	}

	@Override
	public void getTrayMode(final RequestCallback callback)
	{
		serverRequest(GWT.getModuleBaseURL() + "getTrayMode", callback);
	}

	@Override
	public void getUpdates()
	{
		serverRequest(GWT.getModuleBaseURL() + "links?since=" + model.getLastUpdated());
	}

	@Override
	public void log(final String type, final String details)
	{
		serverRequest(GWT.getModuleBaseURL() + "log?type=" + URL.encodeQueryString(type) + "&details="
				+ URL.encodeQueryString(details));
	}

	@Override
	public void nextTrayMode(final RequestCallback callback)
	{
		serverRequest(GWT.getModuleBaseURL() + "nextTrayMode", callback);
	}

	@Override
	public void permit(final String macAddress)
	{
		serverRequest(GWT.getModuleBaseURL() + "permit?macAddress=" + macAddress + "&since" + model.getLastUpdated());
	}

	@Override
	public void setName(final String macAddress, final String name)
	{
		serverRequest(GWT.getModuleBaseURL() + "setName?macAddress=" + macAddress + "&name="
				+ URL.encodeQueryString(name) + "&since=" + model.getLastUpdated());
	}

	@Override
	public void setResource(final String macAddress, final boolean resource)
	{
		serverRequest(GWT.getModuleBaseURL() + "setResource?macAddress=" + macAddress + "&resource=" + resource
				+ "&since=" + model.getLastUpdated());
	}

	@Override
	public void setTrayDevice(final String macAddress)
	{
		serverRequest(GWT.getModuleBaseURL() + "setTrayDevice?macAddress=" + macAddress);
	}

	public void setZone(final String macAddress, final int zone)
	{
		serverRequest(GWT.getModuleBaseURL() + "setZone?macAddress=" + macAddress + "&zone=" + zone + "&since"
				+ model.getLastUpdated());
	}

	private final native JsArray<Link> getLinks(final String json) /*-{
																	return eval('(' + json + ')');
																	}-*/;

	private void serverRequest(final String url)
	{
		serverRequest(url, callback);
	}

	private void serverRequest(final String url, final RequestCallback callback)
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