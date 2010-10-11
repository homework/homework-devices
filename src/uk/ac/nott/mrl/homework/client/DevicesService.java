package uk.ac.nott.mrl.homework.client;

public interface DevicesService
{
	public void deny(final String macAddress);

	public void getUpdates();

	public void permit(final String macAddress);

	public void setName(final String macAddress, final String name);

	public void setResource(final String macAddress, final boolean resource);
	
	public void setTrayMode(final int mode);
	
	public void setTrayDevice(final String macAddress);	
	
	public void log(final String type, final String details);
}
