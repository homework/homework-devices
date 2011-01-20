package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;
import java.util.List;

import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.ui.Device;

public interface ZoneManager
{
	public boolean allowDrag();

	public int getZone(final Link link);

	public int getZoneCount();

	public String[] getZones();

	public void setZone(final DevicesService service, final Link link, final int zone);
	
	public Comparator<Device> getComparator();
	
	public int reflowDevices(final List<Device> devices);
}
