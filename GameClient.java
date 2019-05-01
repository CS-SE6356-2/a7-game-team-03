/*	Programmer: Tyler Heald
	Date: 4/27/2019
	Description:
		The GameClient class communicates between the GUI and
		the server. GameClient objects contain information about
		the player using them, and relay that information to the
		GUI, and relay commands to the server, which responds with
		information about the state of the CardGame.
	Methods:
		connectToHost - Takes a player name, and a host ip and
			sets up a connection when the GUI calls for it
		disconnectFromHost - disconnects from the host
			when the GUI calls for it
		sendStart - If the client is the game leader, sends a 
			message to the host that the game needs to start
		isLeader
		get/setName
*/

import javafx.application.Platform;

import java.util.*;
import java.io.*;
import java.net.*;

public class GameClient {
    private Socket hostSock = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    private ClientGUI gui;

    private String name = null;
    private String serverMessage = "";

    private boolean leader = false;

    private Thread serverListen = null;
    private Hand cards = new Hand();

    //CONSTRUCTOR
    GameClient(ClientGUI g) {
        gui = g;
    }

    GameClient() {
    }

    public Hand getCards() {
        return cards;
    }

    //Connects to the host specified in the text field on the GUI
    //when the connect button is pressed
    boolean connectToHost(String name, String socket) {
        String[] ipAndPort = socket.split(":");
        try {
            //Connecting to the host
            hostSock = new Socket(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            out = new DataOutputStream(hostSock.getOutputStream());
            in = new DataInputStream(hostSock.getInputStream());

            //Sent to the server as a client label
            out.writeUTF(name);
            //Read in the messages until player or leader specification
            while (!serverMessage.equals("leader") && !serverMessage.equals("player")) {
                serverMessage = in.readUTF();
            }
            //Checking specification
            if (serverMessage.equals("leader")) {
                leader = true;
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    //Disconnects from host when GUI calls for it (not currently used)
    public boolean disconnectFromHost() {
        if (hostSock == null) {
            return false;
        } else {
            try {
                hostSock.close();
                if (serverListen != null)
                    serverListen.join();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    //A method for the leader client to tell the server to start the game
    boolean sendStart() {
        try {
            out.writeUTF("start");
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    //A method to start the client listening constantly for messages from the server
    void listen() {
        serverListen = new Thread(new ListenThread(out, in, gui, cards, () -> {
            String selected = null;
            while (gui == null || (selected = gui.getSelectedCard()) == null) {
            }
            gui.voidSelectedCard();

            return selected;
        }, (String output) -> {
            gui.setPrompt(output);
        }, () -> {
            String data = null;
            while (gui == null || (data = gui.getOtherInput()) == null) {
            }
            gui.voidOtherInput();

            return data;
        }));


        serverListen.start();
    }

    //GETTERS/SETTERS
    boolean isLeader() {
        return leader;
    }

    void setName(String n) {
        name = n;
    }

    String getName() {
        return name;
    }
}


//Thread to listen to messages from the server
class ListenThread implements Runnable {
    interface Input {
        String get();
    }

    interface InternalOutput {
        void pipe(String output);
    }

    private DataOutputStream out;
    private DataInputStream in;

    private ClientGUI gui;

    private volatile Hand cards;

    private Scanner input;

    private Input read;
    private InternalOutput pipe;
    private Input otherPipe;


    ListenThread(DataOutputStream o, DataInputStream i, ClientGUI g, Hand c, Input read, InternalOutput pipe, Input pipe2) {
        out = o;
        in = i;
        gui = g;
        cards = c;

        this.read = read;
        this.pipe = pipe;
        this.otherPipe = pipe2;
    }


    private String wildCardLoop() {
        //Prompt for color
        String color = "";
        while (!color.equals("red") && !color.equals("blue")
                && !color.equals("green") && !color.equals("yellow")) {
            pipe.pipe("What color do you want?");
            color = otherPipe.get();
        }

        return color;
    }


    public void run() {
        //DEBUG
        String message = "";
        while (!message.equals("disconnect")) {
            //Listen for messages from the server
            try {
                message = in.readUTF();
            } catch (IOException e) {
            }

            if (message.length() == 0) continue;
            //DEBUG
            pipe.pipe(message);
            //Splitting the message into its parts
            String[] messParts = message.split(" ");

            //Checking what the message header is
            //Draw a card
            if (messParts[0].equals("draw")) {
                //Add the card to the hand
                Card newCard = new Card(messParts[1], messParts[2]);
                //DEBUG
                cards.addCard(newCard);
                gui.updateCards(cards);
                //Send the new card to the gui
            } else if (messParts[0].equals("turn")) {
                //Check if there its someone elses turn
                if (messParts.length > 1) {
                    //is someone elses move
                } else {
                    //Will be receiving unneeded turn [name] message, get it off socket
                    try {
                        message = in.readUTF();
                    } catch (IOException e) {
                    }
                    //check for "play".
                    //yes, play card
                    //no, listen for cards drawn
                    try {
                        message = in.readUTF();
                        //DEBUG
                        pipe.pipe(message);
                    } catch (IOException e) {
                    }
                    if (message.equals("play")) {
                        //this players move
                        //Tell the GUI to let a card be played.
                        //When one is, GUI will call a method to
                        //send the card to the server through client
                        //gui.getMove();
                        //DEBUG
                        pipe.pipe("Enter the card you want to play:");
                        String cardPlay = read.get();
                        //Send to server
                        //Check for wilds
                        String[] cardCheck = cardPlay.split(" ");
                        if (cardCheck[1].equals("wild")) {
                            //make the new card
                            String color = wildCardLoop();
                            cardPlay = cardCheck[0] + " " + color;
                        }
                        try {
                            pipe.pipe("Want to play card " + cardPlay);
                            out.writeUTF(cardPlay);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //Checking for illegal move
                        try {
                            message = in.readUTF();
                        } catch (IOException e) {
                        }
                        while (message.equals("illegal move")) {
                            //gui.getMove();
                            cards.printCards();
                            pipe.pipe("Enter the card you want to play:");
                            cardPlay = read.get();
                            cardCheck = cardPlay.split(" ");
                            //Check for a wild card
                            if (cardCheck[1].equals("wild")) {
                                //Prompt for color
                                String color = wildCardLoop();
                                //make the new card
                                cardPlay = cardCheck[0] + " " + color;
                            }
                            try {
                                out.writeUTF(cardPlay);
                                message = in.readUTF();
                            } catch (IOException e) {
                            }
                        }
                        //DEBUG
                        pipe.pipe("made legal move");
                        //Move was legal, server will say it was legal
                        //client will remove from hand
                        String[] cardPlayed = cardPlay.split(" ");
                        LinkedList<Card> temp = new LinkedList<Card>();
                        //Have to check for wild & draw4
                        if (cardPlayed[0].equals("draw4")) {
                            temp.add(new Card("draw4", "wild"));
                        } else if (cardPlayed[0].equals("wild")) {
                            temp.add(new Card("wild", "wild"));
                        } else {
                            temp.add(new Card(cardPlayed[0], cardPlayed[1]));
                        }
                        cards.removeCards(temp);
                        gui.updateCards(cards);
                        //DEBUG
                        pipe.pipe("Removed " + temp.get(0).getString());
                    } else {
                        //Listen for card drawn
                        pipe.pipe("drawing a card");
                        //split the string to get the card
                        //will be in [1], [2]
                        String[] cardParts = message.split(" "); // message is the drawn card
                        cards.addCard(new Card(cardParts[1], cardParts[2]));
                        gui.updateCards(cards);
                    }
                }
            }
        }
    }
}