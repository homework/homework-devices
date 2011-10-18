package uk.ac.nott.mrl.homework.client.ui;

import com.google.gwt.dom.client.Element;

public class TransformImplSafari implements Transform
{
	@Override
	public void translateY(Element element, int y)
	{
		element.getStyle().setProperty("webkitTransform", "translate3d(0px," + y + "px ,0px)");
	}
}
