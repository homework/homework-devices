package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.nott.mrl.homework.server.model.Model;

public class GetChanges extends HttpServlet
{
	// private final static Logger logger = Logger.getLogger(GetChanges.class.getName());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{
		response.setContentType("application/json");
		// logger.info(request.getRequestURL().toString());

		final String group = request.getParameter("group");
		final String sinceString = request.getParameter("since");
		long since = new Date().getTime() - Model.getTimeout();
		if (sinceString != null)
		{
			since = (long) Double.parseDouble(sinceString);
		}

		ModelController.updateModel();

		response.setContentType("application/json");
		if (group != null)
		{
			ModelController.listGroupItems(response.getWriter(), group);
		}
		else
		{
			// final String sinceString = request.getParameter("since");
			ModelController.listItems(response.getWriter(), since);
		}
	}
}