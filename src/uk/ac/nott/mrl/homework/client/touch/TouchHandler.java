package uk.ac.nott.mrl.homework.client.touch;

import uk.ac.nott.mrl.homework.client.ui.Device;

public interface TouchHandler
{
	public void onTouchEvent(Device thingy, TouchEvent event);
}
