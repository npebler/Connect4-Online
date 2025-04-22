import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;


public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	
	
	Server(){

		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			} catch(Exception e) {
					System.err.println("Server did not launch");
				}
			}
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;
				try { // try to open the streams
					out = new ObjectOutputStream(connection.getOutputStream());
					in = new ObjectInputStream(connection.getInputStream());
					connection.setTcpNoDelay(true);

					System.out.println("Client #" + count + " connected.");
				}
				catch(Exception e) {
					System.err.println("Client #" + count + " failed to connect.");
				}
			}
			
			public void updateClients(Message message) {
				for (ClientThread client : clients) {
					try {
						client.out.writeObject(message);
						client.out.flush();
					} catch (Exception e) {
						System.err.println("Error sending message to client #" + client.count + ": " + e.getMessage());
					}
				}
			}
			
			public void run(){
				try {
					while (true) {
						Message message = (Message) in.readObject();
						System.out.println("Client #" + count + " sent: " + message);
						updateClients(message);
					}
				} catch (Exception e) {
					System.err.println("Client #" + count + " disconnected.");
					clients.remove(this);
				}
		
				// try {
				// 	in = new ObjectInputStream(connection.getInputStream());
				// 	out = new ObjectOutputStream(connection.getOutputStream());
				// 	connection.setTcpNoDelay(true);	
				// }
				// catch(Exception e) {
				// 	System.out.println("Streams not open");
				// }
				
				// updateClients("new client on server: client #"+count);
					
				//  while(true) {
				// 	    try {
				// 	    	String data = in.readObject().toString();
				// 	    	System.out.println("client: " + count + " sent: " + data);
				// 	    	updateClients("client #"+count+" said: "+data);
					    	
				// 	    	}
				// 	    catch(Exception e) {
				// 	    	System.err.println("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
				// 	    	updateClients("Client #"+count+" has left the server!");
				// 	    	clients.remove(this);
				// 	    	break;
				// 	    }
				// 	}
				}//end of run
			
			
		}//end of client thread
}


	
	

	
