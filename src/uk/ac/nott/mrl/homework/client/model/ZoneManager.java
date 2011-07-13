package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;
import java.util.List;

import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.user.client.ui.Image;

public interface ZoneManager
{
	public boolean allowDrag();

	public Comparator<Device> getComparator();

	public int getZone(final Link link);

	public int getZoneCount();

	public String[] getZones();

	public int reflowDevices(final List<Device> devices);

	public void setZone(final DevicesService service, final Link link, final int zone);

	public void updateImage(final int zone, final Image image, final float data);
}
