/*	Programmer: Tyler Heald
	Date: 4/27/2019
	Description:
		The ClientGUI class contains all of the pieces needed to interact
		with an underlying GameClient object. The user will input their
		name, the host socket, and will select their cards they want to
		play from the GUI, and it will communicate to the GameClient object
*/
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

public class ClientGUI extends Application {
	
	GameClient client = new GameClient();
	
	public void start(Stage primaryStage) {
		primaryStage.setTitle("UNO!");
		
		//Setting up the GridPane to hold all the gui elements
		GridPane grid = new GridPane();
		//DEBUG
		grid.setGridLinesVisible(true);
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));
		
		Text welcome = new Text("Welcome to UNO!");
		welcome.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		grid.add(welcome, 0, 0, 2, 1);
		
		Label name = new Label("Name:");
		grid.add(name, 0, 1);
		TextField nameField = new TextField();
		grid.add(nameField, 1, 1);
		
		Label hostAddress = new Label("Host Address:");
		grid.add(hostAddress, 0, 2);
		
		TextField userTextField = new TextField();
		grid.add(userTextField, 1, 2);
		
		Text actiontarget = new Text();
		grid.add(actiontarget, 1, 6);
		
		Button btn = new Button("Connect");
		HBox hbBtn = new HBox(10);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btn);
		grid.add(hbBtn, 1, 4);
		//Adding action to the button
		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				actiontarget.setFill(Color.FIREBRICK);
				actiontarget.setText("Connecting to server...");
				//Setting client name
				client.setName(nameField.getText());
				//Moving to the game screen if succesfully connected
				if(client.connectToHost(nameField.getText(), userTextField.getText())) {
					toGameScreen(primaryStage);
				}
				else {
					actiontarget.setText("Failed to connect!");
				}
			}
		});
		
		//Setting the scene
		Scene scene = new Scene(grid, 300, 275);
		primaryStage.setScene(scene);
		
		primaryStage.show();
	}
	
	//Method to set the scene to the screen for playing the game
	void toGameScreen(Stage primaryStage) {
		GridPane grid = new GridPane();
		//DEBUG
		grid.setGridLinesVisible(true);
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));
		
		Label playerLabel = new Label();
		if(client.isLeader()) {playerLabel.setText("Leader - " + client.getName());}
		else {playerLabel.setText("Player - " + client.getName());}
		grid.add(playerLabel, 0, 0);
		
		//Creating an ObservableList<> for ListView, and the Listview for cards
		ObservableList<String> cardList = FXCollections.observableArrayList();
		ListView<String> cards = new ListView<String>(cardList);
		
		//Creating a button just for the leader that tells the server to start the game
		if(client.isLeader()) {
			Button startButton = new Button("Start Game");
			HBox btnBx = new HBox(10);
			btnBx.getChildren().add(startButton);
			btnBx.setAlignment(Pos.TOP_RIGHT);
			grid.add(btnBx, 1, 0);
			startButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					//Telling the client to let the server know it needs to start
					client.sendStart();
					//Telling the client to start listening to the server
					client.listen();
				}
			});
		}
		else {
			//Client is not the leader, just needs to listen to the server
			client.listen();
		}
		
		//Adding the listView for all of the players cards
		grid.add(cards, 0, 1);
		
		Scene scene = new Scene(grid, 500, 300);
		primaryStage.setScene(scene);
	}
	
	//Methods for responding to client messages
	/*
	void addCard(ListView<String> cards, String card) {
		
	}
	void removeCard(ListView<String> cards, String card) {
		
	}
	void topOfDiscard(ItemToDisplayOn , String card) {
		
	}
	void previousMove(ItemToDisplayOn , String player, String move) {
		
	}
	void illegalMove() {
		
	}
	void isTurn() {
		
	}
	void winner(String player) {
		
	}
	void numOfCards(String player, int num) {
		
	}
	*/
}