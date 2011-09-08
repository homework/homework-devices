package uk.ac.nott.mrl.homework.client;

import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundleWithLookup
{
	@Source("network-workgroup.png")
	ImageResource all();

	@Source("network-offline.png")
	ImageResource denied();

	@Source("drive-harddisk.png")
	ImageResource drive();

	ImageResource printer();

	@Source("emblem-important.png")
	public ImageResource requesting();

	@Source("device.css")
	DeviceCSS style();

	public TextResource testlinks();

	public TextResource testlinks2();

	public TextResource testlinkssimple();

	@Source("tray-bandwidth.png")
	public ImageResource trayBandwidth();

	@Source("tray-events.png")
	ImageResource trayEvents();

	@Source("tray-signal.png")
	ImageResource traySignal();

	ImageResource warning();

	@Source("web-blue.png")
	public ImageResource webblue();

	@Source("web-green.png")
	public ImageResource webgreen();

	@Source("web-red.png")
	public ImageResource webred();
}
