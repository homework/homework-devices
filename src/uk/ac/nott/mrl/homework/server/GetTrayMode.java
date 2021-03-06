package uk.ac.nott.mrl.homework.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
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
		try
		{
			// if (PollingThread.trayPlugged = false) { return; }

			String filepath = getInitParameter("filepath");
			if (filepath == null)
			{
				filepath = "/home/homenet/homeworkduino/res/role.cfg";
			}

			final File file = new File(filepath);
			if (file.exists())
			{
				final BufferedReader reader = new BufferedReader(new FileReader(file));
				final String mode = reader.readLine();
				logger.info("Current Ashtray Mode: " + mode);
				response.getWriter().write(mode);
			}
		}
		catch (final Exception e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}
}