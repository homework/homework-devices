package uk.ac.nott.mrl.homework.client;

import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundleWithLookup
{
	@Source("testlinks.txt")
	public TextResource testLinks();

	@Source("testlinkssimple.txt")
	public TextResource testLinksSimple();

	@Source("printer.png")
	public ImageResource printer();

	@Source("network-workgroup.png")
	public ImageResource all();

	@Source("drive-harddisk.png")
	public ImageResource drive();

	@Source("web-blue.png")
	public ImageResource webblue();

	@Source("web-green.png")
	public ImageResource webgreen();

	@Source("web-red.png")
	public ImageResource webred();

	@Source("process-stop.png")
	public ImageResource denied();
	
	@Source("tray-bandwidth.png")
	public ImageResource trayBandwidth();
	
	@Source("tray-signal.png")
	public ImageResource traySignal();

	@Source("tray-events.png")
	public ImageResource trayEvents();
}
