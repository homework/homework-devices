package uk.ac.nott.mrl.homework.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NextTrayMode extends HttpServlet
{
	//private static final Logger logger = Logger.getLogger(NextTrayMode.class.getName());

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
		int nextMode = 0;
		if (file.exists())
		{
			final BufferedReader reader = new BufferedReader(new FileReader(file));
			final String mode = reader.readLine();
			nextMode = (Integer.parseInt(mode) + 1) % 3;
		}
		file.createNewFile();
		final FileWriter writer = new FileWriter(file);
		final String nextModeString = Integer.toString(nextMode);
		writer.write(nextModeString);
		writer.flush();
		writer.close();

		response.getWriter().write(nextModeString);

		Log.log("Set Tray Mode", request.getRemoteAddr() + ": " +  nextModeString);
	}
}