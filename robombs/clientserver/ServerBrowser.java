package robombs.clientserver;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Simple server browser that listens on a specified UDP port for servers broadcasting their connection data on that port.
 */
public class ServerBrowser {

    private int port=0;
    private boolean exit=false;
    private String masterServerURL = "http://robombs.arienh4.net/list.php";
    private URL masterServer;
    private boolean running=false;
    private List<ServerEntry> servers=new ArrayList<ServerEntry>();
    private List<DataChangeListener> dataChangeListener=new ArrayList<DataChangeListener>();

    /**
     * Creates a new ServerBrowser that will listen on a specified UDP-port for servers to broadcast their data. This prepares the browser, it doesn't start it.
     * @param port the UDP port to use
     */
    public ServerBrowser(int port) {
        this.port=port;
        try {
			masterServer = new URL(masterServerURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    /**
     * Adds a new DataChangesListener to the browser. The listener will be notified if the server list changes, i.e.
     * if servers get removed or added.
     * @param listener the listener
     */
    public void addListener(DataChangeListener listener) {
        dataChangeListener.add(listener);
    }

    /**
     * Starts the LAN browser.
     */
    public void startBrowser() {
        new Thread(new LANBrowserThread()).start();
        new Thread(new MasterBrowserThread()).start();
    }

    /**
     * Stops the LAN browser.
     */
    public void stopBrowser() {
        exit=true;
    }

    /**
     * Returns the current list of servers that this browser has found.
     * @return List the server list
     */
    public List<ServerEntry> getServerList() {
        synchronized(servers) {
            return new ArrayList<ServerEntry>(servers);
        }
    }

    /**
     * Notify the listeners...
     */
    private void fireEvent() {
        for (Iterator<DataChangeListener> itty = dataChangeListener.iterator(); itty.hasNext(); ) {
            DataChangeListener dcl = itty.next();
            dcl.dataChanged(getServerList());
        }
    }

    /**
     * The thread that checks with the master server for Internet servers.
     */
    
    private class MasterBrowserThread implements Runnable {

		public void run() {
			try {
				while(!exit)
				{
					refreshServers();
					Thread.sleep(3000);
				}
			} catch (Exception e) {
				NetLogger.log("Can't start server Internet browser due to: ");
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
            	running=true;
            	DatagramSocket bsock=new DatagramSocket(port);
                bsock.setSoTimeout(3000);
                byte[] buffer = new byte[1000];
                DatagramPacket dpr = new DatagramPacket(buffer, buffer.length);
                while(!exit) {
                    boolean ok=false;
                    try {
                        bsock.receive(dpr);
                        ok=true;
                    }catch(SocketTimeoutException e) {
                    }

                    if (ok) {
                        byte[] data = new byte[dpr.getLength()-2];
                        System.arraycopy(dpr.getData(), 2, data, 0, data.length);
                        DataContainer dc = new DataContainer(data, false);
                        ServerEntry se = new ServerEntry(dc.getNextString(), dpr.getAddress(), dc.getNextInt(), dc.getNextInt(), false);
                        boolean found = false;
                        synchronized (servers) {
                            for (Iterator<ServerEntry> itty = servers.iterator(); itty.hasNext(); ) {
                                ServerEntry st = itty.next();
                                if (st.equals(se)) {
                                    st.setClientCount(se.getClientCount());
                                    st.touch();
                                    if (se.getClientCount()==-9999) {
                                    	// -9999 flags that the server is going down.
                                    	itty.remove();
                                    	st.setClientCount(0);
                                    	NetLogger.log("ServerBrowser: Server " + st.getName() + " removed by server's request!");
                                    }
                                    found = true;
                                }
                            }

                            if (!found && se.getClientCount()!=-9999) {
                            	// Don't add it again, if it was a removal request
                            	NetLogger.log("ServerBrowser: Server " + se.getName() + " added!");
                                servers.add(se);
                            }
                        }
                    }
                    synchronized (servers) {
                        for (Iterator<ServerEntry> itty = servers.iterator(); itty.hasNext(); ) {
                            ServerEntry st = itty.next();
                            if (st.isOld()) {
                                NetLogger.log("ServerBrowser: Server " + st.getName() + " removed!");
                                itty.remove();
                            }
                        }
                        fireEvent();
                    }
                }
            } catch(Exception e) {
                NetLogger.log("Can't start server LAN browser due to: ");
                e.printStackTrace();
            }
            running=false;
        }
    }
    
    public boolean isRunning() {
    	return running;
    }

	/**
	 * Refreshes servers from master.
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public void refreshServers() throws IOException, UnknownHostException {
		for (Iterator<ServerEntry> iterator = servers.iterator(); iterator.hasNext(); ) {
			ServerEntry entry = iterator.next();
			if(entry.isInternet())
			{
				iterator.remove();
				NetLogger.log("ServerBrowser: Server " + entry.getName() + " removed!");
			}
		}
		InputStream serverStream = masterServer.openStream();
		BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverStream));
		String serverLine;
		while((serverLine = serverReader.readLine()) != null)
		{
			String[] data = serverLine.split("\\|");
			NetLogger.log(serverLine);
			NetLogger.log("0:"+data[0]+":1:"+data[1]+":2:"+data[2]+":3:"+data[3]);
			ServerEntry entry = new ServerEntry(data[0], InetAddress.getByName(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), true);
			NetLogger.log("ServerBrowser: Server " + entry.getName() + " added!");
			servers.add(entry);
		}
	}
    
}
