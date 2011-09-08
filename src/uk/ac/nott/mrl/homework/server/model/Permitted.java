package uk.ac.nott.mrl.homework.server.model;

import java.util.List;

public class Permitted
{
	private List<String> permitted;
	private List<String> denied;

	public Permitted()
	{

	}

	public Iterable<String> denied()
	{
		return denied;
	}

	public Iterable<String> permitted()
	{
		return permitted;
	}
}
