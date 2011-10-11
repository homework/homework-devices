package uk.ac.nott.mrl.homework.client.ui;

import uk.ac.nott.mrl.homework.client.DevicesService;
import uk.ac.nott.mrl.homework.client.model.Item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class DeviceMetadataDialog extends Composite
{
	private static JSONObject metadata = null;
	
	private static DeviceMetadataDialogUiBinder uiBinder = GWT
			.create(DeviceMetadataDialogUiBinder.class);

	interface DeviceMetadataDialogUiBinder extends UiBinder<Widget, DeviceMetadataDialog> {
	}

	@UiField
	TextBox nameBox;
	
	@UiField
	ListBox typeList;
	
	@UiField
	ListBox ownerList;

	public DeviceMetadataDialog(Item item, DevicesService service)
	{
		initWidget(uiBinder.createAndBindUi(this));
		nameBox.setText(item.getName());
		typeList.setVisibleItemCount(1);
		ownerList.setVisibleItemCount(1);
		setMetadata(metadata);
		service.getMetadata(new RequestCallback()
		{	
			@Override
			public void onResponseReceived(Request request, Response response)
			{
				try
				{
					GWT.log(response.getText());
					setMetadata(JSONParser.parseStrict(response.getText()).isObject());
				} 
				catch(Exception e)
				{
					
				}
				
			}
			
			@Override
			public void onError(Request request, Throwable exception)
			{
			}
		});
	}
	
	@UiHandler("acceptButton")
	void accept(ClickEvent event)
	{
		accept();
	}
	
	@UiHandler("cancelButton")
	void cancel(ClickEvent event)
	{
		cancel();
	}
	
	protected abstract void cancel();
	
	protected abstract void accept();
	
	protected String getName()
	{
		return nameBox.getText();
	}
	
	protected String getOwner()
	{
		return ownerList.getItemText(ownerList.getSelectedIndex());
	}
	
	protected String getType()
	{
		return typeList.getItemText(typeList.getSelectedIndex());
	}
	
	private void setMetadata(JSONObject newMetadata)
	{
		metadata = newMetadata;
		if(metadata != null)
		{
			typeList.clear();
			JSONArray types = newMetadata.get("types").isArray();
			for(int index = 0; index < types.size(); index++)
			{
				typeList.addItem(types.get(index).isString().stringValue());
			}
			ownerList.clear();
			JSONObject owners = metadata.get("owners").isObject();
			for(String key: owners.keySet())
			{
				ownerList.addItem(key);
			}
		}
	}
}
