package uk.ac.nott.mrl.homework.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;

public class TransformImpl implements Transform
{
	@Override
	public void translateY(final Element element, int y)
	{
		element.getStyle().setTop(y, Unit.PX);		
	}
}
