package uk.ac.nott.mrl.homework.server.model;

import java.util.Iterator;
import java.util.List;

public class Permitted implements Iterable<String>
{
	private List<String> permitted;
	
	public Permitted()
	{
		
	}
	
	public Iterator<String> iterator()
	{
		return permitted.iterator();
	}
}
