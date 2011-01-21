package uk.ac.nott.mrl.homework.server;

import org.junit.Test;

import uk.ac.nott.mrl.homework.server.model.Companies;

public class CompanyTest
{
	@Test
	public void testCompany()
	{
		final Companies companies = new Companies();
		System.out.println(companies.getCompany("00:0b:85:92:66:af"));
		System.out.println(companies.getCompany("00:1d:e0:92:66:af"));
	}
}
