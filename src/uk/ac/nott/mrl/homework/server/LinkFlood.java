package uk.ac.nott.mrl.homework.server;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hwdb.srpc.Connection;

public class LinkFlood extends HttpServlet
{
	private final static int count = 100;
	private final static Logger logger = Logger.getLogger(LinkFlood.class.getName());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
			IOException
	{

	}
	
	@Override
	public void init() throws ServletException
	{
		super.init();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final Random random = new Random();				
				logger.info("Starting Link Flooding");
				while (true)
				{
					Connection connection = null;
					try
					{
						connection = ModelController.createRPCConnection();
						while (true)
						{
							for(int index = 0; index < count; index++)
							{
								final String randomMac = String.format(	"%02x%02x%02x%02x%02x%02x", random.nextInt(256),
																		random.nextInt(256), random.nextInt(256),
																		random.nextInt(256), random.nextInt(256),
																		random.nextInt(256));

						
								final String query = String.format(	"SQL:INSERT into Links values (\"%s\", '0.0', '0', '0', '0')",
																	randomMac);

								final String result = connection.call(query.toString());
								if (!result.startsWith("0<|>Success"))
								{
									logger.warning("Failed query:" + query + ";" + result);
									Thread.sleep(5000);
								}
							}
							
							Thread.sleep(100);
						}
					}
					catch (final Exception e)
					{
						logger.log(Level.WARNING, e.getMessage(), e);
					}
					finally
					{
						try
						{
							if(connection != null)
							{
								connection.disconnect();
							}
						}
						catch(final Exception e)
						{
							logger.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		}).start();
	}
}