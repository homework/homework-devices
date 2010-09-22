package uk.ac.nott.mrl.homework.client.model;

import com.google.gwt.core.client.JavaScriptObject;

public class Flow extends JavaScriptObject
{
	protected Flow()
	{

	}

	public final native String getDestIP() /*-{ return this.destIP; }-*/;

	public final native String getSourceIP() /*-{ return this.sourceIP;  }-*/;

	public final native int getDestPort() /*-{ return this.destPort; }-*/;

	public final native int getSourcePort() /*-{ return this.sourcePort; }-*/;

	public final native int getByteCount() /*-{ return this.byteCount; }-*/;

	public final native int getPacketCount() /*-{ return this.packetCount; }-*/;

	public final native int getProtocolNumber() /*-{ return this.protocol; }-*/;

	// public Date getTimeStamp()
	// {
	// return timeStamp;
	// }

	public final native String getClassification() /*-{ return this.classification; }-*/;
}