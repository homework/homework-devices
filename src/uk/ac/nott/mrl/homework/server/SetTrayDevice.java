package uk.ac.nott.mrl.homework.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetTrayDevice extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(GetTrayMode.class.getName());
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		final String macAddress = request.getParameter("macAddress");
		logger.info("Set Ashtray Device: " + macAddress);

		String filepath = getInitParameter("filepath");
		if (filepath == null)
		{
			filepath = "/home/homenet/homeworkduino/res/probe.cfg";
		}

		final File file = new File(filepath);
		file.createNewFile();
		final FileWriter writer = new FileWriter(file);
		writer.write(macAddress);
		writer.flush();
		writer.close();
		
		response.getWriter().write(macAddress);		

		Log.log("Set Tray Device", macAddress);
	}
}