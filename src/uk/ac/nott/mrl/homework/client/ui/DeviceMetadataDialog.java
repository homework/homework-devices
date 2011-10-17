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
import com.google.gwt.user.client.Window;
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
	
	private final Item item;
	
	private final void selectItem(final ListBox list, final String item)
	{
		GWT.log("Find " + item);
		for(int index = 0; index < list.getItemCount(); index ++)
		{
			GWT.log(list.getValue(index));
			if(item.toLowerCase().equals(list.getValue(index).toLowerCase()))
			{
				GWT.log("Set selected index " + index + ": " + list.getValue(index));
				list.setSelectedIndex(index);
				return;
			}
		}
	}

	public DeviceMetadataDialog(Item item, DevicesService service)
	{
		initWidget(uiBinder.createAndBindUi(this));
		this.item = item;
		nameBox.setText(item.getName());
		nameBox.setText(Window.Navigator.getUserAgent());
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
		
		nameBox.setFocus(true);
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
			if(item.getType() != null)
			{
				selectItem(typeList, item.getType());
			}
			if(item.getOwner() != null)
			{
				selectItem(ownerList, item.getOwner());
			}
			nameBox.setFocus(true);
		}
	}
}
