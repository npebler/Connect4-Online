import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;



public class Client extends Thread{

	Socket socketClient;
	
	ObjectOutputStream out;
	ObjectInputStream in;

	private Consumer<Message> callback;
	
	Client(Consumer<Message> call){
	
		callback = call;
	}

	
	public void run() {
		
		try {
			socketClient= new Socket("127.0.0.1",5555);
	    	out = new ObjectOutputStream(socketClient.getOutputStream());
	    	in = new ObjectInputStream(socketClient.getInputStream());
	   	 	socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			System.err.println("Error connecting to server: " + e.getMessage());
		}
		
		while(true) {
			try {
				Message message = (Message) in.readObject();
				callback.accept(message);
			}
			catch(Exception e) {
				System.err.println("Error reading message: " + e.getMessage());
			}
		}
    }
	
	public void send(Message message) {
		try {
			out.writeObject(message);
			out.flush(); // send immediately
		} catch (IOException e) {
			System.err.println("Error sending message: " + e.getMessage());
		}
	}


	public Message receive() {
		try {
			return (Message) in.readObject(); // receive message from the server
		} catch (Exception e) {
			System.err.println("Error receiving message: " + e.getMessage());
			return null;
		}
	}


}
