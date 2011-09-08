package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.URL;

public class DevicesServiceImpl implements DevicesService
{
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
	public void getDevices(final String groupID, final RequestCallback callback)
	{
		serverRequest(	GWT.getModuleBaseURL() + "changes?group=" + groupID + "&since=" + model.getLastUpdated(),
						callback);
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
		serverRequest(GWT.getModuleBaseURL() + "changes?since=" + model.getLastUpdated());
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

	private void serverRequest(final String url)
	{
		serverRequest(url, model.getCallback());
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
}