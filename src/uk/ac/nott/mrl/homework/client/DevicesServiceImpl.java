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

		serverRequest(GWT.getModuleBaseURL() + "status?command=deny&macAddress=" + macAddress + "&since=" + model.getLastUpdated());
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
	public void getUpdates(final RequestCallback callback)
	{
		serverRequest(GWT.getModuleBaseURL() + "changes?since=" + model.getLastUpdated(), callback);
	}

	@Override
	public void log(final String type, final String details)
	{
		serverRequest(GWT.getModuleBaseURL() + "log?type=" + URL.encodeQueryString(type) + "&details="
				+ URL.encodeQueryString(details));
	}

	@Override
	public void permit(final String macAddress)
	{
		serverRequest(GWT.getModuleBaseURL() + "status?command=permit&macAddress=" + macAddress + "&since=" + model.getLastUpdated());
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
			builder.setTimeoutMillis(15000);
			builder.sendRequest(null, callback);
		}
		catch (final Exception e)
		{
			GWT.log(e.getMessage(), e);
		}
	}

	public void setZone(final String macAddress, final int zone)
	{
		serverRequest(GWT.getModuleBaseURL() + "setZone?macAddress=" + macAddress + "&zone=" + zone + "&since"
				+ model.getLastUpdated());
	}

	@Override
	public void getMetadata(RequestCallback callback)
	{
		serverRequest(GWT.getHostPageBaseURL() + "metadata.json", callback);
	}
	
	@Override
	public void setStatus(String macAddress, String state, String name,
			String owner, String type)
	{
		String url = GWT.getModuleBaseURL() + "status?macAddress=" + macAddress;
		if(state != null)
		{
			url = url + "&command=" + state;
		}
		if(name != null)
		{
			url = url + "&name=" + name;
		}
		if(owner != null)
		{
			url = url + "&owner=" + owner;
		}
		if(type != null)
		{
			url = url + "&type=" + type;
		}
		url = url + "&since=" + model.getLastUpdated();
		serverRequest(url);
	}
}