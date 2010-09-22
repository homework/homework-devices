package uk.ac.nott.mrl.homework.client.touch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class TouchEvent extends JavaScriptObject
{
	protected TouchEvent()
	{
	}

	public final native JsArray<Touch> changedTouches() /*-{ return this.changedTouches; }-*/;

	public final native void preventDefault() /*-{ this.preventDefault(); }-*/;

	public final native JsArray<Touch> targetTouches() /*-{ return this.targetTouches; }-*/;

	public final native JsArray<Touch> touches() /*-{ return this.touches; }-*/;
}
