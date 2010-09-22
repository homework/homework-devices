package uk.ac.nott.mrl.homework.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.SimplePanel;

public class Line extends SimplePanel
{
	private final AnimatedInt width;
	private final int height;

	public Line(final int lineWidth, final int height, final String styleName)
	{
		setStylePrimaryName(styleName);
		final Style style = getElement().getStyle();
		style.setHeight(height, Unit.PX);

		this.height = height;
		this.width = new AnimatedInt(lineWidth, 1, 0, 40)
		{
			@Override
			public void update(final int value)
			{
				style.setWidth(value, Unit.PX);
			}
		};
	}

	public int getWidth()
	{
		return width.getValue();
	}

	public int getY(final int deviceY)
	{
		if (height == -1) { return 0; }
		return deviceY - height;
	}

	public void setWidth(final int i)
	{
		width.setValue(i);
	}

	public void updateHeight(final Device device, final int fullHeight)
	{
		final Style style = getElement().getStyle();
		if (height == -1)
		{
			style.setHeight(fullHeight, Unit.PX);
		}
		else
		{
			style.setHeight(device.getElement().getOffsetHeight() + 2 * height, Unit.PX);
		}
	}
}