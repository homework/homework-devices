package uk.ac.nott.mrl.homework.client.model;

import com.google.gwt.core.client.JavaScriptObject;

public class Item extends JavaScriptObject
{
	public enum State
	{
		added, updated, active, dead, inactive
	}

	public static final String getShortCompanyName(final Link link)
	{
		String company = link.getCorporation();
		if (company != null)
		{
			int cut = company.indexOf(' ');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}

			cut = company.indexOf(',');
			if (cut != -1)
			{
				company = company.substring(0, cut);
			}
			return company;
		}
		return "Unknown";
	}

	public static final String getTypeID(final Link link, final Zone zone)
	{
		final String company = getShortCompanyName(link).toLowerCase();
		return zone.getName() + ":" + company;
	}

	protected Item()
	{
	}

	public final native String getChange()
	/*-{
		return this.change;
	}-*/;

	public final native String getCompany()
	/*-{
		return this.company;
	}-*/;

	public final native String getID()
	/*-{
		return this.id;
	}-*/;

	public final native String getIPAddress()
	/*-{
		return this.ipAddress;
	}-*/;

	public final native String getMacAddress()
	/*-{
		return this.macAddress;
	}-*/;

	public final native String getName()
	/*-{
		return this.name;
	}-*/;

	public final float getOpacity()
	{

		return 1f;
	}

	public final native String getState()
	/*-{
		return this.state;
	}-*/;

	public final native double getTimestamp()
	/*-{
		return this.timestamp;
	}-*/;

	public final native void setState(String state)
	/*-{
		this.state = state;
	}-*/;
}