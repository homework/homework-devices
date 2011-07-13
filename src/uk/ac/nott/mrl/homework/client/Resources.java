package uk.ac.nott.mrl.homework.client;

import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundleWithLookup
{
	@Source("network-workgroup.png")
	public ImageResource all();

	@Source("process-stop.png")
	public ImageResource denied();

	@Source("drive-harddisk.png")
	public ImageResource drive();

	@Source("printer.png")
	public ImageResource printer();

	@Source("device.css")
	public DeviceCSS style();

	@Source("testlinks.txt")
	public TextResource testLinks();

	@Source("testlinkssimple.txt")
	public TextResource testLinksSimple();

	@Source("tray-bandwidth.png")
	public ImageResource trayBandwidth();

	@Source("tray-events.png")
	public ImageResource trayEvents();

	@Source("tray-signal.png")
	public ImageResource traySignal();

	@Source("web-blue.png")
	public ImageResource webblue();

	@Source("web-green.png")
	public ImageResource webgreen();

	@Source("web-red.png")
	public ImageResource webred();
}
