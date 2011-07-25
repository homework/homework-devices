package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;

import uk.ac.nott.mrl.homework.client.model.Item.State;
import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.ImageResource;

public interface Model
{
	public ImageResource getImage(int zone);

	void addListener(final ItemListener listener);

	boolean allowDrag();

	boolean canBeGrouped(Link link);

	Comparator<Device> getComparator();

	long getLastUpdated();

	String getName(Link link);

	State getState(Link link);

	int getZone(Link link);

	Zone[] getZones();

	void updateLinks(final JsArray<Link> newLinks);
}
