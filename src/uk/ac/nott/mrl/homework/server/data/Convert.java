package uk.ac.nott.mrl.homework.server.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Convert
{

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

			for (final File file : files)
			{
				if (file.getName().startsWith("flow") && file.getName().endsWith(".txt"))
				{
					convertFlow(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
				}
				else if (file.getName().startsWith("lease") && file.getName().endsWith(".txt"))
				{
					convertLease(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
				}
				else if (file.getName().startsWith("link") && file.getName().endsWith(".txt"))
				{
					convertLink(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
				}
				else if (file.getName().startsWith("sys") && file.getName().endsWith(".txt"))
				{
					convertSystemEvents(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
				}
				else if (file.getName().startsWith("userevent") && file.getName().endsWith(".txt"))
				{
					convertUserEvents(file, new File("etc/data/" + file.getName().replace(".txt", ".csv")));
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static final void convertFlow(final File file, final File outFile)
	{
		try
		{
			outFile.createNewFile();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write("Timestamp,Protocol,Source IP Address,Destination IP Address,Source Port,Destination Port,Application,Number of Packets,Number of Bytes");
			writer.newLine();

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
						+ line.substring(20).replaceAll(":", ",");
				writer.write(outline);
				writer.newLine();
			}

			writer.flush();
			writer.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static final void convertLease(final File file, final File outFile)
	{
		try
		{
			outFile.createNewFile();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write("Timestamp,Action,MAC Address,IP Address,Host Name");
			writer.newLine();

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
						+ line.substring(20).replaceAll(";", ",");
				writer.write(outline);
				writer.newLine();
			}

			writer.flush();
			writer.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static final void convertLink(final File file, final File outFile)
	{
		try
		{
			outFile.createNewFile();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write("Timestamp,MAC Address,RSSI,Retry Count,Number of Packets,Number of Bytes");
			writer.newLine();

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final String timestamp = line.substring(0, 10) + " " + line.substring(11, 19);
				String macAddress = line.substring(20, line.indexOf(';'));
				while(macAddress.length() < 12)
				{
					macAddress = "0" + macAddress;
				}
				
				macAddress = macAddress.substring(0,2) + ":" + macAddress.substring(2,4) + ":" + macAddress.substring(4,6) + ":" + macAddress.substring(6,8) + ":" + macAddress.substring(8,10) + ":" + macAddress.substring(10);		
				
				String outline = timestamp + "," + macAddress + line.substring(line.indexOf(';')).replaceAll(";", ",");
				writer.write(outline);
				writer.newLine();
			}

			writer.flush();
			writer.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static final void convertSystemEvents(final File file, final File outFile)
	{
		try
		{
			outFile.createNewFile();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write("Timestamp,Event");
			writer.newLine();

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
						+ line.substring(20).replaceAll(";", ",");
				writer.write(outline);
				writer.newLine();
			}

			writer.flush();
			writer.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static final void convertUserEvents(final File file, final File outFile)
	{
		try
		{
			outFile.createNewFile();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write("Timestamp,Application,Type,Payload");
			writer.newLine();

			final BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				final String outline = line.substring(0, 10) + " " + line.substring(11, 19) + ","
						+ line.substring(20).replaceAll(";", ",");
				writer.write(outline);
				writer.newLine();
			}

			writer.flush();
			writer.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
}