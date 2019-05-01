/*	Programmer: Tyler Heald
	Date: 4/27/2019
	Description:
		The ClientGUI class contains all of the pieces needed to interact
		with an underlying GameClient object. The user will input their
		name, the host socket, and will select their cards they want to
		play from the GUI, and it will communicate to the GameClient object
*/

import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.canvas.*;

import java.util.Stack;

public class ClientGUI extends Application implements Runnable {

    private GameClient client;
    public final static int WIDTH = 900; // window width
    public final static int HEIGHT = 600;  // window height

    private Stage window; // main stage to display on the screen

    private Thread thread; // thread running
    private boolean running = false; // whether or not the application is still active

    private Canvas canvas; // draw canvas for the window

    /**
     * Defines the stop method for the JavaFX Application.
     * 1) Sets running to false, which kills the GUI thread.
     * 2) Joins GUI thread with Main thread, preventing it from dangling.
     * 3) Prints stacktrace and error if thread.join() fails.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("Client GUI is stopping.");
        running = false;
        try {
            if (thread != null)
                thread.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to join display thread with Main thread.");
        }
        client.disconnectFromHost();
        super.stop();
    }

    public void start(Stage primaryStage) {
        client = new GameClient();

        primaryStage.setTitle("UNO!");

        //Setting up the GridPane to hold all the gui elements
        GridPane grid = new GridPane();
        //DEBUG
        initGrid(grid);

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

        Text actionTarget = new Text();
        grid.add(actionTarget, 1, 6);

        Button btn = new Button("Connect");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);
        //Adding action to the button
        btn.setOnAction((e) -> {
            actionTarget.setFill(Color.FIREBRICK);
            actionTarget.setText("Connecting to server...");
            //Setting client name
            client.setName(nameField.getText());
            //Moving to the game screen if succesfully connected
            if (client.connectToHost(nameField.getText(), userTextField.getText())) {
                toGameScreen(primaryStage);
            } else {
                actionTarget.setText("Failed to connect!");
            }
        });

        //Setting the scene
        Scene scene = new Scene(grid, WIDTH, HEIGHT);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Render method handles screen refresh and redraw.
     * Serves as hub for all renders to the screen.
     * Currently only calls ViewHandler.render() in order
     * to draw all assigned Renderables.
     * Clears background of screen with WHITE box before
     * starting any other render process.
     */
    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        if (gc == null) {
            System.err.println("MAIN: Cannot render to Canvas when GraphicsContext is null.");
            return;
        }

        // painting the whole screen with white
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

    }

    public void tick() {
        System.out.println("Ticking nothing rn.");
    }


    public void run() {
        /*
         * Defines the activity that occurs
         *  every time the thread updates the UI.
         */
        final Runnable updater = () -> {
            render(); // Renders the Renderables at their new position
            tick(); // Updates all Renderables with whatever tick method
        };

        /*
         * Attempts to get the thread to update tickRate times a second.
         *  Not 100% accurate, so tick count frequently varies between
         *  tickRate-1 & tickRate+1 times a second even if system is
         *  sufficiently fast.
         * Tick rate will be significantly slower if the system is unable
         *  to render and tick at the set tickrate. 60.0 is very standard.
         */
        long lastUpdate = System.nanoTime();
        final double tickRate = 60.0;
        final double tickGap = Math.pow(10, 9) / tickRate;
        /*
         * Loops while thread hasn't yet joined with main thread
         *  While loop runs very fast, so tickGap is updated very
         *  frequently to check for next update.
         */
        while (running) {
            // Checks if tickGap time has passed since last update and calls update if so
            if (System.nanoTime() >= lastUpdate + tickGap) {
                // UI update is run on the Application thread to prevent thread crashes & desync
                Platform.runLater(updater);
                lastUpdate += tickGap;
            }
        }
    }


    private void initGrid(GridPane grid) {    //DEBUG
        grid.setGridLinesVisible(false);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
    }

    private void initScene(GridPane grid) {
        Label playerLabel = new Label();

        final String labelText = String.format("%s - %s", client.isLeader() ? "Leader" : "Player", client.getName());
        playerLabel.setText(labelText);

        playerLabel.setTextFill(Color.WHITE);


        grid.add(playerLabel, 0, 0);

        //Creating an ObservableList<> for ListView, and the Listview for cards
        ObservableList<String> cardList = FXCollections.observableArrayList();
        ListView<String> cards = new ListView<>(cardList);

        //Creating a button just for the leader that tells the server to start the game
        if (client.isLeader()) {
            Button startButton = new Button("Start Game");
            HBox btnBx = new HBox(10);
            btnBx.getChildren().add(startButton);
            btnBx.setAlignment(Pos.TOP_RIGHT);
            grid.add(btnBx, 1, 0);
            startButton.setOnAction((e) -> {
                client.sendStart();     //Telling the client to let the server know it needs to start
                client.listen();        //Telling the client to start listening to the server
            });
        } else {
            //Client is not the leader, just needs to listen to the server
            client.listen();
        }

        //Adding the listView for all of the players cards
        grid.add(cards, 0, 1);
    }


    //Method to set the scene to the screen for playing the game
    private void toGameScreen(Stage primaryStage) {
        GridPane grid = new GridPane();

        canvas = new Canvas(WIDTH, HEIGHT);

        StackPane stack = new StackPane();
        stack.getChildren().add(canvas);
        stack.getChildren().add(grid);

        initGrid(grid);
        initScene(grid);

        thread = new Thread(this);
        thread.setDaemon(false);

        running = true;

        Scene scene = new Scene(stack, WIDTH, HEIGHT);
        primaryStage.setScene(scene);

        thread.start();
    }


    void addCard(ListView<String> cards, String card) {

    }

    void removeCard(ListView<String> cards, String card) {

    }

    /*
        void topOfDiscard(ItemToDisplayOn, String card) {

        }

        void previousMove(ItemToDisplayOn, String player, String move) {

        }
    */
    void illegalMove() {

    }

    void isTurn() {

    }

    void winner(String player) {

    }

    void numOfCards(String player, int num) {

    }

}