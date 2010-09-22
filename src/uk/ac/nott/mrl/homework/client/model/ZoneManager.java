package uk.ac.nott.mrl.homework.client.model;

import uk.ac.nott.mrl.homework.client.DevicesService;

public interface ZoneManager
{
	public boolean allowDrag();

	public int getZone(final Link link);

	public int getZoneCount();

	public String[] getZones();

	public void setZone(final DevicesService service, final Link link, final int zone);
}
