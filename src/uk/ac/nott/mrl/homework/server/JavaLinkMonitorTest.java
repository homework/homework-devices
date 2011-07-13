package uk.ac.nott.mrl.homework.server;

import java.net.InetAddress;
import java.util.Date;
import java.util.logging.Logger;

import uk.ac.nott.mrl.homework.server.model.Link;

public class JavaLinkMonitorTest
{
	// private static int sig_received;
	private static int exit_status = 0;

	private static final String HWDB_SERVER_ADDR = "192.168.0.1";

	private static final short HWDB_SERVER_PORT = 987;
	private static final Logger logger = Logger.getLogger(JavaLinkMonitorTest.class.getName());

	// private static final int SOCK_RECV_BUF_LEN = 65535;

	private static int must_exit = 0;

	private static final int TIME_DELTA = 10;
	private static final String USAGE = "./linkmonitor [-h host] [-p port]";

	/**
	 * @param args
	 */
	public static void main(final String[] args)
	{
		final JavaSRPC srpc = new JavaSRPC();
		String query;
		String host;
		short port;

		host = HWDB_SERVER_ADDR;
		port = HWDB_SERVER_PORT;

		for (int i = 1; i < args.length;)
		{
			final int j = i + 1;
			if (j == args.length)
			{
				System.err.println("usage: " + USAGE);
				System.exit(-1);
			}

			if (args[i].equals("-h"))
			{
				host = args[j];
			}
			else if (args[i].equals("-p"))
			{
				port = Short.parseShort(args[j]);
			}
			else
			{
				System.err.println("Unknown flag: " + args[i] + " " + args[j]);
			}
			i = j + 1;
		}

		try
		{
			srpc.connect(InetAddress.getByName(host), port);

			Date expected = new Date();
			Date last = null;

			while (must_exit == 0)
			{
				expected = new Date(expected.getTime() + TIME_DELTA);
				if (last != null)
				{
					final String s = String.format("@%016x@", last.getTime() * 1000000);
					logger.info(s);
					query = String.format(	"SQL:select * from Links [ range %d seconds ] where timestamp > %s",
											TIME_DELTA + 1, s);
				}
				else
				{
					query = String.format("SQL:select * from Links [ range %d seconds ]", TIME_DELTA);
				}

				try
				{
					Thread.sleep(TIME_DELTA);
				}
				catch (final InterruptedException e)
				{
					e.printStackTrace();
				}

				final String result = srpc.call(query);

				Link.parseResultSet(result);

				last = new Date();

				try
				{
					Thread.sleep(2000);
				}
				catch (final Exception e)
				{
				}
			}
			srpc.disconnect();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		System.exit(exit_status);
	}
}