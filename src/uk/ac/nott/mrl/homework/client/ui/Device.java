package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.touch.TouchEvent;
import uk.ac.nott.mrl.homework.client.touch.TouchHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class Device extends FlowPanel
{
	private Link link;

	private final TextBox textBoxName = new TextBox();
	private final Label text = new Label();

	private DevicesService service;

	private TouchHandler touchStart;

	private boolean isSignalDevice = false;

	public Device(final Link link, final int bandWidthMax)
	{
		this.link = link;

		text.setStyleName("deviceName");
		text.setText(getDeviceName());
		textBoxName.setMaxLength(80);
		add(text);
		updateStyle(link.getState());

		add(textBoxName);
		textBoxName.addBlurHandler(new BlurHandler()
		{
			@Override
			public void onBlur(final BlurEvent event)
			{
				cancelEdit();
			}
		});
		textBoxName.addKeyDownHandler(new KeyDownHandler()
		{
			@Override
			public void onKeyDown(final KeyDownEvent event)
			{
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
				{
					acceptEdit();
				}
				else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
				{
					cancelEdit();
				}
			}
		});
		textBoxName.setVisible(false);

		// fontSize = new AnimatedInt(10, 1, 10, 40)
		// {
		// @Override
		// public void update(final int value)
		// {
		// getElement().getStyle().setFontSize(value, Unit.PX);
		// }
		// };
		// opacity = new AnimatedFloat(0, 1, 0.05f, 0, 1)
		// {
		// @Override
		// public void update(final float value)
		// {
		// getElement().getStyle().setOpacity(value);
		// }
		// };
		// error = new AnimatedInt(0, 1, 0, 10)
		// {
		// @Override
		// public void update(final int value)
		// {
		// getElement().getStyle().setProperty("borderLeftWidth", value + "px");
		// getElement().getStyle().setProperty("borderLeftColor", "#E22");
		// getElement().getStyle().setProperty("borderLeftStyle", "solid");
		// }
		// };
		// y = new AnimatedInt(0, 40, 0, 1000000)
		// {
		// @Override
		// protected void update(final int value)
		// {
		// getElement().getStyle().setTop(value, Unit.PX);
		// }
		// };

		setLeft(getZone() * DevicesPanel.getZoneWidth() + 25);
		update(link, bandWidthMax);
		registerDomTouchEvents();
	}

	public void acceptEdit()
	{
		text.setText(textBoxName.getText());
		textBoxName.setFocus(false);
		service.setName(link.getMacAddress(), textBoxName.getText());
	}

	public void addClickHandler(final ClickHandler handler)
	{
		addDomHandler(handler, ClickEvent.getType());
	}

	public void addDoubleClickHandler(final DoubleClickHandler handler)
	{
		addDomHandler(handler, DoubleClickEvent.getType());
	}

	public void addMouseDownHandler(final MouseDownHandler handler)
	{
		addDomHandler(handler, MouseDownEvent.getType());
	}

	public void addTouchStartHandler(final TouchHandler handler)
	{
		touchStart = handler;
	}

	public void cancelEdit()
	{
		service.log("Cancel Edit", link.getMacAddress());
		textBoxName.setFocus(false);
		textBoxName.setVisible(false);
		text.setVisible(true);
	}

	public void edit(final DevicesService service)
	{
		this.service = service;
		service.log("Start Edit", link.getMacAddress());
		textBoxName.setText(text.getText());
		text.setVisible(false);
		textBoxName.setVisible(true);
		textBoxName.setFocus(true);
	}

	public Link getLink()
	{
		return link;
	}

	public int getZone()
	{
		return Model.zoneManager.getZone(getLink());
	}

	public void setLeft(final int left)
	{
		getElement().getStyle().setLeft(left, Unit.PX);
	}

	public void setSignalDevice(final boolean signal)
	{
		isSignalDevice = signal;
		updateStyle(link.getState());
	}

	public void setTop(final int top)
	{
		getElement().getStyle().setTop(top, Unit.PX);
	}

	@Override
	public String toString()
	{
		return text.getText();
	}

	public void update(final Link link, final int bandWidthMax)
	{
		final String oldState = this.link.getState();
		
		final int oldZone = getZone();
		this.link = link;

		if (!getDeviceName().equals(text.getText()))
		{
			text.setText(getDeviceName());
		}

		if (!oldState.equals(link.getState()))
		{
			updateStyle(oldState);
		}


		// Font size by bandwidth
		if (link.getIPAddress() != null)
		{
			getElement().getStyle().setFontSize((30 * link.getByteCount() / bandWidthMax) + 5, Unit.PX);
			// GWT.log("Bandwidth: " + (100 * link.getByteCount() / bandWidthMax) + "% - " +
			// link.getByteCount() + "/" + bandWidthMax);
			
			// Font size by Signal Strength:
			// fontSize.setValue((int) (50 + (link.getRssi() / 2)));

		}
		else
		{
			getElement().getStyle().setFontSize(15, Unit.PX);
		}

		if (link.getOld())
		{
			getElement().getStyle().setOpacity(0.2);
		}
		else
		{
			// Constance opacity
			// opacity.setValue(1, 0.05f, AnimationType.decel);

			// Opacity by Signal Strength
			getElement().getStyle().setOpacity(1.3 + (link.getRssi() / 100));
			// opacity.setValue(1.3f + (link.getRssi() / 100), 0.05f, AnimationType.decel);
		}

		if (getZone() != oldZone)
		{
			setLeft(getZone() * DevicesPanel.getZoneWidth() + 25);
		}
	}

	private String getDeviceName()
	{
		String deviceName = link.getDeviceName();
		if (deviceName == null)
		{
			deviceName = link.getCorporation();
			if (deviceName != null)
			{
				int cut = deviceName.indexOf(' ');
				if (cut != -1)
				{
					deviceName = deviceName.substring(0, cut);
				}

				cut = deviceName.indexOf(',');
				if (cut != -1)
				{
					deviceName = deviceName.substring(0, cut);
				}
				deviceName += " Device";
			}
			else
			{
				deviceName = "Unknown Device";
			}
		}
		return deviceName;
	}

	private void onTouchStart(final JavaScriptObject event)
	{
		final TouchEvent touchEvent = event.cast();
		if (touchStart != null)
		{
			touchStart.onTouchEvent(this, touchEvent);
		}
	}

	private final native void registerDomTouchEvents()
	/*-{
		var instance = this;
		var element = this.@uk.ac.nott.mrl.homework.client.ui.Device::getElement()();

		element.addEventListener("touchstart", $entry(function(e){
		        instance.@uk.ac.nott.mrl.homework.client.ui.Device::onTouchStart(Lcom/google/gwt/core/client/JavaScriptObject;)(e);
		}), false);
	}-*/;

	private void updateStyle(final String oldState)
	{
		setStylePrimaryName("device");
		removeStyleName(oldState);
		addStyleName(link.getState());
		if (isSignalDevice)
		{
			addStyleName("signal");
		}
		else
		{
			removeStyleName("signal");
		}
	}
}