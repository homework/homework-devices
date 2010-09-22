package uk.ac.nott.mrl.homework.client;

public interface DevicesService
{
	public void permit(final String macAddress);

	public void deny(final String macAddress);

	public void setResource(final String macAddress, final boolean resource);

	public void setName(final String macAddress, final String name);

	public void getUpdates();
}
