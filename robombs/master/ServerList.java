package robombs.master;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ServerList implements Iterable<ServerEntry> {
	
	private static ServerList instance = null;
	private List<ServerEntry> servers = new ArrayList<ServerEntry>();
	private List<ServerThread> clients = new ArrayList<ServerThread>();
	
	private ServerList()
	{
		
	}
	
	public static ServerList get()
	{
		if (instance == null)
			instance = new ServerList();
		return instance;
	}
	
	public Iterator<ServerEntry> iterator()
	{
		return servers.iterator();
	}
	
	public void add(ServerEntry se) throws InterruptedException
	{
		servers.add(se);
		for(ServerThread client : clients)
		{
			client.put(se);
		}
	}
	
	public void addClient(ServerThread client)
	{
		clients.add(client);
	}
	
	public void removeClient(ServerThread client)
	{
		clients.remove(client);
	}
}
