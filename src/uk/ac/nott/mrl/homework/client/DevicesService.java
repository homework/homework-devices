package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.http.client.RequestCallback;

public interface DevicesService
{
	public void deny(final String macAddress);

	public void getDevices(final String groupID, final RequestCallback callback);

	public Model getModel();

	public void getUpdates(final RequestCallback callback);

	public void log(final String type, final String details);

	public void permit(final String macAddress);
	
	public void setStatus(final String macAddress, final String state, final String name, final String owner, final String type);
	
	public void getMetadata(final RequestCallback callback);
}
