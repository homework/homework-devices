package uk.ac.nott.mrl.homework.client.model;

import com.google.gwt.core.client.JavaScriptObject;

public class Link extends JavaScriptObject
{
	protected Link()
	{

	}

	public final native int getByteCount() /*-{ return this.byteCount; }-*/;

	public final native String getCorporation() /*-{ return this.corporation; }-*/;

	public final native String getDeviceName() /*-{ return this.deviceName; }-*/;

	public final native String getIPAddress() /*-{ return this.ipAddress; }-*/;

	public final native String getMacAddress() /*-{ return this.macAddress; }-*/;

	public final native boolean getOld() /*-{ return this.old || false; }-*/;

	public final native int getPacketCount() /*-{ return this.packetCount; }-*/;

	public final native int getRetryCount() /*-{ return this.retryCount; }-*/;

	public final native float getRssi() /*-{ return this.rssi; }-*/;

	public final native String getState() /*-{ return this.state; }-*/;

	public final native String setState(String state) /*-{ this.state = state; }-*/;
	
	public final native double getTimestamp() /*-{ return this.timeStamp; }-*/;

	public final native boolean isResource() /*-{ return this.resource || false; }-*/;

	
	
	public final native boolean setOld() /*-{ var oldOld = this.old || false; 
											this.old = true; return !oldOld; }-*/;
}