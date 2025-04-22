

import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application{

	Client clientConnection;

	ComboBox<Integer> listUsers;
	ListView<String> listItems;

	public static void main(String[] args) {
		// Client clientThread = new Client();
		// clientThread.start();

		// Scanner s = new Scanner(System.in);
		// while (s.hasNext()){
		// 	String x = s.next();
		// 	clientThread.send(x);
		// }

		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
        // Initialize the client thread
		clientConnection = new Client(data->{
				Platform.runLater(()->{
					switch (data.type){
						case NEWUSER:
							listUsers.getItems().add(data.recipient);
							listItems.getItems().add(data.recipient + " has joined!");
							break;
						case DISCONNECT:
							listUsers.getItems().remove(data.recipient);
							listItems.getItems().add(data.recipient + " has disconnected!");
							break;
						case TEXT:
							listItems.getItems().add(data.recipient+": "+data.message);
					}
			});
		});
        clientConnection.start();

        chatScreen(primaryStage);
	}

	// 
	// loginScreen
	//
	// Method for the login/register screen
	// user will input their username and password to sign in
	// or they can register for a new account
	//
	// private void loginScreen(Stage primaryStage) {
	// 	GridPane grid = new GridPane();
	// 	grid.setHgap(10);
	// 	grid.setVgap(10);

	// 	// username and password fields
	// 	Label usernameLabel = new Label("Username:");
	// 	TextField usernameField = new TextField();
    //     grid.add(usernameLabel, 0, 0);
    //     grid.add(usernameField, 1, 0);

    //     Label messageLabel = new Label();
	// 	TextField passwordField = new TextField();

	// }

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
