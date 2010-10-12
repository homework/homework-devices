package uk.ac.nott.mrl.homework.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetTrayDevice extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String macAddress = request.getParameter("macAddress");
		System.out.println("Set Ashtray Device :" + macAddress);

		String filepath = getInitParameter("filepath");
		if (filepath == null)
		{
			filepath = "/home/homenet/homeworkduino/res/probe.cfg";
		}

		if (macAddress != null)
		{
			final File file = new File(filepath);
			file.createNewFile();
			final FileWriter writer = new FileWriter(file);
			writer.write(macAddress);
		}
		
		Log.log("Set Tray Device", macAddress);		
	}
}