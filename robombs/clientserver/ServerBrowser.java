package robombs.clientserver;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import robombs.master.ServerEntry;

/**
 * Simple server browser that listens on a specified UDP port for servers
 * broadcasting their connection data on that port.
 */
public class ServerBrowser {

	private int port = 0;
	private boolean exit = false;
	private boolean running = false;
	private String masterServer = "";
	private List<ServerEntry> servers = new ArrayList<ServerEntry>();
	private List<DataChangeListener> dataChangeListener = new ArrayList<DataChangeListener>();

	/**
	 * Creates a new ServerBrowser that will listen on a specified UDP-port for
	 * servers to broadcast their data. This prepares the browser, it doesn't
	 * start it.
	 * 
	 * @param port
	 *            the UDP port to use
	 */
	public ServerBrowser(int port) {
		this.port = port;
	}

	/**
	 * Adds a new DataChangesListener to the browser. The listener will be
	 * notified if the server list changes, i.e. if servers get removed or
	 * added.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addListener(DataChangeListener listener) {
		dataChangeListener.add(listener);
	}

	/**
	 * Starts the browser.
	 */
	public void startBrowser() {
		new Thread(new NetBrowserThread()).start();
		new Thread(new LANBrowserThread()).start();
	}

	/**
	 * Stops the browser.
	 */
	public void stopBrowser() {
		exit = true;
	}

	/**
	 * Returns the current list of servers that this browser has found.
	 * 
	 * @return List the server list
	 */
	public List<ServerEntry> getServerList() {
		synchronized (servers) {
			return new ArrayList<ServerEntry>(servers);
		}
	}

	/**
	 * Notify the listeners...
	 */
	private void fireEvent() {
		for (Iterator<DataChangeListener> itty = dataChangeListener.iterator(); itty
				.hasNext();) {
			DataChangeListener dcl = itty.next();
			dcl.dataChanged(getServerList());
		}
	}

	private class NetBrowserThread implements Runnable {

		public void run() {
			try {
				Socket socket = new Socket(InetAddress.getByName(masterServer),
						4444);
				PrintWriter out = new PrintWriter(socket.getOutputStream(),
						true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				out.println("client");
				out.println("query");
				String line;
				while ((line = in.readLine()) != "EOF") {
					String[] data = line.split("\\|");
					servers.add(new ServerEntry(URLDecoder.decode(data[0],
							"UTF-8"), InetAddress.getByName(URLDecoder.decode(
							data[1], "UTF-8")), Integer.parseInt(URLDecoder
							.decode(data[2], "UTF-8")), Integer
							.parseInt(URLDecoder.decode(data[3], "UTF-8")),
							true));
				}
				while (!exit) {
					out.println("update");
					line = in.readLine();
					String[] data = line.split("\\|");
					ServerEntry se = new ServerEntry(URLDecoder.decode(data[0],
							"UTF-8"), InetAddress.getByName(URLDecoder.decode(
							data[1], "UTF-8")), Integer.parseInt(URLDecoder
							.decode(data[2], "UTF-8")), Integer
							.parseInt(URLDecoder.decode(data[3], "UTF-8")),
							true);
					boolean found = false;
					synchronized (servers) {
						for (Iterator<ServerEntry> itty = servers.iterator(); itty
								.hasNext();) {
							ServerEntry st = itty.next();
							if (st.equals(se)) {
								st.setClientCount(se.getClientCount());
								st.touch();
								if (se.getClientCount() == -9999) {
									// -9999 flags that the server is going
									// down.
									itty.remove();
									st.setClientCount(0);
									NetLogger.log("ServerBrowser: Server "
											+ st.getName()
											+ " removed by server's request!");
								}
								found = true;
							}
						}

						if (!found && se.getClientCount() != -9999) {
							// Don't add it again, if it was a removal request
							NetLogger.log("ServerBrowser: Server "
									+ se.getName() + " added!");
							servers.add(se);
						}
						for (Iterator<ServerEntry> itty = servers.iterator(); itty
								.hasNext();) {
							ServerEntry st = itty.next();
							if (st.isOld()) {
								NetLogger.log("ServerBrowser: Server "
										+ st.getName() + " removed!");
								itty.remove();
							}
						}
						fireEvent();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * The thread that listens on the port for LAN servers to send their data.
	 */
	private class LANBrowserThread implements Runnable {

		public void run() {
			try {
				running = true;
				DatagramSocket bsock = new DatagramSocket(port);
				bsock.setSoTimeout(3000);
				byte[] buffer = new byte[1000];
				DatagramPacket dpr = new DatagramPacket(buffer, buffer.length);
				while (!exit) {
					boolean ok = false;
					try {
						bsock.receive(dpr);
						ok = true;
					} catch (SocketTimeoutException e) {
					}

					if (ok) {
						byte[] data = new byte[dpr.getLength() - 2];
						System
								.arraycopy(dpr.getData(), 2, data, 0,
										data.length);
						DataContainer dc = new DataContainer(data, false);
						ServerEntry se = new ServerEntry(dc.getNextString(),
								dpr.getAddress(), dc.getNextInt(), dc
										.getNextInt(), false);
						boolean found = false;
						synchronized (servers) {
							for (Iterator<ServerEntry> itty = servers
									.iterator(); itty.hasNext();) {
								ServerEntry st = itty.next();
								if (st.equals(se)) {
									st.setClientCount(se.getClientCount());
									st.touch();
									if (se.getClientCount() == -9999) {
										// -9999 flags that the server is going
										// down.
										itty.remove();
										st.setClientCount(0);
										NetLogger
												.log("ServerBrowser: Server "
														+ st.getName()
														+ " removed by server's request!");
									}
									found = true;
								}
							}

							if (!found && se.getClientCount() != -9999) {
								// Don't add it again, if it was a removal
								// request
								NetLogger.log("ServerBrowser: Server "
										+ se.getName() + " added!");
								servers.add(se);
							}
						}
					}
					synchronized (servers) {
						for (Iterator<ServerEntry> itty = servers.iterator(); itty
								.hasNext();) {
							ServerEntry st = itty.next();
							if (st.isOld()) {
								NetLogger.log("ServerBrowser: Server "
										+ st.getName() + " removed!");
								itty.remove();
							}
						}
						fireEvent();
					}
				}
			} catch (Exception e) {
				NetLogger.log("Can't start server LAN browser due to: ");
				e.printStackTrace();
			}
			running = false;
		}
	}

	public boolean isRunning() {
		return running;
	}

}
