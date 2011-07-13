package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Link;
import uk.ac.nott.mrl.homework.client.model.Model;

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
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class Device extends FlowPanel
{
	private boolean init = false;

	private boolean isSignalDevice = false;
	private Link link;

	private DevicesService service;

	private final Label text = new Label();

	private final TextBox textBoxName = new TextBox();

	public Device(final Link link, final int bandWidthMax)
	{
		this.link = link;

		setStylePrimaryName(DevicesClient.resources.style().device());

		text.setStyleName(DevicesClient.resources.style().deviceName());
		text.setText(getDeviceName());
		textBoxName.setMaxLength(80);
		add(text);
		updateStyle(null);

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

		setLeft(getZone() * DevicesPanel.getZoneWidth() + 25);
		update(link, bandWidthMax);
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

	public void addTouchStartHandler(final TouchStartHandler handler)
	{
		addDomHandler(handler, TouchStartEvent.getType());
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
		updateStyle(link);
	}

	public void setTop(final int top)
	{
		getElement().getStyle().setTop(top, Unit.PX);
		if (!init)
		{
			addStyleName(DevicesClient.resources.style().deviceAnim());
			init = true;
		}
	}

	@Override
	public String toString()
	{
		return text.getText();
	}

	public void update(final Link link, final int bandWidthMax)
	{
		final Link oldLink = this.link;

		final int oldZone = getZone();
		this.link = link;

		if (!getDeviceName().equals(text.getText()))
		{
			text.setText(getDeviceName());
		}

		updateStyle(oldLink);

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

	private void updateStyle(final Link oldLink)
	{
		String oldStyle = null;
		if(oldLink != null)
		{
			oldStyle = Model.zoneManager.getStyleName(oldLink);
		}
		String newStyle = Model.zoneManager.getStyleName(link);
		if(!newStyle.equals(oldStyle))
		{
			removeStyleName(oldStyle);
			addStyleName(newStyle);
		}
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