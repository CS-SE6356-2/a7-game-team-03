import javafx.util.Pair;

import java.util.*;
import java.net.*;
import java.io.*;

public class GameServer {
    private static ServerSocket listenSock = null;
    private static DataOutputStream outStream = null;
    private static DataInputStream inStream = null;

    private static ArrayList<ClientPair> clients = new ArrayList<ClientPair>();
    //Variable for numOfClients for thread purposes
    private static int numOfClients = 0;
    private static int MAX_PLAYERS = 10;
    // this is a change

    private static CardGame uno = null;

    private static void printListening() {
        try {
            System.out.println("Listening on: " + InetAddress.getLocalHost().getHostAddress() + ":" + listenSock.getLocalPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static void distributeCards(CardGame cardGame) {
        //Deal the cards
        //NOTE: the CardGame will itself notify the client of the cards they have
        //PERHAPS NOT? KEEP SERVER AND LOGIC SEPARATE MAYBE
        cardGame.dealCards();
        //DEBUG
        cardGame.printHands();
        System.out.print("\n");
        cardGame.printDecks();
    }

    private static Pair<Socket, Thread> decideLeader() throws IOException {
        Socket clientSock = listenSock.accept();
        Thread cliSet = new Thread(new ClientSetup(clientSock, new DataInputStream(clientSock.getInputStream()),
                new DataOutputStream(clientSock.getOutputStream()), clients));
        //Thread to set-up the client by adding their details to clients arraylist
        cliSet.start();
        numOfClients = 1;
        System.out.println("Leader client on: " + InetAddress.getLocalHost().getHostAddress() + ":" + clientSock.getLocalPort());

        return new Pair<>(clientSock, cliSet);
    }

    private static void beginGame(CardGame cardGame, PlayerQueue playOrder) throws IOException {

        //Bool to check if a skip is still needed in the following game loop
        boolean needToSkip = false;
        boolean firstTurn = true;

        //while(true) to just run until someone wins
        while (true) {
            //DEBUG
            cardGame.printHands();
            //DEBUG
            System.out.println("LAST PLAYED: " + cardGame.lastPlayed().getString());
            System.out.printf("needToSkip = %s\n", needToSkip ? "true" : "false");

            //DEEBUG
            System.out.println(playOrder.getPlayer().getName());
            //Setup to talk communicate with current player
            Socket currentPlayer = playOrder.getPlayer().getSocket();
            DataInputStream currentIn = new DataInputStream(currentPlayer.getInputStream());
            DataOutputStream currentOut = new DataOutputStream(currentPlayer.getOutputStream());

            //ACTION CARDS TAKE EFFECT EVEN ON THE FIRST TURN,
            //EXCEPT FOR WILD DRAW4, WHICH AUTOMATICALLY GETS RESHUFFLED
            //IF IT STARTS A DISCARD PILE
            if (cardGame.lastPlayed().getVal().equals("skip") && needToSkip) {
                firstTurn = false;
                //DEBUG
                System.out.println("in skip");
                //Notify all of skippage
                for (ClientPair client : clients) {
                    DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " skipped");
                }
                //Skip
                playOrder.nextPlayer();
                needToSkip = false;
                continue;
            } else if (cardGame.lastPlayed().getVal().equals("draw2") && needToSkip) {
                firstTurn = false;
                //DEBUG
                System.out.println("in draw2");
                //Draw 2 cards, then skip
                for (int i = 0; i < 2; i++) {
                    String drawnCard = cardGame.drawCard(playOrder.getPlayer());
                    //Notify of card drawn
                    currentOut.writeUTF("draw " + drawnCard);
                }
                //Notify all of skippage
                for (ClientPair client : clients) {
                    DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " drew2 skipped");
                }
                //Skip
                playOrder.nextPlayer();
                needToSkip = false;
                continue;
            } else if (cardGame.lastPlayed().getVal().equals("draw4") && needToSkip) {
                firstTurn = false;
                //DEBUG
                System.out.println("in draw4");
                //Draw 4, then skip
                for (int i = 0; i < 4; i++) {
                    String drawnCard = cardGame.drawCard(playOrder.getPlayer());
                    //Notify of card drawn
                    try {
                        currentOut.writeUTF("draw " + drawnCard);
                    } catch (IOException e) {
                    }
                }
                //Notify all of skippage
                for (ClientPair client : clients) {
                    DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " drew4 skipped");
                }
                //Skip
                playOrder.nextPlayer();
                needToSkip = false;
                continue;
            }
            //Reversing the play order only if on the first round
            else if (cardGame.lastPlayed().getVal().equals("reverse") && firstTurn) {
                firstTurn = false;
                //DEBUG
                System.out.println("in reverse");
                //Reverse the order, and go to the right player
                playOrder.reverseOrder();
                playOrder.nextPlayer();
                //Change the player communication stuff
                currentPlayer = playOrder.getPlayer().getSocket();
                currentIn = new DataInputStream(currentPlayer.getInputStream());
                currentOut = new DataOutputStream(currentPlayer.getOutputStream());
            }
            //No longer need to know its the first turn
            if (firstTurn) {
                firstTurn = false;
            }
            //String to store the player's move
            String move = "";
            //Notify the player it is their turn
            //DEBUG
            System.out.println("Sending turn");
            currentOut.writeUTF("turn");
            //Tell everyone who's turn it is
            for (ClientPair client : clients) {
                DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                outToYou.writeUTF("turn " + playOrder.getPlayer().getName());
            }

            //Check if the player has a legal move
            if (cardGame.hasLegalPlay(playOrder.getPlayer())) {
                //DEBUG
                System.out.println(playOrder.getPlayer().getName() + " has a move");
                //They do, they must play a card
                currentOut.writeUTF("play");
                //Receive the card played
                move = currentIn.readUTF();
                //check if its a legal move
                while (!cardGame.isLegalMove(playOrder.getPlayer(), move)) {
                    //no, notify, and wait for legal move
                    currentOut.writeUTF("illegal move");
                    move = currentIn.readUTF();
                }
                //Yes, notify and play
                currentOut.writeUTF("legal");
				//Checking if move requires a skip
				String[] moveCheck = move.split(" ");
				if(moveCheck[0].equals("draw4") || moveCheck[0].equals("draw2")
					|| moveCheck[0].equals("skip")) {
						needToSkip = true;
					}
                //Have to add play to the card string
                move = "play " + move;
                cardGame.makeMove(playOrder.getPlayer(), move);

            } else {
                //DEBUG
                System.out.println(playOrder.getPlayer().getName() + " doesn't have a move");
                //They don't have a legal move, must draw a card
                String drawnCard = cardGame.drawCard(playOrder.getPlayer());
                //Notify of card drawn
                currentOut.writeUTF("draw " + drawnCard);

                //Set move to drew for notifying others
                move = "drew";
            }

            //Update the cardGame as to move taken(card played, card drawn)
            //UPDATE FOR ACTION CARDS
            //Tell other players what happened (who played what card, whats on the
            //top of the discard, how many cards everyone has)
            for (ClientPair client : clients) {
                DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                if (move.equals("drew")) {
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " drew");
                } else {
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " used " + move);
                }
            }
            //Check for uno being "called"

