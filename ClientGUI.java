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
		
		GridPane grid = new GridPane();
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
				if(client.connectToHost(nameField.getText(), userTextField.getText())) {
					toGameScreen(primaryStage);
				}
				else {
					actiontarget.setText("Failed to connect!");
				}
			}
		});
		
		Scene scene = new Scene(grid, 300, 275);
		primaryStage.setScene(scene);
		
		primaryStage.show();
	}
	
	void toGameScreen(Stage primaryStage) {
		GridPane grid = new GridPane();
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
					client.sendStart();
				}
			});
		}
		
		grid.add(cards, 0, 1);
		
		Scene scene = new Scene(grid, 500, 300);
		primaryStage.setScene(scene);
	}
}