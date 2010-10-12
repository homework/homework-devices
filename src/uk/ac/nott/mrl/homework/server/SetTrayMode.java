package uk.ac.nott.mrl.homework.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetTrayMode extends HttpServlet
{
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String mode = request.getParameter("mode");
		System.out.println("Set Ashtray Mode :" + mode);
		
		String filepath = getInitParameter("filepath");
		if (filepath == null)
		{
			filepath = "/home/homenet/homeworkduino/res/role.cfg";
		}

		if (mode != null)
		{
			final File file = new File(filepath);
			file.createNewFile();
			final FileWriter writer = new FileWriter(file);
			writer.write(mode);
		}
		
		Log.log("Set Tray Mode", mode);
	}
}