            //Check if the player won
            //yes, notify everyone, end game
            //no, keep going
            if (cardGame.won(playOrder.getPlayer())) {
                //yes, notify everyone and end game
                for (ClientPair client : clients) {
                    DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
                    outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " won");
                }
                //exit while loop
                break;
            }
            //no, keep going

            //Tell everyone who has how many cards
            //for(

            //Reversing the order if needed
            if (cardGame.lastPlayed().getVal().equals("reverse")) {
                playOrder.reverseOrder();
            }

            //Go to the next player, and repeat.
            playOrder.nextPlayer();
        }
    }


    private static Thread initAllClients(Thread leadStart) throws IOException {
        //Connecing other clients while the leader hasn't started the game, or there
        //are less than 10 players at least started connecting
        Socket clientSock = null;
        Thread cliSet = null;
        while (leadStart.isAlive() && clients.size() < 10) {
            //Listening for another client
            //If the connection times out, tries again
            try {
                clientSock = listenSock.accept();
            } catch (SocketTimeoutException e) {
                continue;
            }

            System.out.println("New client on: " + InetAddress.getLocalHost().getHostAddress() + ":" + clientSock.getLocalPort());
            //Thread to add the new client to the arrayLists
            cliSet = new Thread(new ClientSetup(clientSock, new DataInputStream(clientSock.getInputStream()),
                    new DataOutputStream(clientSock.getOutputStream()), clients));
            cliSet.start();
            ++numOfClients;
            //Sending player specification
            DataOutputStream playerOut = new DataOutputStream(clientSock.getOutputStream());
            playerOut.writeUTF("player");
        }

        return cliSet;
    }

    private static void initiateClientDisconnect() throws IOException {
        for (ClientPair client : clients) {
            DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
            out.writeUTF("disconnect");
            client.getSocket().close();
        }
    }


    private static void beginGame() {
        try {
            //First client is the leader of the game
            Pair<Socket, Thread> leaderResult = decideLeader();

            Socket clientSock = leaderResult.getKey();
            Thread cliSet = leaderResult.getValue();

            //Sending the thread to leadStart
            Thread leadStart = new Thread(new LeaderControl(cliSet, new DataInputStream(clientSock.getInputStream()),
                    new DataOutputStream(clientSock.getOutputStream())));
            //Thread to listen for when the leader starts the game
            leadStart.start();

            //Setting the ServerSocket to timeout after 3 seconds
            listenSock.setSoTimeout(3000);

            cliSet = initAllClients(leadStart);

            //Wait for last client to connect
            try {
                cliSet.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //DEBUG

            //Waiting for the leader to say start?
            System.out.println("Game start!");
            //Create the CardGame object with player info
            CardGame cardGame = new CardGame(numOfClients, clients, new File("cardlist"));
            distributeCards(cardGame);

            //Get a PlayerQueue to run in order
            PlayerQueue playOrder = cardGame.sortPlayersInPlayOrder();

            beginGame(cardGame, playOrder);

            //DEBUG END
            initiateClientDisconnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Trouble communicating with a client.");
        }
    }


    private static void initListenSocket() {
        try {
            listenSock = new ServerSocket(0);
        } catch (IOException e) {
            System.out.println("No server");
        }
    }


    public static void main(String[] args) {
        // Initializing listener socket
        initListenSocket();

        //PRINTING IP
        printListening();

        // game start
        beginGame();
    }
}

