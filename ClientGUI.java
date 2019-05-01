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
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
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

public class ClientGUI extends Application implements Runnable {

    private GameClient client;
    public final static int WIDTH = 1280; // window width
    public final static int HEIGHT = 720;  // window height

    private volatile String selectedCard = null;
    private volatile String otherInput = null;

    private Thread thread; // thread running
    private boolean running = false; // whether or not the application is still active

    private Canvas canvas; // draw canvas for the window
    private Image img;

    private Label prompt;

    private boolean cardsChanged = true;

    //Creating an ObservableList<> for ListView, and the Listview for cards
    private ObservableList<String> cardList = FXCollections.observableArrayList();
    private ListView<String> cards = new ListView<>(cardList);

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
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to join display thread with Main thread.");
        }
        client.disconnectFromHost();
        super.stop();

        System.out.println("System exiting.");

        System.exit(0);
    }

    private void handleLogin(Stage primaryStage, TextField nameField, TextField userTextField, Text actionTarget) {
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
    }

    public String getSelectedCard() {
        return selectedCard;
    }

    public String getOtherInput() {
        return otherInput;
    }

    public void voidOtherInput() {
        Platform.runLater(() -> otherInput = null);
    }


    public void updateCards(Hand hand) {
        Platform.runLater(() -> {
            cardList.clear();
            for (Card c : hand.getActiveCards()) {
                cardList.add(c.getString());
            }
        });
    }


    public void voidSelectedCard() {
        Platform.runLater(() ->
                selectedCard = null);
    }

    public void setPrompt(String labelText) {
        Platform.runLater(() -> {
            prompt.setText(labelText);
        });
    }

    public void start(Stage primaryStage) {
        client = new GameClient(this);

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

        cards.setOnMouseClicked((e) -> {
            handleItemSelection();
        });

        userTextField.setOnKeyPressed((e) -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleLogin(primaryStage, nameField, userTextField, actionTarget);
            }
        });

        //Adding action to the button
        btn.setOnAction((e) -> {
            handleLogin(primaryStage, nameField, userTextField, actionTarget);
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

        gc.drawImage(img, 0, 0, WIDTH, HEIGHT);

    }

    public void tick() {
        if (cardList.isEmpty()) {
            for (Card card : client.getCards().getActiveCards()) {
                cardList.add(card.getString());
            }
        } else if (cardsChanged) {
            cardList.clear();

            for (Card card : client.getCards().getActiveCards()) {
                cardList.add(card.getString());
            }

            cardsChanged = false;
        }
    }


    public void run() {
        /*
         * Defines the activity that occurs
         *  every time the thread updates the UI.
         */
        final Runnable updater = () -> {
            render();
            tick();
        }; // Renders the Renderables at their new position

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

    private void handleItemSelection() {
        selectedCard = cards.getSelectionModel().getSelectedItem();
    }


    private void initGrid(GridPane grid) {    //DEBUG
        grid.setGridLinesVisible(false);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
    }

    private void initScene(GridPane grid) {
        final String labelText = String.format("%s - %s", client.isLeader() ? "Leader" : "Player", client.getName());

        Label playerLabel = new Label(labelText);
        playerLabel.setTextFill(Color.WHITE);
        grid.add(playerLabel, 0, 0);

        Button playCardBtn = new Button("Play Card");
        grid.add(playCardBtn, 1, 1);

        playCardBtn.setOnAction((e) -> {
            handleItemSelection();
        });


        prompt = new Label("Prompt Text.");
        prompt.setTextFill(Color.WHITE);
        grid.add(prompt, 2, 1);


        TextField enterColor = new TextField();
        enterColor.setOnKeyPressed((e) ->
        {
            if (e.getCode() == KeyCode.ENTER) {
                otherInput = enterColor.getText();
            }
        });

        grid.add(enterColor, 2, 2);


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
        primaryStage.setTitle("Uno!??");

        canvas = new Canvas(WIDTH, HEIGHT);

        StackPane stack = new StackPane();
        stack.getChildren().add(canvas);
        stack.getChildren().add(grid);

        initGrid(grid);
        initScene(grid);

        try {
            img = new Image(String.format("file:%s", "res/bkg.jpg"));
        } catch (Exception e) {
            System.err.println("Unable to load image.");
        }

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