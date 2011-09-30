package uk.ac.nott.mrl.homework.client.model;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.PopupPanel;

import uk.ac.nott.mrl.homework.client.DevicesClient;
import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.ui.DeviceMetadataDialog;

public class ControlInternetZone extends Zone
{
	public ControlInternetZone(final int index)
	{
		super(index, "Permitted", DevicesClient.resources.webblue());
	}

	@Override
	public void add(final DevicesService service, final Item item)
	{
		final PopupPanel popup = new PopupPanel(false, true);
		popup.setWidget(new DeviceMetadataDialog(item, service)
		{
			@Override
			protected void cancel()
			{
				popup.hide();
			}
			
			@Override
			protected void accept()
			{
				service.setState(item.getMacAddress(), "permit", getName(), getOwner(), getType());
					
				popup.hide();
				
				//service.getModel().update(macAddress, "permit");
				//service.permit(macAddress);				
			}
		});
		popup.center();
		popup.getElement().getStyle().setOpacity(1);
		popup.show();
		popup.getElement().getStyle().setOpacity(1);
		popup.getElement().getStyle().setVisibility(Visibility.VISIBLE);
	}

	@Override
	public boolean canAdd()
	{
		return true;
	}
}
