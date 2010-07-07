package robombs.master;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerThread extends Thread {

	private Socket socket = null;
	private ServerList servers = null;
	private BlockingQueue<ServerEntry> queue = null;

	public ServerThread(Socket socket) {
		this.socket = socket;
		this.servers = ServerList.get();
		setDaemon(true);
	}

	public void run() {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			out.println("ready");
			String type = in.readLine();
			while (socket.isClosed() == false) {
				if (type == "server") {
					ServerEntry se = ServerEntry.parse(in.readLine());
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
								}
								found = true;
							}
						}

						if (!found && se.getClientCount() != -9999) {
							// Don't add it again, if it was a removal request
							servers.add(se);
						}
					}
					synchronized (servers) {
						for (Iterator<ServerEntry> itty = servers.iterator(); itty
								.hasNext();) {
							ServerEntry st = itty.next();
							if (st.isOld()) {
								itty.remove();
							}
						}
					}
				} else if (type == "client")
				{
					String request = in.readLine();
					if(request == "query")
					{
						synchronized(servers)
						{
							for(ServerEntry se : servers)
							{
								out.println(se);
							}
							queue = new LinkedBlockingQueue<ServerEntry>();
						}
						out.println("EOF");
					}
					else if (request == "update")
					{
						ServerEntry se = queue.take();
						out.println(se);
					}
				}
				sleep(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void put(ServerEntry se) throws InterruptedException
	{
		if(queue == null) return;
		queue.put(se);
	}
}
