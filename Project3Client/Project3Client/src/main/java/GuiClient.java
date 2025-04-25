

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class GuiClient extends Application{

	Client clientConnection;
    private Stage primaryStage;
    private String username;
	ComboBox<Integer> listUsers;
	ListView<String> listItems;

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
						listItems.getItems().add(data.recipient + ": " + data.message);
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
	//
    // private void loginScreen() {
    //     VBox loginDisplay = new VBox(15);
    //     loginDisplay.setAlignment(Pos.CENTER);
    //     loginDisplay.setPadding(new Insets(30));
    //     loginDisplay.setStyle("-fx-background-color: linear-gradient(to bottom right, #4e54c8, #8f94fb);");

    //     Label title = new Label("Connect Four");
    //     title.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
    //     title.setTextFill(Color.WHITE);
    //     title.setEffect(new DropShadow());

    //     TextField usernameField = new TextField();
    //     usernameField.setPromptText("Username");
    //     usernameField.setMaxWidth(220);

    //     PasswordField passwordField = new PasswordField();
    //     passwordField.setPromptText("Password");
    //     passwordField.setMaxWidth(220);

    //     Button loginButton = new Button("Login");
    //     loginButton.setMaxWidth(220);
    //     loginButton.setOnAction(e -> {
    //         username = usernameField.getText();
    //         String password = passwordField.getText();
    //         if (!username.isEmpty() && !password.isEmpty()) {
    //             clientConnection.send(new Message("LOGIN:" + username + ":" + password));
    //             homeScreen(); // Transition to the home screen after login
    //         }
    //     });

    //     Button createAccountButton = new Button("Create Account");
    //     createAccountButton.setMaxWidth(220);
    //     createAccountButton.setOnAction(e -> {
    //         String username = usernameField.getText();
    //         String password = passwordField.getText();
    //         if (!username.isEmpty() && !password.isEmpty()) {
    //             clientConnection.send(new Message("CREATE ACCOUNT:" + username + ":" + password));
    //         }
    //     });

    //     loginDisplay.getChildren().addAll(title, usernameField, passwordField, loginButton, createAccountButton);

    //     Scene loginScene = new Scene(loginDisplay, 400, 300);
    //     primaryStage.setScene(loginScene);
    //     primaryStage.setTitle("Connect Four - Login");
    //     primaryStage.show();
    // }



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

        Label feedbackLabel = new Label();
        feedbackLabel.setTextFill(Color.RED);

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(220);
        loginButton.setOnAction(e -> {
			System.out.println("Login button clicked");
            username = usernameField.getText();
            String password = passwordField.getText();
            if (!username.isEmpty() && !password.isEmpty()) {
                clientConnection.send(new Message(username + ":" + password, MessageType.LOGIN));
            } else {
                feedbackLabel.setText("Please enter both username and password.");
            }
        });

        Button createAccountButton = new Button("Create Account");
        createAccountButton.setMaxWidth(220);
        createAccountButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (!username.isEmpty() && !password.isEmpty()) {
                clientConnection.send(new Message(username + ":" + password, MessageType.CREATE_ACCOUNT));
            } else {
                feedbackLabel.setText("Please enter both username and password.");
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
        if ("Login successful".equals(data.message)) {
            System.out.println("Login successful!");
            homeScreen();
        } else if ("Login failed!".equals(data.message)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null);
            alert.setContentText("Invalid username or password. Please try again.");
            alert.showAndWait();
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Account Created");
            alert.setHeaderText(null);
            alert.setContentText("Your account has been created successfully!");
            alert.showAndWait();
            homeScreen();
        } else if ("Create account failed! Account already exists.".equals(data.message)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Account Creation Failed");
            alert.setHeaderText(null);
            alert.setContentText("An account with this username already exists.");
            alert.showAndWait();
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
        playButton.setOnAction(e -> waitingScreen());

        Button playWithFriendButton = new Button("Play with Friend");
        playWithFriendButton.setMaxWidth(220);
        playWithFriendButton.setOnAction(e -> {
            // Logic for playing with a friend
        });

        playOptions.getChildren().addAll(playButton, playWithFriendButton);
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

        Label connectingLabel = new Label("CONNECTING...");
        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(e -> homeScreen());

        waitingLayout.getChildren().addAll(connectingLabel, cancelButton);

        Scene waitingScene = new Scene(waitingLayout, 800, 600);
        primaryStage.setScene(waitingScene);
    }


	//
	// gameScreen
	//
	// description
	//
	// private void gameScreen() {
	// 	BorderPane gameLayout = new BorderPane();
	// 	gameLayout.setPadding(new Insets(10));
 
	// 	HBox gameAttributes = new HBox(10);
	// 	gameAttributes.setAlignment(Pos.CENTER);
 
	// 	Label playerColorLabel = new Label("You are " + playerColor);
	// 	Label playerTurn = new Label();
 
	// 	Button forfeitButton = new Button("FORFEIT");
	// 	forfeitButton.setOnAction(e -> {
	// 		clientConnection.send("FORFEIT");
	// 	});
 
	// 	gameAttributes.getChildren().addAll(playerColorLabel, playerTurn, forfeitButton);
	// 	gameLayout.setTop(gameAttributes);
 
	// 	//input game board here
 
	// 	VBox chatBox = new VBox(10);
	// 	Label chatLabel = new Label("CHAT: ");
	// 	TextField chatField = new TextField();
	// 	chatField.setPromptText("Enter message...");
	// 	chatField.setOnAction(e -> {
 
	// 	});
 
	// 	chatBox.getChildren().addAll(chatLabel, chatField);
	// 	gameLayout.setBottom(chatBox); 
	//  }
 

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
}
