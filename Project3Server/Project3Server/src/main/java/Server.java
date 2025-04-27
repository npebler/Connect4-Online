import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server{

	int count = 1; // number of clients
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	ArrayList<String> loginInformation = new ArrayList<String>(); // stores username:password info
	List<ClientThread> waitingPlayers = new ArrayList<>(); // List of players waiting for a game

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
		String username;
		
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
			if (message.type == MessageType.TEXT && this.username != null) {
				message.sender = this.username; // Set the sender's username
			}
			
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
					} else if (message.type == MessageType.TEXT) {
						// Check which type of text message it is
						if (message.message.startsWith("UPDATE_STATS:")) {
							// Extract username and result from the message
							String[] parts = message.message.split(":");
							if (parts.length >= 3) {
								String username = parts[1];
								String result = parts[2];
								updateStats(username, result); // Call updateStats with extracted values
							} else {
								System.err.println("Malformed UPDATE_STATS message: " + message.message);
							}
						} else if (message.message.startsWith("PLAY") || message.message.equals("CANCEL_PLAY")) {
							handlePlayRequest(this, message.message);
						} else if (message.message.startsWith("FORFEIT")) {
							String[] parts = message.message.split(":");
							if (parts.length == 3) {
								String forfeiter = parts[1];
								String opponent = parts[2];
								handleForfeit(forfeiter, opponent);
								// Don't broadcast the FORFEIT message
								return;
							} else {
								System.err.println("Malformed FORFEIT message: " + message.message);
							}
						}
					} else if (message.type == MessageType.GAME_MOVE) {
						// Forward the game move to the opponent
						// Format: MOVE:COLUMN
						String[] moveParts = message.message.split(":");
						if (moveParts.length == 2) {
							// Find the opponent and forward the move
							for (ClientThread client : clients) {
								if (client != this && client.username != null) {
									try {
										client.out.writeObject(new Message("MOVE:" + moveParts[1], MessageType.GAME_MOVE));
										client.out.flush();
									} catch (Exception e) {
										System.err.println("Error forwarding game move: " + e.getMessage());
									}
									break;
								}
							}
						}
						continue;
					} else if (message.type == MessageType.GAME_END) {
						String[] endParts = message.message.split(":");
						if (endParts.length == 2) {
							String result = endParts[1];
							
							// find opponent
							ClientThread opponent = null;
							for (ClientThread client : clients) {
								if (client != this && client.username != null) {
									opponent = client;
									break;
								}
							}
							
							if (opponent != null) {
								try {
									// Update stats for both players
									if (result.equals("WIN")) {
										updateStats(this.username, "WIN");
										updateStats(opponent.username, "LOSS");
										
										// Send confirmation to the winner
										this.out.writeObject(new Message("GAME_END:WIN", MessageType.GAME_END));
										this.out.flush();
										
										// Send notification to the loser
										opponent.out.writeObject(new Message("GAME_END:LOSE", MessageType.GAME_END));
										opponent.out.flush();
									} 
									else if (result.equals("LOSE")) {
										updateStats(this.username, "LOSS");
										updateStats(opponent.username, "WIN");
										
										// Send confirmation to the loser
										this.out.writeObject(new Message("GAME_END:LOSE", MessageType.GAME_END));
										this.out.flush();
										
										// Send notification to the winner
										opponent.out.writeObject(new Message("GAME_END:WIN", MessageType.GAME_END));
										opponent.out.flush();
									}
									else if (result.equals("DRAW")) {
										updateStats(this.username, "DRAW");
										updateStats(opponent.username, "DRAW");
										
										// Send confirmation to both players
										this.out.writeObject(new Message("GAME_END:DRAW", MessageType.GAME_END));
										this.out.flush();
										
										opponent.out.writeObject(new Message("GAME_END:DRAW", MessageType.GAME_END));
										opponent.out.flush();
									}
									else if (result.equals("FORFEIT")) {
										updateStats(this.username, "LOSS");
										updateStats(opponent.username, "WIN");
										
										// Send confirmation to the forfeiter
										this.out.writeObject(new Message("GAME_END:FORFEIT", MessageType.GAME_END));
										this.out.flush();
										
										// Send notification to the winner
										opponent.out.writeObject(new Message("GAME_END:WIN", MessageType.GAME_END));
										opponent.out.flush();
									}
								} catch (Exception e) {
									System.err.println("Error handling game end: " + e.getMessage());
								}
							}
						}
						// Don't broadcast game end messages to all clients
						continue;
					}
					// Only broadcast non-game messages to all clients
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
				String userStats = null;
		
				for (String cred : loginInformation) {
					String[] parts = cred.split(":");
					if (parts[0].equals(username) && parts[1].equals(password)) {
						found = true;
						userStats = cred; // Store the full account info (username:password:wins:losses:draws)
						break;
					}
				}
		
				if (found && userStats != null) {
					System.out.println("Login successful for: " + username);
					// Set the username field of the ClientThread
					this.username = username;
					// Send back stats to client with login success
					out.writeObject(new Message("Login successful:" + userStats, MessageType.LOGIN));
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

                if (exists) { // if the username exists fail the register
					System.out.println("Account creation failed - username exists: " + username);
                    out.writeObject(new Message("Create account failed! Account already exists.", MessageType.CREATE_ACCOUNT));
                } else { // otherwise make a new account
                    loginInformation.add(username + ":" + password + ":0:0:0"); // new account, 0 wins losses and draws
					saveLoginInformation(); // save the new account to the file
					System.out.println("Account created for: " + username);
					// Set the username field of the ClientThread
					this.username = username;
                    out.writeObject(new Message("Create account successful!", MessageType.CREATE_ACCOUNT));
                }
                out.flush();
            } catch (Exception e) {
                System.err.println("Error creating account: " + e.getMessage());
            }
        }


		//
		// handlePlayRequest
		//
		// Handle the play request from the client
		//
		private void handlePlayRequest(ClientThread client, String message) {
			synchronized (waitingPlayers) {
				if (message.equals("CANCEL_PLAY")) {
					// Remove the client from the waiting queue
					if (waitingPlayers.remove(client)) {
						System.out.println("Client " + client.username + " canceled and was removed from the waiting queue.");
					} else {
						System.out.println("Client " + client.username + " was not in the waiting queue.");
					}
					return;
				}
		
				if (waitingPlayers.isEmpty()) {
					// Add the client to the waiting list if no one is waiting
					waitingPlayers.add(client);
					try {
						client.out.writeObject(new Message("Waiting for another player...", MessageType.TEXT));
						client.out.flush();
					} catch (Exception e) {
						System.err.println("Error sending waiting message: " + e.getMessage());
					}
				} else {
					ClientThread opponent = waitingPlayers.remove(0);
					try {
						client.out.writeObject(new Message("OPPONENT:" + opponent.username + ":FIRST_TURN:true:RED", MessageType.GAME_START));
						client.out.flush();
						opponent.out.writeObject(new Message("OPPONENT:" + client.username + ":FIRST_TURN:false:YELLOW", MessageType.GAME_START));
						opponent.out.flush();
					} catch (Exception e) {
						System.err.println("Error starting game: " + e.getMessage());
					}
				}
			}
		}


		private void handleForfeit(String forfeiter, String opponent) {
			try {
				updateStats(forfeiter, "LOSS");
				updateStats(opponent, "WIN");
		
				for (ClientThread client : clients) {
					if (client.username.equals(forfeiter)) {
						client.out.writeObject(new Message("FORFEIT", MessageType.GAME_END));
						client.out.flush();
					} else if (client.username.equals(opponent)) {
						client.out.writeObject(new Message("WIN", MessageType.GAME_END));
						client.out.flush();
					}
				}
		
				System.out.println("Game forfeited by " + forfeiter + ". " + opponent + " wins.");
			} catch (Exception e) {
				System.err.println("Error handling forfeit: " + e.getMessage());
			}
		}


	} // end of ClientThread

	class GameThread extends Thread {
        private ClientThread player1;
        private ClientThread player2;

        GameThread(ClientThread p1, ClientThread p2) {
            this.player1 = p1;
            this.player2 = p2;
        }

        public void run() {
            try {
                // Notify both players to start the game
                player1.out.writeObject(new Message("START_GAME:RED:" + player2.username, MessageType.GAME_START));
                player1.out.flush();
                player2.out.writeObject(new Message("START_GAME:YELLOW:" + player1.username, MessageType.GAME_START));
                player2.out.flush();

                System.out.println("Game started between " + player1.username + " and " + player2.username);

                while (true) {
                    Message messageFromPlayer1 = (Message) player1.in.readObject();
                    Message messageFromPlayer2 = (Message) player2.in.readObject();

                    if (messageFromPlayer1.message.startsWith("MOVE:")) {
                        player2.out.writeObject(messageFromPlayer1);
                        player2.out.flush();
                    }
                    if (messageFromPlayer2.message.startsWith("MOVE:")) {
                        player1.out.writeObject(messageFromPlayer2);
                        player1.out.flush();
                    }

                    if (messageFromPlayer1.message.startsWith("GAME_RESULT:")) {
                        handleGameResult(messageFromPlayer1, player1, player2);
                        break;
                    }
                    if (messageFromPlayer2.message.startsWith("GAME_RESULT:")) {
                        handleGameResult(messageFromPlayer2, player2, player1);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in game thread: " + e.getMessage());
            }
        }


		private void handleGameResult(Message message, ClientThread winner, ClientThread loser) {
			try {
				String[] parts = message.message.split(":");
				String result = parts[1]; // WIN, LOSS, or DRAW
		
				if (result.equals("WIN")) {
					winner.out.writeObject(new Message("WIN", MessageType.GAME_END));
					winner.out.flush();
					loser.out.writeObject(new Message("LOSE", MessageType.GAME_END));
					loser.out.flush();
		
					// Update stats
					updateStats(winner.username, "WIN");
					updateStats(loser.username, "LOSS");
				} else if (result.equals("DRAW")) {
					winner.out.writeObject(new Message("DRAW", MessageType.GAME_END));
					winner.out.flush();
					loser.out.writeObject(new Message("DRAW", MessageType.GAME_END));
					loser.out.flush();
		
					// Update stats
					updateStats(winner.username, "DRAW");
					updateStats(loser.username, "DRAW");
				}
			} catch (Exception e) {
				System.err.println("Error handling game result: " + e.getMessage());
			}
		}
	}
	
	//
	// loadLoginInformation
	//
	// For the server to load the login information
	// from "accountInfo.txt" file which stores
	// usernames:passwords:wins:losses:draws
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
				System.out.println("Loaded " + loginInformation.size() + " accounts"); // display number of accounts after it loads
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


	//
	// updateStats
	//
	// Update the server account list with
	// the new wins, losses and draws
	//
	private void updateStats(String username, String result) {
		for (int i = 0; i < loginInformation.size(); i++) {
			String account = loginInformation.get(i);
			String[] parts = account.split(":");
	
			if (parts.length >= 5 && parts[0].equals(username)) {
				int wins = Integer.parseInt(parts[2]);
				int losses = Integer.parseInt(parts[3]);
				int draws = Integer.parseInt(parts[4]);
	
				// Update stats based on the result
				if (result.equals("WIN")) {
					wins++;
				} else if (result.equals("LOSS")) {
					losses++;
				} else if (result.equals("DRAW")) {
					draws++;
				}
	
				// Update the account information
				String updatedAccount = parts[0] + ":" + parts[1] + ":" + wins + ":" + losses + ":" + draws;
				loginInformation.set(i, updatedAccount);
				saveLoginInformation(); // Save the updated stats to the file
	
				System.out.println("Updated stats for " + username + ": " + wins + " wins, " + losses + " losses, " + draws + " draws");
				break;
			}
		}
	}
}