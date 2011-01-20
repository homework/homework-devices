package uk.ac.nott.mrl.homework.server.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class Convert
{
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	private static final DateFormat dayFormat = new SimpleDateFormat("dd");	
	private static final DateFormat outFormat = new SimpleDateFormat("yyyyMMdd");	
	
	private interface Converter
	{
		boolean shouldConvertFile(File file);
		
		String convertLine(String line);
		
		String getHeader();
		
		Date getDate(String line) throws ParseException;
	}
	
	private static class FlowConverter implements Converter
	{

		@Override
		public boolean shouldConvertFile(File file)
		{
			return file.getName().startsWith("flow") && file.getName().endsWith(".txt");
		}
		
		public Date getDate(String line) throws ParseException
		{
			final String dateString = line.substring(0,10);
			return dateFormat.parse(dateString);
		}
		
		public String getHeader()
		{
			return "Timestamp,Protocol,Source IP Address,Destination IP Address,Source Port,Destination Port,Application,Number of Packets,Number of Bytes";
		}

		@Override
		public String convertLine(String line)
		{
			return line.substring(0,10) + " " + line.substring(11, 19) + ","
			+ line.substring(20).replaceAll(":", ",");
		}		
	}
	
	private static class LeaseConverter implements Converter
	{

		@Override
		public boolean shouldConvertFile(File file)
		{
			return file.getName().startsWith("lease") && file.getName().endsWith(".txt");
		}
		
		public Date getDate(String line) throws ParseException
		{
			final String dateString = line.substring(0,10);
			return dateFormat.parse(dateString);
		}
		
		public String getHeader()
		{
			return "Timestamp,Action,MAC Address,IP Address,Host Name";
		}

		@Override
		public String convertLine(String line)
		{
			return line.substring(0, 10) + " " + line.substring(11, 19) + ","
			+ line.substring(20).replaceAll(";", ",");
		}		
	}	
	
	/**
	 * @param args
	 */
	public static void main(final String[] args)
	{
		try
		{
			// Convert Flows
			final File dataDirectory = new File("etc/data");
			final File[] files = dataDirectory.listFiles();
			final Collection<Converter> converters = new ArrayList<Converter>();

			converters.add(new FlowConverter());
			converters.add(new LeaseConverter());			
			
			for (final File file : files)
			{
				for(final Converter converter: converters)
				{
					if(converter.shouldConvertFile(file))
					{
						convert(converter, file);
					}
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

//	if (file.getName().startsWith("flow") && file.getName().endsWith(".txt"))
//	{
//		convertFlow(file);
//	}
//	else if (file.getName().startsWith("lease") && file.getName().endsWith(".txt"))
//	{
//		convertLease(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
//	}
//	else if (file.getName().startsWith("link") && file.getName().endsWith(".txt"))
//	{
//		convertLink(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
//	}
//	else if (file.getName().startsWith("sys") && file.getName().endsWith(".txt"))
//	{
//		convertSystemEvents(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
//	}
//	else if (file.getName().startsWith("userevent") && file.getName().endsWith(".txt"))
//	{
//		convertUserEvents(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
//	}
	
	private static void convert(final Converter converter, final File file)
	{
		System.out.println("Converting " + file.getName() + " with " + converter.getClass().getSimpleName());
		try
		{
			File outFile = null;
			Date currentDate = null;	
			String fileName = file.getName().substring(0, file.getName().length() - 4);
			BufferedWriter writer = null;

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final Date lineDate = converter.getDate(line);
				if(!lineDate.equals(currentDate))
				{
					if(writer != null)
					{
						writer.flush();
						writer.close();						
					}
					
					if(fileName.endsWith(dayFormat.format(lineDate)))
					{
						fileName = fileName.substring(0,fileName.length() - 2);
					}
					
					outFile = new File("etc/data/" + fileName + outFormat.format(lineDate) + ".csv");
					System.out.println("Writing outfile " + outFile.getName());
					
					outFile.createNewFile();
					currentDate = lineDate;
					writer = new BufferedWriter(new FileWriter(outFile));
					writer.write(converter.getHeader());
					writer.newLine();					
				}
				
				writer.write(converter.convertLine(line));
				writer.newLine();
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}	
	}

//	private static final void convertLease(final File file, final File outFile)
//	{
//		try
//		{
//			outFile.createNewFile();
//			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
//			writer.write("Timestamp,Action,MAC Address,IP Address,Host Name");
//			writer.newLine();
//
//			final BufferedReader reader = new BufferedReader(new FileReader(file));
//			String line = null;
//			while ((line = reader.readLine()) != null)
//			{
//				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
//						+ line.substring(20).replaceAll(";", ",");
//				writer.write(outline);
//				writer.newLine();
//			}
//
//			writer.flush();
//			writer.close();
//		}
//		catch (final Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	private static final void convertLink(final File file, final File outFile)
//	{
//		try
//		{
//			outFile.createNewFile();
//			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
//			writer.write("Timestamp,MAC Address,RSSI,Retry Count,Number of Packets,Number of Bytes");
//			writer.newLine();
//
//			final BufferedReader reader = new BufferedReader(new FileReader(file));
//			String line = null;
//			while ((line = reader.readLine()) != null)
//			{
//				final String timestamp = line.substring(0, 10) + " " + line.substring(11, 19);
//				String macAddress = line.substring(20, line.indexOf(';'));
//				while(macAddress.length() < 12)
//				{
//					macAddress = "0" + macAddress;
//				}
//				
//				macAddress = macAddress.substring(0,2) + ":" + macAddress.substring(2,4) + ":" + macAddress.substring(4,6) + ":" + macAddress.substring(6,8) + ":" + macAddress.substring(8,10) + ":" + macAddress.substring(10);		
//				
//				String outline = timestamp + "," + macAddress + line.substring(line.indexOf(';')).replaceAll(";", ",");
//				writer.write(outline);
//				writer.newLine();
//			}
//
//			writer.flush();
//			writer.close();
//		}
//		catch (final Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	private static final void convertSystemEvents(final File file, final File outFile)
//	{
//		try
//		{
//			outFile.createNewFile();
//			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
//			writer.write("Timestamp,Event");
//			writer.newLine();
//
//			final BufferedReader reader = new BufferedReader(new FileReader(file));
//			String line = null;
//			while ((line = reader.readLine()) != null)
//			{
//				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
//						+ line.substring(20).replaceAll(";", ",");
//				writer.write(outline);
//				writer.newLine();
//			}
//
//			writer.flush();
//			writer.close();
//		}
//		catch (final Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	private static final void convertUserEvents(final File file, final File outFile)
//	{
//		try
//		{
//			outFile.createNewFile();
//			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
//			writer.write("Timestamp,Application,Type,Payload");
//			writer.newLine();
//
//			final BufferedReader reader = new BufferedReader(new FileReader(file));
//			String line = null;
//			while ((line = reader.readLine()) != null)
//			{
//				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
//						+ line.substring(20).replaceAll(";", ",");
//				writer.write(outline);
//				writer.newLine();
//			}
//
//			writer.flush();
//			writer.close();
//		}
//		catch (final Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
}