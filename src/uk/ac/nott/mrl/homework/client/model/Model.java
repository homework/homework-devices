package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;

import uk.ac.nott.mrl.homework.client.model.Item.State;
import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.core.client.JsArray;

public interface Model
{
	void addListener(final ItemListener listener);

	boolean canBeGrouped(Link link);

	Comparator<Device> getComparator();

	long getLastUpdated();

	String getName(Link link);

	State getState(Link link);

	int getZone(Link link);

	Zone[] getZones();
	
	void removeLink(Link link);

	void updateLinks(final JsArray<Link> newLinks);
	
	void updateLink(Link link);
}
