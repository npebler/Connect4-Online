import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class GuiClient extends Application{

    Client clientConnection;
    private Stage primaryStage;
    private String username;
    ComboBox<Integer> listUsers;
    ListView<String> listItems = new ListView<>();
    private Label feedbackLabel;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private String playerColor = "RED";
    private Circle[][] gameCircles = new Circle[6][7];
    private String[][] boardState = new String[6][7];
    private boolean turn = false;
    private Label turnLabel = new Label();
    private String opponent;
    private boolean gameEnded = false; // Flag to track if the game has ended

    public static void main(String[] args) {
       launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
       this.primaryStage = primaryStage;

       // Initialize the client thread
       clientConnection = new Client(data->{
          Platform.runLater(()->{
             switch (data.type){
                case NEWUSER:
                   listItems.getItems().add(data.recipient + " has joined!");
                   break;
                case DISCONNECT:
                   listItems.getItems().add(data.recipient + " has disconnected!");
                   break;
                case TEXT:
					   //  listItems.getItems().add(data.recipient + ": " + data.message);
                   //  break;
                       if (data.message.startsWith("PLAY") || data.message.startsWith("OPPONENT:") || data.message.startsWith("WAITING") || data.message.startsWith("COLOR:") || data.message.startsWith("UPDATE_STATS:") || data.message.startsWith("FORFEIT")) {
                      // Handle game commands
                      if (data.message.equals("Waiting for another player...")) {
                            // Special case for waiting message
                            listItems.getItems().add(data.message);
                      }
                   } else {
                      // Regular chat message - use the sender field if available, otherwise use recipient ID
                      String senderName;
                      if (data.sender != null && !data.sender.isEmpty()) {
                         senderName = data.sender;
                      } else {
                         senderName = data.recipient != -1 ? "User " + data.recipient : "Server";
                      }
                      listItems.getItems().add(senderName + ": " + data.message);
                   }
                    break;


                case LOGIN:
                   loginHelper(data);
                   break;
                case CREATE_ACCOUNT:
                   registerHelper(data);
                   break;
                case GAME_MOVE:
                   // Handle game move
                   handleOpponentMove(data);
                   break;
                case GAME_START:
                   // Handle game start
                   handleGameStart(data);
                   break;
                case GAME_END:
                   // Handle game end
                   handleGameEnd(data);
                   break;
             }
          });
       });
       clientConnection.start();

       primaryStage.setOnCloseRequest(event -> {
          clientConnection.send(new Message("DISCONNECT", MessageType.DISCONNECT));
          clientConnection.close(); // Close the client connection
       });

       loginScreen(); // begin with the login screen, the login screen will call the next screen
    }


    //
    // loginScreen
    //
    // Method for the login/register screen
    // user will input their username and password to sign in
    // or they can register for a new account
    // accounts are stored in an array of strings in this format: username:password
    // that eventually get stored in a file in the server
    //
    private void loginScreen() {
       VBox loginDisplay = new VBox(15);
       loginDisplay.setAlignment(Pos.CENTER);
       loginDisplay.setPadding(new Insets(30));
       loginDisplay.setStyle("-fx-background-color: linear-gradient(to bottom right, #4e54c8, #8f94fb);");

       Label title = new Label("Connect Four");
       title.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
       title.setTextFill(Color.WHITE);
       title.setEffect(new DropShadow());

       TextField usernameField = new TextField();
       usernameField.setPromptText("Username");
       usernameField.setMaxWidth(220);

       PasswordField passwordField = new PasswordField();
       passwordField.setPromptText("Password");
       passwordField.setMaxWidth(220);

       feedbackLabel = new Label();
       feedbackLabel.setMaxWidth(220);
       feedbackLabel.setWrapText(true);
       feedbackLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

       Button loginButton = new Button("Login");
       loginButton.setMaxWidth(220);
       loginButton.setOnAction(e -> {
          System.out.println("Login button clicked");
          username = usernameField.getText();
          String password = passwordField.getText();
          if (!username.isEmpty() && !password.isEmpty()) {
             feedbackLabel.setText("Logging in...");
             feedbackLabel.setTextFill(Color.BLUE);
             clientConnection.send(new Message(username + ":" + password, MessageType.LOGIN));
          } else {
             feedbackLabel.setText("Please enter both username and password.");
             feedbackLabel.setTextFill(Color.RED);
          }
       });

       Button createAccountButton = new Button("Create Account");
       createAccountButton.setMaxWidth(220);
       createAccountButton.setOnAction(e -> {
          username = usernameField.getText();
          String password = passwordField.getText();
          if (!username.isEmpty() && !password.isEmpty()) {
             feedbackLabel.setText("Creating account...");
             feedbackLabel.setTextFill(Color.BLUE);
             clientConnection.send(new Message(username + ":" + password, MessageType.CREATE_ACCOUNT));
          } else {
             feedbackLabel.setText("Please enter both username and password.");
             feedbackLabel.setTextFill(Color.RED);
          }
       });

       loginDisplay.getChildren().addAll(title, usernameField, passwordField, loginButton, createAccountButton, feedbackLabel);

       Scene loginScene = new Scene(loginDisplay, 400, 300);
       primaryStage.setScene(loginScene);
       primaryStage.setTitle("Connect Four - Login");
       primaryStage.show();
    }


    //
    // loginHelper
    //
    // helper method for the login screen
    // deals with the login portion
    //
   private void loginHelper(Message data) {
		if (data.message.startsWith("Login successful:")) {
			String[] parts = data.message.split(":");
			if (parts.length >= 5) { // Ensure the message contains username, password, wins, losses, draws
				username = parts[1]; // Username
				wins = Integer.parseInt(parts[3]); // Wins
				losses = Integer.parseInt(parts[4]); // Losses
				draws = Integer.parseInt(parts[5]); // Draws
			}
			System.out.println("Login successful with stats - Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
			homeScreen();
		} else if ("Login failed!".equals(data.message)) {
			feedbackLabel.setText("Invalid username or password. Please try again.");
			feedbackLabel.setTextFill(Color.RED);
		}
	}


    //
    // registerHelper
    //
    // helper method for the login screen
    // deals with the register portion
    //
    private void registerHelper(Message data) {
       if ("Create account successful!".equals(data.message)) {
          feedbackLabel.setText("Account created successfully!");
          feedbackLabel.setTextFill(Color.GREEN);
          homeScreen();
       } else if ("Create account failed! Account already exists.".equals(data.message)) {
          feedbackLabel.setText("An account with this username already exists.");
          feedbackLabel.setTextFill(Color.RED);
       }
    }


    //
    // homeScreen
    //
    // description
    //
    private void homeScreen() {
       BorderPane homeDisplay = new BorderPane();
       homeDisplay.setPadding(new Insets(30));
       homeDisplay.setStyle("-fx-background-color: linear-gradient(to bottom right, #4e54c8, #8f94fb);");

       Label title = new Label("CONNECT FOUR");
       title.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 36));
       title.setTextFill(Color.WHITE);
       title.setEffect(new DropShadow(5, Color.BLACK));
       BorderPane.setAlignment(title, Pos.CENTER);
       homeDisplay.setTop(title);

       VBox playOptions = new VBox(15);
       playOptions.setAlignment(Pos.CENTER);

       Button playButton = new Button("Play");
       playButton.setMaxWidth(220);
	   playButton.setOnAction(e -> {
		feedbackLabel.setText("Searching for a match...");
		feedbackLabel.setTextFill(Color.BLUE);
		clientConnection.send(new Message("PLAY", MessageType.TEXT));
		waitingScreen();
	});

       VBox statsBox = new VBox(5);
       statsBox.setAlignment(Pos.CENTER);
       statsBox.setPadding(new Insets(20));
       statsBox.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 10;");

       Label statsTitle = new Label("Your Stats");
       statsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
       statsTitle.setTextFill(Color.WHITE);

       Label winsLabel = new Label("Wins: " + wins);
       winsLabel.setTextFill(Color.WHITE);
       Label lossesLabel = new Label("Losses: " + losses);
       lossesLabel.setTextFill(Color.WHITE);
       Label drawsLabel = new Label("Draws: " + draws);
       drawsLabel.setTextFill(Color.WHITE);

       statsBox.getChildren().addAll(statsTitle, winsLabel, lossesLabel, drawsLabel);

       playOptions.getChildren().addAll(playButton, statsBox);
       homeDisplay.setCenter(playOptions);

       Scene homeScene = new Scene(homeDisplay, 800, 600);
       primaryStage.setScene(homeScene);
       primaryStage.setTitle("Connect Four - Home");
       primaryStage.show();
    }


    //
    // waitingScreen
    //
    // description
    //
	private void waitingScreen() {
		VBox waitingLayout = new VBox(10);
		waitingLayout.setAlignment(Pos.CENTER);
		waitingLayout.setPadding(new Insets(10));
	
		Label connectingLabel = new Label("Waiting for another player...");
		connectingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
		connectingLabel.setTextFill(Color.BLACK);
	
		Button cancelButton = new Button("CANCEL");
		cancelButton.setOnAction(e -> {
			clientConnection.send(new Message("CANCEL_PLAY", MessageType.TEXT)); // Notify the server
			homeScreen(); // Return to the home screen
		});
	
		waitingLayout.getChildren().addAll(connectingLabel, cancelButton);
	
		Scene waitingScene = new Scene(waitingLayout, 800, 600);
		primaryStage.setScene(waitingScene);
		primaryStage.setTitle("Waiting for Opponent");
		primaryStage.show();
	}


    //
    // gameScreen
    //
    // description
    //
	private void gameScreen() {
		BorderPane gameLayout = new BorderPane();
		gameLayout.setPadding(new Insets(10));
		
		// Create the game board grid
		GridPane board = gameBoard();
		gameLayout.setCenter(board);
		
		// Top section with game info
		HBox gameAttributes = new HBox(20);
		gameAttributes.setAlignment(Pos.CENTER);
		gameAttributes.setPadding(new Insets(10));
		
		Label playerColorLabel = new Label("You are " + playerColor);
		playerColorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
		
		turnLabel = new Label(turn ? "Your turn!" : "It is " + opponent + "'s turn");
		turnLabel.setFont(Font.font("Arial", 14));
		
		Button forfeitButton = new Button("Forfeit");
		forfeitButton.setOnAction(e -> {
			clientConnection.send(new Message("GAME_END:FORFEIT", MessageType.GAME_END));
			losses++;
			homeScreen();
		});
		
		gameAttributes.getChildren().addAll(playerColorLabel, turnLabel, forfeitButton);
		gameLayout.setTop(gameAttributes);
		
		// Chat area at the bottom (optional)
		VBox chatBox = new VBox(10);
		chatBox.setPadding(new Insets(10));
		
		listItems = new ListView<>();
		listItems.setPrefHeight(100);
		
		HBox messageArea = new HBox(10);
		TextField chatField = new TextField();
		chatField.setPromptText("Type a message...");
		chatField.setPrefWidth(300);
		
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> {
			if (!chatField.getText().isEmpty()) {
				clientConnection.send(new Message(chatField.getText(), MessageType.TEXT));
				chatField.clear();
			}
		});
		
		messageArea.getChildren().addAll(chatField, sendButton);
		chatBox.getChildren().addAll(listItems, messageArea);
		gameLayout.setBottom(chatBox);
		
		Scene gameScene = new Scene(gameLayout, 800, 700);
		primaryStage.setScene(gameScene);
		primaryStage.setTitle("Connect Four - Game vs " + opponent);
	}


	//
	// endScreen
	//
	// Screen that displays at the end of the game
	// includes a rematch button and the chat
	//
   private void endScreen(String message) {
      System.out.println("Transitioning to end screen with message: " + message);
      
      VBox resultBox = new VBox(20);
      resultBox.setAlignment(Pos.CENTER);
      resultBox.setPadding(new Insets(30));
      resultBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #4e54c8, #8f94fb);");

      Label resultLabel = new Label(message);
      resultLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
      resultLabel.setTextFill(Color.WHITE);

      Label statsLabel = new Label("Your stats: Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
      statsLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
      statsLabel.setTextFill(Color.WHITE);

      HBox buttonBox = new HBox(10); // Horizontal box for buttons
      buttonBox.setAlignment(Pos.CENTER);

      Button returnButton = new Button("Return to Home");
      returnButton.setOnAction(e -> {
         System.out.println("Return to Home button clicked");
         homeScreen();
      });

      Button rematchButton = new Button("Rematch");
      rematchButton.setOnAction(e -> {
         System.out.println("Rematch button clicked");
         clientConnection.send(new Message("PLAY", MessageType.TEXT)); // Notify the server to start a new game
         waitingScreen(); // Transition to the waiting screen
      });

      buttonBox.getChildren().addAll(returnButton, rematchButton);

      resultBox.getChildren().addAll(resultLabel, statsLabel, buttonBox);

      Scene resultScene = new Scene(resultBox, 400, 300);
      primaryStage.setScene(resultScene);
      
      System.out.println("End screen displayed");
   }


    //
    // chatScreen
    //
    // Method for the chat screen
    // user will be able to send and receive messages
    // from the other user in the game/chat
    //
    private void chatScreen(Stage primaryStage) {
       BorderPane chatScreen = new BorderPane();

       TextArea chatArea = new TextArea();
       chatArea.setEditable(false); // read only
       chatArea.setWrapText(true);
       chatScreen.setCenter(chatArea);

       HBox inputBox = new HBox(10);
       TextField inputField = new TextField();
       inputField.setPromptText("Type your message here...");
       Button sendButton = new Button("Send");

       inputBox.getChildren().addAll(inputField, sendButton);
       chatScreen.setBottom(inputBox); // line the input box to the bottom

       Scene chatScene = new Scene(chatScreen, 400, 300);
       primaryStage.setScene(chatScene);
       primaryStage.setTitle("Chat");
       primaryStage.show();


       sendButton.setOnAction(e -> { // send button
          String message = inputField.getText();
          if (!message.isEmpty()) {
             clientConnection.send(new Message(-1, message)); // sends a message object so we can hold multiple pieces of data
             inputField.clear();
          }
       });

       // thread that listens for any new messages
       new Thread(() -> {
          while (true) {
             try {
                Message receivedMessage = clientConnection.receive(); // Receive a Message object
                if (receivedMessage != null) {
                   Platform.runLater(() -> chatArea.appendText(
                         "User " + receivedMessage.recipient + ": " + receivedMessage.message + "\n"
                   ));
                }
             } catch (Exception e) {
                System.err.println("Error receiving message: " + e.getMessage());
                break;
             }
          }
       }).start();
    }


	//
	// gameBoard
	//
	// Method for making the gameboard
	//
    private GridPane gameBoard() {
       GridPane grid = new GridPane();
       grid.setAlignment(Pos.CENTER);
       grid.setHgap(5);
       grid.setVgap(5);
       grid.setPadding(new Insets(10));
       grid.setStyle("-fx-background-color: #2980b9; -fx-background-radius: 10;");

       for(int row = 0; row < 6; row++) {
          for(int col = 0; col < 7; col++) {
             //background
             Rectangle rect = new Rectangle(60,60);
             rect.setFill(Color.rgb(41, 120, 187));
             grid.add(rect, col, row);

             //pieces
             Circle circle = new Circle(30);
             circle.setFill(Color.WHITE);
             circle.setStroke(Color.BLACK);
             circle.setStrokeWidth(2);

             grid.add(circle, col, row);
             gameCircles[row][col] = circle;

             final int column = col; //stores column for the clicker
             circle.setOnMouseClicked(e -> {
                // Only allow moves when it's the player's turn
                if(turn) {
                   // Check if the column is not full before making a move
                   boolean columnFull = true;
                   for (int r = 5; r >= 0; r--) {
                      if (boardState[r][column].equals("EMPTY")) {
                         columnFull = false;
                         break;
                      }
                   }
                   if (!columnFull) {
                      makeMove(column);
                   }
                } else {
                   // Visual feedback that it's not the player's turn
                   circle.setEffect(new DropShadow(10, Color.RED));
                   new Thread(() -> {
                      try {
                         Thread.sleep(300);
                         Platform.runLater(() -> circle.setEffect(null));
                      } catch (InterruptedException ex) {
                         ex.printStackTrace();
                      }
                   }).start();
                }
             });
          }
       }
       return grid;
    }


	//
	// makeMove
	//
	// Method to handle the move made by the player
	// updates the game board and checks for a win
	//
	private void makeMove(int column) {
		if (!turn) {
			return; // Not your turn
		}
		
		for (int row = 5; row >= 0; row--) {
			if (boardState[row][column].equals("EMPTY")) {
				boardState[row][column] = playerColor;
				
				if (playerColor.equals("RED")) {
					gameCircles[row][column].setFill(Color.RED);
				} else {
					gameCircles[row][column].setFill(Color.YELLOW);
				}
				
				// Check if this player won
				if (checkWin(row, column, playerColor)) {
					turnLabel.setText("You win!");
					clientConnection.send(new Message("GAME_END:WIN", MessageType.GAME_END));
					gameEnded = true; // Set the game ended flag
					return;
				}
				
				// Check for draw
				if (checkDraw()) {
					turnLabel.setText("Game is a draw!");
					clientConnection.send(new Message("GAME_END:DRAW", MessageType.GAME_END));
					gameEnded = true; // Set the game ended flag
					return;
				}
				
				// Send the move to the server
				clientConnection.send(new Message("MOVE:" + column, MessageType.GAME_MOVE));
				
				// Update turn state
				turn = false;
				turnLabel.setText("It is " + opponent + "'s turn");
				
				break;
			}
		}
	}


	//
	// handleStats
	//
	// updates the wins, losses and draws based on the Message obj passed through
	//
	private void handleStats(Message data) {
		String result = data.message;
		
		System.out.println("Handling stats update with result: " + result);

		if (result.startsWith("STATS_UPDATED")) {
			// Parse the updated stats from the server's response
			String[] parts = result.split(":");
			if (parts.length >= 4) {
				wins = Integer.parseInt(parts[1]);
				losses = Integer.parseInt(parts[2]);
				draws = Integer.parseInt(parts[3]);
				System.out.println("Stats updated from server - Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
			}
		} else if (result.equals("WIN")) {
			wins++;
			System.out.println("Local stats updated - WIN added. New wins: " + wins);
		} else if (result.equals("LOSS")) {
			losses++;
			System.out.println("Local stats updated - LOSS added. New losses: " + losses);
		} else if (result.equals("DRAW")) {
			draws++;
			System.out.println("Local stats updated - DRAW added. New draws: " + draws);
		}
	}
	

	//
	// handleGameStart
	//
	//
	//
	private void handleGameStart(Message data) {
		// Parse opponent and turn information
		// Example format: "OPPONENT:username:FIRST_TURN:true/false:COLOR"
		String[] parts = data.message.split(":");
		if (parts.length >= 5) {
			opponent = parts[1];
			turn = Boolean.parseBoolean(parts[3]);
			playerColor = parts[4]; // Get the color assigned by the server
			
			// Initialize the game board
			initializeBoard();
			
			// Set up the turn label
			if (turn) {
				turnLabel.setText("Your turn!");
			} else {
				turnLabel.setText("It is " + opponent + "'s turn");
			}
			
			// Switch to game screen
			gameScreen();
		}
	}

	//
	// checkWin
	//
	// calls the other methods to check for a win
	//
    private boolean checkWin(int row, int col, String color) {		
		// Log the win check for debugging
		System.out.println("Checking for win at row " + row + ", col " + col + " with color " + color);
		
		boolean win = checkStraight(row, col, color) || checkDiagonal(row, col, color);
		
		if (win) {
			System.out.println("WIN DETECTED for " + color + " at row " + row + ", col " + col);
		}
		
		return win;
    }


	//
	// checkStraight
	//
	// check straight up and down or left and right for a win
	//
    private boolean checkStraight(int row, int column, String color) {
       // Check horizontal (left to right)
       int count = 0;
       for (int c = 0; c < 7; c++) {
           if (boardState[row][c].equals(color)) {
               count++;
               if (count >= 4) {
                   System.out.println("Horizontal win detected at row " + row);
                   return true;
               }
           } else {
               count = 0;
           }
       }
       
       // Check vertical (top to bottom)
       count = 0;
       for (int r = 0; r < 6; r++) {
           if (boardState[r][column].equals(color)) {
               count++;
               if (count >= 4) {
                   System.out.println("Vertical win detected at column " + column);
                   return true;
               }
           } else {
               count = 0;
           }
       }
       
       return false;
    }


	//
	// checkDiagonal
	//
	// check diagonal for a win
	//
    private boolean checkDiagonal(int row, int col, String color) {
       // Check diagonal (top-left to bottom-right)
       int count = 0;
       for (int r = 0; r < 6; r++) {
           for (int c = 0; c < 7; c++) {
               // Check diagonal starting at this position
               count = 0;
               for (int i = 0; i < 4; i++) {
                   int r1 = r + i;
                   int c1 = c + i;
                   if (r1 < 6 && c1 < 7 && boardState[r1][c1].equals(color)) {
                       count++;
                   } else {
                       break;
                   }
               }
               if (count >= 4) {
                   System.out.println("Diagonal win detected (top-left to bottom-right) at row " + r + ", col " + c);
                   return true;
               }
               
               // Check diagonal (top-right to bottom-left)
               count = 0;
               for (int i = 0; i < 4; i++) {
                   int r1 = r + i;
                   int c1 = c - i;
                   if (r1 < 6 && c1 >= 0 && boardState[r1][c1].equals(color)) {
                       count++;
                   } else {
                       break;
                   }
               }
               if (count >= 4) {
                   System.out.println("Diagonal win detected (top-right to bottom-left) at row " + r + ", col " + c);
                   return true;
               }
           }
       }
       return false;
    }


	// 
	// checkDraw
	//
	// check to see if the game is a draw
	//
    private boolean checkDraw() {
       for(int col = 0; col < 7; col++) {
          if(boardState[0][col].equals("EMPTY")) {
             return false;
          }
       }
       return true;
    }


	//
	// handleOpponentMove
	//
	// Method to handle the move made by the opponent
	// updates the game board and checks for a win
	//
	private void handleOpponentMove(Message data) {
		// Parse the move data (assuming format: "MOVE:COLUMN")
		String[] parts = data.message.split(":");
		if (parts.length == 2 && parts[0].equals("MOVE")) {
			int column = Integer.parseInt(parts[1]);
			
			// Find the first empty row in this column (from bottom up)
			for (int row = 5; row >= 0; row--) {
				if (boardState[row][column].equals("EMPTY")) {
					// Update the board with opponent's move
					String opponentColor = playerColor.equals("RED") ? "YELLOW" : "RED";
					boardState[row][column] = opponentColor;
					
					// Update the UI
					if (opponentColor.equals("RED")) {
						gameCircles[row][column].setFill(Color.RED);
					} else {
						gameCircles[row][column].setFill(Color.YELLOW);
					}
					
					// Check if opponent won
					if (checkWin(row, column, opponentColor)) {
						// Handle opponent win
						turnLabel.setText(opponent + " wins!");
						// Notify server about the loss
						clientConnection.send(new Message("GAME_END:LOSE", MessageType.GAME_END));
						losses++;
						gameEnded = true; // Set the game ended flag
						return;
					}
					
					// Check for draw
					if (checkDraw()) {
						turnLabel.setText("Game is a draw!");
						clientConnection.send(new Message("GAME_END:DRAW", MessageType.GAME_END));
						draws++;
						gameEnded = true; // Set the game ended flag
						return;
					}
					
					// Now it's this player's turn
					turn = true;
					turnLabel.setText("Your turn!");
					break;
				}
			}
		}
	}

	private void initializeBoard() {
		// Initialize the board state
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 7; j++) {
				boardState[i][j] = "EMPTY";
			}
		}
		
		// Reset game state
		gameEnded = false;  // Reset the game ended flag
	}


   private void handleGameEnd(Message data) {
    // Extract the result from the message
    String[] parts = data.message.split(":");
    if (parts.length == 2) {
        String result = parts[1];
        
        // Set the game ended flag to prevent multiple dialogs
        gameEnded = true;
        
        // Update stats based on result
        if (result.equals("WIN")) {
            wins++;
            endScreen("You won the game!");
        } else if (result.equals("LOSE")) {
            losses++;
            endScreen("You lost the game.");
        } else if (result.equals("FORFEIT")) {
            wins++; // If opponent forfeits, you win
            endScreen("Your opponent forfeited. You win!");
        } else if (result.equals("DRAW")) {
            draws++;
            endScreen("The game ended in a draw.");
        }
    }
}

}