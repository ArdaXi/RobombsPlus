package robombs.master;

import java.io.IOException;
import java.net.ServerSocket;

public class MasterServer {
	public static void main(String[] args)
	{
		ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            serverSocket = new ServerSocket(4444);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 4444.");
            System.exit(-1);
        }

        try {
			while (listening)
				new ServerThread(serverSocket.accept()).start();
			
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
