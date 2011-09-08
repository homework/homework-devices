package uk.ac.nott.mrl.homework.client.model;

import java.util.Comparator;

import uk.ac.nott.mrl.homework.client.ui.Device;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestCallback;

public interface Model
{
	void addListener(final ItemListener listener);

	RequestCallback getCallback();

	Comparator<Device> getComparator();

	double getLastUpdated();

	int getZone(Item item);

	Zone[] getZones();

	void update(JsArray<Item> items);

	void update(String id, String state);
}
