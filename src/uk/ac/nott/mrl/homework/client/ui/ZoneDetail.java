package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.model.Model;
import uk.ac.nott.mrl.homework.client.model.Zone;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class ZoneDetail extends FlowPanel
{
	private final Image image = new Image();

	private final Label label = new Label();

	public ZoneDetail(final Model model, final Zone zone)
	{
		super();
		getElement().getStyle().setWidth(100 / model.getZones().length, Unit.PCT);
		getElement().getStyle().setLeft((100 / model.getZones().length) * zone.getIndex(), Unit.PCT);		

		setStylePrimaryName(DevicesClient.resources.style().zone());

		if (zone.getImage() != null)
		{
			image.setResource(zone.getImage());
		}
		label.setText(zone.getName());		
		add(image);
		add(label);	
	}
}
