package uk.ac.nott.mrl.homework.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetTrayMode extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(GetTrayMode.class.getName());
	
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		String filepath = getInitParameter("filepath");
		if (filepath == null)
		{
			filepath = "/home/homenet/homeworkduino/res/role.cfg";
		}

		final File file = new File(filepath);
		if(file.exists())
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String mode = reader.readLine();
			Log.log("Get Tray Mode", mode);
			logger.info("Current Ashtray Mode: " + mode);
			response.getWriter().write(mode);
		}
	}
}