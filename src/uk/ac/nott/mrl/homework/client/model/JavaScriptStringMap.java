package uk.ac.nott.mrl.homework.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class JavaScriptStringMap extends JavaScriptObject
{
	protected JavaScriptStringMap()
	{
	}

	public final native String get(String key)
	/*-{
		return this[key];
	}-*/;

	public final native void put(String key, String value)
	/*-{
		this[key] = value;
	}-*/;
	
	public final native JsArrayString getKeys()
	/*-{
		var keys = [];
		for(var key in this)
		{
			keys.push(key);
   		}
		return keys;
	}-*/;
}