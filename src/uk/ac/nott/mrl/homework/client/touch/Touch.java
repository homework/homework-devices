package uk.ac.nott.mrl.homework.client.touch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;

public class Touch extends JavaScriptObject
{

	protected Touch()
	{
	}

	public final native int pageX() /*-{
									return this.pageX;
									}-*/;

	public final native Node target() /*-{
										return this.target;
										}-*/;
}