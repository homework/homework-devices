package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Item;
import uk.ac.nott.mrl.homework.client.model.Model;

import com.google.gwt.core.client.GWT;
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

	// private boolean isSignalDevice = false;
	private Item item;

	private DevicesService service;

	private final Label text = new Label();

	private final TextBox textBoxName = new TextBox();

	// private final Model model;

	public Device(final Model model, final Item item)
	{
		// this.model = model;
		this.item = item;

		setStylePrimaryName(DevicesClient.resources.style().device());

		text.setStyleName(DevicesClient.resources.style().deviceName());
		text.setText(item.getName());
		textBoxName.setMaxLength(80);
		add(text);
		// updateStyle(null);

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
					GWT.log("Accept");
					acceptEdit();
				}
				else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
				{
					cancelEdit();
				}
			}
		});
		textBoxName.setVisible(false);

		setLeft(25);
		update(item);
	}

	public void acceptEdit()
	{
		text.setText(textBoxName.getText());
		textBoxName.setFocus(false);
		service.setName(item.getMacAddress(), textBoxName.getText());
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
		service.log("Name Edit Ended", item.getName());
		textBoxName.setFocus(false);
		textBoxName.setVisible(false);
		text.setVisible(true);
	}

	public void edit(final DevicesService service)
	{
		this.service = service;
		if (item.getMacAddress() == null) { return; }
		service.log("Name Edit Started", item.getName());
		textBoxName.setText(text.getText());
		text.setVisible(false);
		textBoxName.setVisible(true);
		textBoxName.setFocus(true);
	}

	public Item getItem()
	{
		return item;
	}

	public void setLeft(final int left)
	{
		getElement().getStyle().setLeft(left, Unit.PX);
	}

	public void setSignalDevice(final boolean signal)
	{
		// isSignalDevice = signal;
		// updateStyle(item);
	}

	public void setTop(final int top)
	{
		if (!init)
		{
			init = true;
		}
		else
		{
			addStyleName(DevicesClient.resources.style().deviceAnim());
		}
		getElement().getStyle().setTop(top, Unit.PX);
	}

	@Override
	public String toString()
	{
		return text.getText();
	}

	public void update(final Item item)
	{
		if (!text.getText().equals(item.getName()))
		{
			text.setText(item.getName());
		}

		getElement().getStyle().setOpacity(item.getOpacity());

		this.item = item;

		final ZonePanel zone = (ZonePanel) getParent();
		if (zone != null)
		{
			setStyleName(zone.getZone().getDeviceStyle(item));
		}

		// updateStyle(oldItem);

		// Font size by bandwidth
		// if (DefaultModel.zoneManager.changeSize(link))
		// {
		// getElement().getStyle().setFontSize((30 * link.getByteCount() / bandWidthMax) + 5,
		// Unit.PX);
		// // GWT.log("Bandwidth: " + (100 * link.getByteCount() / bandWidthMax) + "% - " +
		// // link.getByteCount() + "/" + bandWidthMax);
		//
		// // Font size by Signal Strength:
		// // fontSize.setValue((int) (50 + (link.getRssi() / 2)));
		//
		// }
		// else
		// {
		// getElement().getStyle().setFontSize(15, Unit.PX);
		// }

		// if (getZone() != oldZone)
		// {
		// setLeft(getZone() * model.getZoneWidth() + 25);
		// }
	}
}