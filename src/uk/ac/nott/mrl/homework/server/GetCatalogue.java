package uk.ac.nott.mrl.homework.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hwdb.srpc.Connection;

import uk.ac.nott.mrl.homework.server.model.Catalogue;
import uk.ac.nott.mrl.homework.server.model.Metadata;
import uk.ac.nott.mrl.homework.server.model.ResultSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GetCatalogue extends HttpServlet
{
	// private final static Logger logger = Logger.getLogger(GetChanges.class.getName());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final Catalogue catalogue = new Catalogue();
		final Connection connection = ModelController.createRPCConnection();
		final String userResults = connection.call("SQL:select * from Users");
		if (userResults != null)
		{
			ResultSet result = new ResultSet(userResults);
			for(String[] columns: result)
			{
				catalogue.addSubject(columns[2], columns[1]);
			}
		}
		catalogue.addSubject("any", "*");

		final String nameResults = connection.call("SQL:select * from DeviceNames");
		if (nameResults != null)
		{
			ResultSet result = new ResultSet(nameResults);
			for(String[] columns: result)
			{
				catalogue.addName(columns[1], columns[2]);
			}
		}

		final String typeResults = connection.call("SQL:select * from DeviceTypes");
		if (typeResults != null)
		{
			ResultSet result = new ResultSet(typeResults);
			for(String[] columns: result)
			{
				catalogue.addBundle(columns[1], columns[2].toLowerCase() + "bundle");
			}
		}		
		
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		
		try
		{
			final File file = new File(getServletContext().getRealPath("metadata.json"));
			final Metadata metadata = gson.fromJson(new FileReader(file), Metadata.class);
			for(String key: metadata.getOwners().keySet())
			{
				catalogue.addBundle(key, metadata.getOwners().get(key));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}		
		
		final String resultString = gson.toJson(catalogue);
		response.setContentType("application/json");		
		response.getWriter().println(resultString);
	}
}