package org.hwdb.srpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Magnus Morton
 */
public class SRPC
{

	private class Cleaner extends TimerTask
	{

		@Override
		public void run()
		{
			// scan connection table
			for (final Connection conn : connectionTable.values())
			{
				try
				{
					conn.checkStatus();
				}
				catch (final IOException e)
				{
					System.err.println(e.getMessage());
				}
				// purge timed out connections
				if (conn.isTimedOut())
				{
					connectionTable.remove(conn.getSource());
				}
			}

		}
	}

	/**
	 * Thread which continually reads from the channel, and hands off data to connections
	 */
	private class Reader implements Runnable
	{

		@Override
		public void run()
		{
			while (channel.isOpen())
			{
				try
				{

					final ByteBuffer inBuffer = ByteBuffer.allocate(FRAGMENT_SIZE * 10);
					final InetSocketAddress addr = (InetSocketAddress) channel.receive(inBuffer);
					inBuffer.rewind();
					final Payload payload = PayloadFactory.create(inBuffer);
					final Endpoint endpoint = new Endpoint(addr.getAddress(), addr.getPort(), payload.getSubport());
					Connection conn = connectionTable.get(endpoint);
					// if CONNECT and connection doesn't exist
					if (payload.getCommand() == Command.CONNECT && conn == null)
					{
						final String serviceName = ((ConnectPayload) payload).getService();
						final Service service = serviceTable.get(serviceName);

						if (service != null)
						{
							conn = new Connection(SRPC.this, endpoint, service);
							connectionTable.put(endpoint, conn);
							conn.commandReceived(payload);
						}
						// if service is null ignore connection
					}
					else if (conn != null)
					{
						conn.commandReceived(payload);
					}
					else
					{
						// logger.info( "Unknown connection");
					}

				}
				catch (final IOException e)
				{
					// TODO - decide what to do here - throw runtimme exception or log output
					System.err.println(e.getMessage());

				}

			}
		}
	}

	private ConcurrentMap<Endpoint, Connection> connectionTable;
	private ConcurrentMap<String, Service> serviceTable;
	private DatagramChannel channel;

	private int counter;
	private int seed;
	static Charset charset = Charset.forName("UTF-8");
	static CharsetDecoder decoder = charset.newDecoder();
	static CharsetEncoder encoder = charset.newEncoder();
	// public static Logger logger = Logger.getLogger("SRPC");
	static final int TICK_LENGTH = 20;
	static final int FRAGMENT_SIZE = 1024;
	static final int TICKS = 2;
	static final int ATTEMPTS = 7;

	static final int TICKS_BETWEEN_PINGS = (60 * 50);

	static final int PINGS_BEFORE_PURGE = 3;

	/**
	 * Default constructor
	 * 
	 * Binds to ephemeral port. Equivalent to rpc_init
	 * 
	 * @throws IOException
	 *             if channel error occurs
	 */
	public SRPC() throws IOException
	{
		channelInit();
		channel.socket().bind(null);
		this.init();
	}

	/**
	 * Constructor
	 * 
	 * Binds the RPC system to specified port equivalent to rpc_init
	 * 
	 * @param port
	 *            the port to bind to
	 * @throws IOException
	 *             if channel error occurs
	 */
	public SRPC(final int port) throws IOException
	{
		channelInit();
		channel.socket().bind(new InetSocketAddress(port));
		this.init();
	}

	/**
	 * initialises the DatagramChannel
	 * 
	 * @throws IOException
	 *             if IO error occurs
	 */
	private void channelInit() throws IOException
	{
		channel = DatagramChannel.open();
		channel.configureBlocking(true);

	}

	/**
	 * Connects to remote RPC system
	 * 
	 * @param address
	 *            the ip address to connect to
	 * @param port
	 *            the port number to connect to
	 * @param service
	 *            the name of the service to connect to
	 * @return a Connection object representing the connection
	 * @throws IOException
	 *             if connection error
	 */
	public Connection connect(final byte[] address, final int port, final String service) throws IOException
	{
		final InetAddress addr = InetAddress.getByAddress(address);
		final Endpoint endpoint = new Endpoint(addr, port, newSubport());
		final Connection conn = new Connection(this, endpoint, null);
		connectionTable.put(endpoint, conn);
		conn.connect(service);
		return conn;
	}

	/**
	 * Connects to remote RPC system
	 * 
	 * @param host
	 *            the hostname to connect to
	 * @param port
	 *            the port number to connect to
	 * @param service
	 *            the name of the service to connect to
	 * @return a Connection object representing the connection
	 * @throws IOException
	 *             if connection error
	 */
	public Connection connect(final String host, final int port, final String service) throws IOException
	{
		final InetAddress addr = InetAddress.getByName(host);
		final Endpoint endpoint = new Endpoint(addr, port, newSubport());
		final Connection conn = new Connection(this, endpoint, null);
		connectionTable.put(endpoint, conn);
		conn.connect(service);
		return conn;
	}

	/**
	 * Returns the details of the local connection
	 * 
	 * @return an InetSocketAddress object (containing address & port)
	 */
	public InetSocketAddress details()
	{
		return (InetSocketAddress) channel.socket().getLocalSocketAddress();

	}

	/**
	 * Gets the DatagramChannel
	 * 
	 * @return the channel
	 */
	protected DatagramChannel getChannel()
	{
		return this.channel;
	}

	/**
	 * initialises connection & service tables. Also sparks off threads
	 */
	private void init()
	{
		// logger.setLevel(Level.OFF);
		connectionTable = new ConcurrentHashMap<Endpoint, Connection>();
		serviceTable = new ConcurrentHashMap<String, Service>();
		seed = new Random().nextInt();

		// spark off reader & timer threads
		new Timer().schedule(new Cleaner(), TICK_LENGTH, TICK_LENGTH);
		new Thread(new Reader()).start();

	}

	/**
	 * Looks up a service name
	 * 
	 * @param service
	 *            the service name
	 * @return the service if it exists, null otherwise
	 */
	protected Service lookupService(final String service)
	{
		return serviceTable.get(service);
	}

	/**
	 * 
	 * @return a probably unique number which can be used as a subport
	 */
	protected synchronized int newSubport()
	{
		if (++counter > 0x7FFF)
		{
			counter = 1;
		}

		return (seed & 0xFFFF) << 16 | counter;
	}

	/**
	 * Creates a service which is offered to clients
	 * 
	 * @param serviceName
	 *            the name of the service
	 * @return a new Service object representing the service
	 */
	public Service offer(final String serviceName)
	{
		final Service service = new Service(serviceName);
		serviceTable.put(serviceName, service);
		return service;
	}

}
