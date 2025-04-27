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
    ListView<String> listItems;
    private Label feedbackLabel;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;
    private String playerColor = "RED";
    private Circle[][] gameCircles = new Circle[6][7];
    private String[][] boardState = new String[6][7];
    private boolean turn = false;
    private Label turnLabel;
    private String opponent;

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
					if (data.message.startsWith("START_GAME")) {
						handleStartGame(data.message);
					} else if (data.message.equals("You forfeited the game. You lose!")) {
						// Handle forfeit loss
						losses++;
						endScreen("You forfeited the game. You lose!");
					} else if (data.message.equals("Your opponent forfeited. You win!")) {
						// Handle forfeit win
						wins++;
						endScreen("Your opponent forfeited. You win!");
					} else {
						listItems.getItems().add(data.recipient + ": " + data.message);
					}
                case LOGIN:
                   loginHelper(data);
                   break;
                case CREATE_ACCOUNT:
                   registerHelper(data);
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
    // private void loginHelper(Message data) {
    //    if ("Login successful".equals(data.message)) {
	// 	String[] parts = data.message.split(":");
	// 	if (parts.length >= 6) { // parse the account info
	// 		username = parts[1];
	// 		wins = Integer.parseInt(parts[3]);
	// 		losses = Integer.parseInt(parts[4]);
	// 		draws = Integer.parseInt(parts[5]);
	// 	}
	// 	System.out.println("Login successful with stats - Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
	// 	homeScreen();
    //    } else if ("Login failed!".equals(data.message)) {
    //       feedbackLabel.setText("Invalid username or password. Please try again.");
    //       feedbackLabel.setTextFill(Color.RED);
    //    }
    // }
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
          new Thread(() -> {
             try {
                Thread.sleep(1500); // 1.5 second delay
                Platform.runLater(() -> homeScreen());
             } catch (InterruptedException e) {
                e.printStackTrace();
             }
          }).start();
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
       Button playWithFriendButton = new Button("Play with Friend");
       playWithFriendButton.setMaxWidth(220);
       playWithFriendButton.setOnAction(e -> {
          // Logic for playing with a friend
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

       VBox colorBox = new VBox(10);
       colorBox.setAlignment(Pos.CENTER);
       colorBox.setPadding(new Insets(10));

       Label colorLabel = new Label("Choose Your Color:");
       colorLabel.setTextFill(Color.WHITE);

       HBox colorOptions = new HBox(15);
       colorOptions.setAlignment(Pos.CENTER);

       Button redButton = new Button("Red");
       redButton.setStyle("-fx-background-color: #e74c3c;");
       redButton.setOnAction(e -> {
          playerColor = "RED";
          clientConnection.send(new Message("COLOR:RED", MessageType.TEXT));
       });

       Button yellowButton = new Button("Yellow");
       yellowButton.setStyle("-fx-background-color: #f1c40f;");
       yellowButton.setOnAction(e -> {
          playerColor = "YELLOW";
          clientConnection.send(new Message("COLOR:YELLOW", MessageType.TEXT));
       });

       colorOptions.getChildren().addAll(redButton, yellowButton);
       colorBox.getChildren().addAll(colorLabel, colorOptions);

       playOptions.getChildren().addAll(playButton, playWithFriendButton, statsBox, colorBox);
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
    // private void waitingScreen() {
    //    VBox waitingLayout = new VBox(10);
    //    waitingLayout.setAlignment(Pos.CENTER);
    //    waitingLayout.setPadding(new Insets(10));

    //    Label connectingLabel = new Label("CONNECTING...");
    //    Button cancelButton = new Button("CANCEL");
    //    cancelButton.setOnAction(e -> homeScreen());

    //    waitingLayout.getChildren().addAll(connectingLabel, cancelButton);

    //    Scene waitingScene = new Scene(waitingLayout, 800, 600);
    //    primaryStage.setScene(waitingScene);
    // }
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
	
		// Top Section: Player Information
		HBox gameAttributes = new HBox(20);
		gameAttributes.setAlignment(Pos.CENTER);
		gameAttributes.setPadding(new Insets(10));
	
		Label playerColorLabel = new Label("You are " + playerColor);
		playerColorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
	
		turnLabel = new Label(turn ? "Your turn" : opponent + "'s turn");
		turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
	
		Button forfeitButton = new Button("FORFEIT");
		forfeitButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
		forfeitButton.setOnAction(e -> {
			clientConnection.send(new Message("FORFEIT:" + username + ":" + opponent, MessageType.TEXT));
			homeScreen(); // Return to home screen after forfeit
		});
	
		gameAttributes.getChildren().addAll(playerColorLabel, turnLabel, forfeitButton);
		gameLayout.setTop(gameAttributes);
	
		// Center Section: Game Board
		GridPane board = gameBoard();
		gameLayout.setCenter(board);
	
		// Bottom Section: Chat Box
		VBox chatBox = new VBox(10);
		chatBox.setPadding(new Insets(10));
	
		Label chatLabel = new Label("CHAT:");
		chatLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
	
		ListView<String> chatHistory = new ListView<>();
		chatHistory.setPrefHeight(150);
	
		HBox messageBox = new HBox(10);
		TextField chatField = new TextField();
		chatField.setPromptText("Enter message...");
		chatField.setPrefWidth(650);
	
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> {
			if (!chatField.getText().isEmpty()) {
				String message = chatField.getText();
				clientConnection.send(new Message("CHAT:" + message, MessageType.TEXT));
				chatHistory.getItems().add("You: " + message);
				chatField.clear();
			}
		});
	
		chatField.setOnAction(e -> sendButton.fire()); // Send on Enter key
	
		messageBox.getChildren().addAll(chatField, sendButton);
		chatBox.getChildren().addAll(chatLabel, chatHistory, messageBox);
	
		gameLayout.setBottom(chatBox);
	
		// Set the Scene
		Scene gameScene = new Scene(gameLayout, 800, 700);
		primaryStage.setScene(gameScene);
		primaryStage.setTitle("Connect Four - Game");
		primaryStage.show();
	}


	//
	// endScreen
	//
	// Screem that displays at the end of the game
	// includes a rematch button and the chat
	//
	private void endScreen(String message) {
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
	
		Button returnButton = new Button("Return to Home");
		returnButton.setOnAction(e -> homeScreen());
	
		resultBox.getChildren().addAll(resultLabel, statsLabel, returnButton);
	
		Scene resultScene = new Scene(resultBox, 400, 300);
		primaryStage.setScene(resultScene);
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
                if(turn) {
                   makeMove(column);
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
    // private void makeMove(int column) {
    // 	for(int row = 5; row >= 0; row--) {
    //     	if(boardState[row][column].equals("EMPTY")) {
	// 			if(playerColor.equals("RED")) {
	// 				Color pieceColor = Color.RED;
	// 			} else {
	// 				Color pieceColor = Color.YELLOW;
	// 			}
	// 		}
	// 	boardState[row][column] = playerColor;
	// 	//fix once implemented 
	// 	// clientConnection.send(new Message("MOVE: " + column, MessageType.TEXT));
	// 	turn = false;
	// 	turnLabel.setText("It is " + opponent + "'s turn");
          
	// 	}
	// }
	private void makeMove(int column) {
		// Find the lowest empty row in the selected column
		int row = -1;
		for (int r = 5; r >= 0; r--) {
			if (boardState[r][column].equals("EMPTY")) {
				row = r;
				break;
			}
		}
	
		if (row == -1) {
			// Column is full, invalid move
			return;
		}
	
		// Update the board state and UI
		boardState[row][column] = playerColor;
		Color pieceColor = playerColor.equals("RED") ? Color.RED : Color.YELLOW;
		gameCircles[row][column].setFill(pieceColor);
	
		// Check for win or draw
		if (checkWin(row, column, playerColor)) {
			clientConnection.send(new Message("UPDATE_STATS:" + username + ":WIN", MessageType.TEXT));
			handleStats(new Message("WIN", MessageType.TEXT));
			endScreen("You won!");
		} else if (checkDraw()) {
			clientConnection.send(new Message("UPDATE_STATS:" + username + ":DRAW", MessageType.TEXT));
			handleStats(new Message("DRAW", MessageType.TEXT));
			endScreen("It's a draw!");
		} else {
			// Switch turns
			turn = false;
			turnLabel.setText("It is " + opponent + "'s turn");
			clientConnection.send(new Message("MOVE:" + column, MessageType.TEXT));
		}
	}


	//
	// handleStats
	//
	// updates the wins, losses and draws based on the Message obj passed through
	//
	private void handleStats(Message data) {
		String result = data.message;

		if (result.startsWith("STATS_UPDATED")) {
			// Parse the updated stats from the server's response
			String[] parts = result.split(":");
			if (parts.length >= 4) {
				wins = Integer.parseInt(parts[1]);
				losses = Integer.parseInt(parts[2]);
				draws = Integer.parseInt(parts[3]);
				System.out.println("Stats updated - Wins: " + wins + ", Losses: " + losses + ", Draws: " + draws);
			}
		} else if (result.equals("WIN")) {
			wins++;
		} else if (result.equals("LOSS")) {
			losses++;
		} else if (result.equals("DRAW")) {
			draws++;
		}
	}
	

	//
	// handleStartGame
	//
	//
	//
	private void handleStartGame(String message) {
		// Parse the START_GAME message
		String[] parts = message.split(":");
		if (parts.length == 3) {
			playerColor = parts[1]; // RED or YELLOW
			opponent = parts[2]; // Opponent's username

			// Display feedback to the user
			feedbackLabel.setText("Matched with " + opponent + "! Starting game...");
			feedbackLabel.setTextFill(Color.GREEN);

			// Transition to the game screen
			gameScreen();
		} else {
			System.err.println("Issue START_GAME message: " + message);
		}
	}

	//
	// checkWin
	//
	// calls the other methods to check for a win
	//
    private boolean checkWin(int row, int col, String color) {		
		if (checkStraight(row, col, color) || checkDiagonal(row, col, color)) {
			// endScreen();
			return true;
		}
		return checkStraight(row, col, color) || checkDiagonal(row, col, color);
    }


	//
	// checkStraight
	//
	// check straight up and down or left and right for a win
	//
    private boolean checkStraight(int row, int column, String color) {
       return false;
    }


	//
	// checkDiagonal
	//
	// check diagonal for a win
	//
    private boolean checkDiagonal(int row, int col, String color) {
       for(int i = 0; i < 6; i++) {
          for (int j = 0; j < 7; j++) {
             int count = 0;
             for (int k = 0; k < 4; k++) {
                int r = i + row * k;
                int c = j + col * k;
                if (boardState[r][c].equals(color)) {
                   count++;
                } else {
                   break;
                }
             }
             if (count == 4) {
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




}