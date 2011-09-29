package uk.ac.nott.mrl.homework.client;

import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.http.client.RequestCallback;

public interface DevicesService
{
	public void deny(final String macAddress);

	public void getDevices(final String groupID, final RequestCallback callback);

	public Model getModel();

	public void getTrayDevice(RequestCallback trayModeCallback);

	public void getTrayMode(final RequestCallback callback);

	public void getUpdates(final RequestCallback callback);

	public void log(final String type, final String details);

	public void nextTrayMode(final RequestCallback callback);

	public void permit(final String macAddress);

	public void setName(final String macAddress, final String name);

	public void setResource(final String macAddress, final boolean resource);

	public void setTrayDevice(final String macAddress);
}
