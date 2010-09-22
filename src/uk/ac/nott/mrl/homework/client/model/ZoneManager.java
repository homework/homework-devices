package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public interface ZoneManager
{
	public boolean allowDrag();

	public String[] getZones();

	public int getZoneCount();

	public int getZone(final Link link);

	public void setZone(final DevicesService service, final Link link, final int zone);
}
