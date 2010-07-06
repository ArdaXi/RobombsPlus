package robombs.game;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

import org.jibble.pircbot.PircBot;

import robombs.clientserver.NetLogger;
import robombs.clientserver.ServerEntry;

public final class ServerBot extends PircBot
{
	private List<ServerEntry> servers;
	
	private static ServerBot instance = null;
	
	private ServerBot()
	{
		this.setName(System.getProperty("user.name"+"-client"));
		try {
			this.connect("irc.freenode.net");
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.joinChannel("#robombs");
	}
	
	public static ServerBot get()
	{
		if(instance == null)
			instance = new ServerBot();
		return instance;
	}
	
	public void setServers(List<ServerEntry> servers) {
		this.servers = servers;
	}
	
	public void onMessage(String channel, String sender,
            String login, String hostname, String message)
	{
		String[] data = message.split("\\|");
		try {
			ServerEntry se = new ServerEntry(URLDecoder.decode(data[0], "UTF-8"), InetAddress.getByName(URLDecoder.decode(data[1], "UTF-8")), Integer.parseInt(URLDecoder.decode(data[2], "UTF-8")), Integer.parseInt(URLDecoder.decode(data[3], "UTF-8")), true);
            synchronized (servers) {
            	boolean found = false;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
