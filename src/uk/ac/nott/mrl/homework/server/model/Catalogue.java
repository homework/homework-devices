package uk.ac.nott.mrl.homework.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

public class Catalogue
{
	@Expose
	private Map<String,List<String>> subjects = new HashMap<String, List<String>>();
	@Expose	
	private Map<String,String> bundlelookup = new HashMap<String, String>();
	@Expose	
	private Map<String, Map<String, String>> devices = new HashMap<String, Map<String,String>>();
	
	public void addSubject(String subject, String ip)
	{
		List<String> ips = subjects.get(subject);
		if(ips == null)
		{
			ips = new ArrayList<String>();
			subjects.put(subject, ips);
		}
		ips.add(ip);
	}
	
	public void addBundle(String id, String bundle)
	{
		bundlelookup.put(id, bundle);
	}
	
	public void addName(String ip, String name)
	{
		Map<String, String> metadata = devices.get(ip);
		if(metadata == null)
		{
			metadata = new HashMap<String, String>();
			devices.put(ip, metadata);
		}
		metadata.put("name", name);
	}
}
