import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server{

	int count = 1; // number of clients
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	ArrayList<String> loginInformation = new ArrayList<String>(); // stores username:password info
	
	TheServer server;
	
	Server(){
		loadLoginInformation();
		server = new TheServer();
		server.start();
	}
	
	public class TheServer extends Thread{
		public void run() {
			try(ServerSocket mysocket = new ServerSocket(5555);) {
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

					if (message.type == MessageType.DISCONNECT) {
						System.out.println("Client #" + count + " disconnected.");
						break;
					} else if (message.type == MessageType.LOGIN) {
                        System.out.println("Processing login request");
                        loginAccount(message);
                    } else if (message.type == MessageType.CREATE_ACCOUNT) {
                        System.out.println("Processing account creation request");
                        registerAccount(message);
                    }
	
					updateClients(message);
				}
			} catch (Exception e) {
				System.err.println("Client #" + count + " disconnected unexpectedly.");
				clients.remove(this);
			} finally { // remove the client if they disconnect
				try {
					connection.close();
					in.close();
					out.close();
				} catch (Exception e) {
					System.err.println("Error closing connection for client #" + count + ": " + e.getMessage());
				}
				clients.remove(this);
				System.out.println("Client #" + count + " removed.");
			}
		}

		//
		// loginAccount
		//
		// Login to an already registered account using the username and password
		//
        private void loginAccount(Message message) {
			try {
				String[] credentials = message.message.split(":"); // split into user + password
				String username = credentials[0];
				String password = credentials[1];
				System.out.println("Login attempt for: " + username);

				boolean found = false;
				for (String cred : loginInformation) {
					String[] parts = cred.split(":");
					if (parts[0].equals(username) && parts[1].equals(password)) {
						found = true;
						break;
					}
				}
				// boolean found = loginInformation.contains(username + ":" + password); // check if there is both a username and password in the field

                if (found) {
					System.out.println("Login successful for: " + username);
                    out.writeObject(new Message("Login successful", MessageType.LOGIN));
                } else {
					System.out.println("Login failed for: " + username);
                    out.writeObject(new Message("Login failed!", MessageType.LOGIN));
                }
                out.flush();
            } catch (Exception e) {
                System.err.println("Error sending login response: " + e.getMessage());
				e.printStackTrace();
            }
        }


		//
		// registerAccount
		//
		// Register for a new account using a username and password
		//
		private void registerAccount(Message message) {
            try {
				String[] credentials = message.message.split(":");
				String username = credentials[0];
				String password = credentials[1];

				boolean exists = false;
				for (String cred : loginInformation) { // check if the username already exists
                    if (cred.startsWith(username + ":")) {
                        exists = true;
                        break;
                    }
                }

				// boolean exists = loginInformation.stream().anyMatch(cred -> cred.startsWith(username + ":"));	
                if (exists) { // if the username exists fail the register
					System.out.println("Account creation failed - username exists: " + username);
                    out.writeObject(new Message("Create account failed! Account already exists.", MessageType.CREATE_ACCOUNT));
                } else { // otherwise make a new account
                    loginInformation.add(username + ":" + password);
					saveLoginInformation(); // save the new account to the file
					System.out.println("Account created for: " + username);
                    System.out.println("Current accounts: " + loginInformation);
                    out.writeObject(new Message("Create account successful!", MessageType.CREATE_ACCOUNT));
                }
                out.flush();
            } catch (Exception e) {
                System.err.println("Error sending create account response: " + e.getMessage());
				e.printStackTrace();
            }
        }
	}

	
	//
	// loadLoginInformation
	//
	// For the server to load the login information
	// from "accountInfo.txt" file which stores
	// usernames and passwords
	// not very secure but it works for this project
	//
	private void loadLoginInformation() {
		try {
			File file = new File("accountInfo.txt");
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					loginInformation.add(line);
				}
				reader.close();
				System.out.println("Loaded " + loginInformation.size() + " accounts"); // display number of accounts
			}
		} catch (Exception e) {
			System.err.println("Error loading account: " + e.getMessage());
		}
	}


	//
	// saveLoginInformation
	//
	// Saves any new account registrations into
	// the "accountInfo.txt" file for the server
	// to keep loaded next time it starts
	//
	private void saveLoginInformation() {
		try {
			FileWriter writer = new FileWriter("accountInfo.txt");
			for (String credential : loginInformation) {
				writer.write(credential + "\n");
			}
			writer.close();
			System.out.println("Account saved");
		} catch (Exception e) {
			System.err.println("Error saving account: " + e.getMessage());
		}
	}
}