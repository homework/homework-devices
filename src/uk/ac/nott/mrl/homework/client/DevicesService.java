package uk.ac.nott.mrl.homework.client;

import com.google.gwt.http.client.RequestCallback;

public interface DevicesService
{
	public void deny(final String macAddress);

	public void getTrayDevice(RequestCallback trayModeCallback);

	public void getTrayMode(final RequestCallback callback);

	public void getUpdates();

	public void log(final String type, final String details);

	public void nextTrayMode(final RequestCallback callback);

	public void permit(final String macAddress);

	public void setName(final String macAddress, final String name);

	public void setResource(final String macAddress, final boolean resource);

	public void setTrayDevice(final String macAddress);
}
