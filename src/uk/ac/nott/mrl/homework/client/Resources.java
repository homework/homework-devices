package uk.ac.nott.mrl.homework.client;

import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundleWithLookup
{
	public TextResource testlinks();
	
	public TextResource testlinks2();	

	public TextResource testlinkssimple();

	@Source("tray-bandwidth.png")
	public ImageResource trayBandwidth();

	@Source("web-blue.png")
	public ImageResource webblue();

	@Source("web-green.png")
	public ImageResource webgreen();

	@Source("web-red.png")
	public ImageResource webred();

	@Source("network-workgroup.png")
	ImageResource all();

	@Source("process-stop.png")
	ImageResource denied();

	@Source("drive-harddisk.png")
	ImageResource drive();

	ImageResource printer();

	@Source("device.css")
	DeviceCSS style();

	@Source("tray-events.png")
	ImageResource trayEvents();

	@Source("tray-signal.png")
	ImageResource traySignal();

	ImageResource warning();
}