//Thread to set up the clients by adding their details to clients
class ClientSetup implements Runnable {
    //FIELDS
    private Socket clientSock;

    private ArrayList<ClientPair> clients;

    private DataInputStream in;
    private DataOutputStream out;

    //CONSTRUCTOR
    ClientSetup(Socket cliSock, DataInputStream input, DataOutputStream output,
                ArrayList<ClientPair> clientStuff) {
        clientSock = cliSock;
        clients = clientStuff;

        in = input;
        out = output;
    }

    //@Override
    public void run() {
        try {
            String name = in.readUTF();
            clients.add(new ClientPair(clientSock, name));
            out.writeUTF("Connected!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Printing clients connected
        System.out.println("Clients connected:");
        for (ClientPair player : clients) {
            System.out.println(player.getName());
        }
    }
}

class LeaderControl implements Runnable {
    //FIELDS
    private DataInputStream inFromLead = null;
    private DataOutputStream outToLead = null;
    private Thread thisSetup = null;

    private static final int MAX_ATTEMPTS = 10;

    //CONSTRUCTOR
    LeaderControl(Thread cliSet, DataInputStream datStream, DataOutputStream outStream) {
        inFromLead = datStream;
        outToLead = outStream;
        thisSetup = cliSet;
    }

    private void attemptThreadJoin() {
        //Wait until the client setup is done before sending messages
        try {
            thisSetup.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void run() {
        attemptThreadJoin();

        String messFromLead = "";
        //Tell leader they are indeed the leader
        try {
            outToLead.writeUTF("leader");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int attempts = 0;
        while (!messFromLead.equals("start")) {
            try {
                //Telling the lead to respond
                outToLead.writeUTF("respond");
                messFromLead = inFromLead.readUTF();
            } catch (IOException e) {
                ++attempts;
                System.err.printf("Unable to get message from server. Attempt number: %d.\n", attempts);

                if (attempts == MAX_ATTEMPTS) {
                    System.err.println("Quitting");
                    return;
                }
            }
        }
    }
}