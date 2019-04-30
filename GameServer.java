import java.util.*;
import java.net.*;
import java.io.*;

public class GameServer{
	static ServerSocket listenSock = null;
	static DataOutputStream outStream = null;
	static DataInputStream inStream = null;
	
	static ArrayList<ClientPair> clients = new ArrayList<ClientPair>();
	//Variable for numOfClients for thread purposes
	static int numOfClients = 0;
	
	static CardGame uno;
	
	
	public static void main(String[] args)
	{
		try {
			listenSock = new ServerSocket(0);
		}
		catch(IOException e){System.out.println("No server");}
		
		//PRINTING IP
		try {
			System.out.println("Listening on: " + InetAddress.getLocalHost().getHostAddress() +":"+ listenSock.getLocalPort());
		}
		catch(UnknownHostException e) {}
		
		try {
			//First client is the leader of the game
			Socket clientSock = listenSock.accept();
			Thread cliSet = new Thread(new clientSetup(clientSock, new DataInputStream(clientSock.getInputStream()),
					new DataOutputStream(clientSock.getOutputStream()), clients));
			//Thread to set-up the client by adding their details to clients arraylist
			cliSet.start();
			numOfClients++;
			System.out.println("Leader client on: " + clientSock.getInetAddress().getLocalHost().getHostAddress() + ":" + clientSock.getLocalPort());
			
			//Sending the thread to leadStart
			Boolean startGame = false;
			Thread leadStart = new Thread(new leaderControl(cliSet, new DataInputStream(clientSock.getInputStream()),
					new DataOutputStream(clientSock.getOutputStream())));
			//Thread to listen for when the leader starts the game
			leadStart.start();
			
			//Setting the ServerSocket to timeout after 3 seconds
			listenSock.setSoTimeout(3000);
			//Connecing other clients while the leader hasn't started the game, or there
			//are less than 10 players at least started connecting
			while(leadStart.isAlive() && clients.size() < 10) {
				//Listening for another client
				//If the connection times out, tries again
				try{
					clientSock = listenSock.accept();
				}
				catch(SocketTimeoutException e) {continue;}
				
				System.out.println("New client on: " + clientSock.getInetAddress().getLocalHost().getHostAddress() + ":" + clientSock.getLocalPort());
				//Thread to add the new client to the arrayLists
				cliSet = new Thread(new clientSetup(clientSock, new DataInputStream(clientSock.getInputStream()),
						new DataOutputStream(clientSock.getOutputStream()), clients));
				cliSet.start();		
				numOfClients ++;
				//Sending player specification
				DataOutputStream playerOut = new DataOutputStream(clientSock.getOutputStream());
				playerOut.writeUTF("player");
			}
			//Wait for last client to connect
			try{
				cliSet.join();
			} catch(InterruptedException e) {}
			//DEBUG
			//Waiting for the leader to say start?
			
			System.out.println("Game start!");
			//Create the CardGame object with player info
			CardGame cardGame = new CardGame(numOfClients, clients, new File("cardlist"));
			//Deal the cards
			//NOTE: the CardGame will itself notify the client of the cards they have
				//PERHAPS NOT? KEEP SERVER AND LOGIC SEPARATE MAYBE
			cardGame.dealCards();
			//DEBUG
			cardGame.printHands();
			System.out.print("\n");
			cardGame.printDecks();
			
			//Get a PlayerQueue to run in order
			PlayerQueue playOrder = cardGame.sortPlayersInPlayOrder();
			
			//Bool to check if a skip is still needed in the following game loop
			boolean skipped = false;
			boolean firstTurn = true;
			
			//while(true) to just run until someone wins
			while(true) {
				//DEBUG
				cardGame.printHands();
				//DEBUG
				System.out.println("LAST PLAYED: " + cardGame.lastPlayed().getString());
				if(skipped) {
					System.out.println("!skipped = false");
				}
				else {
					System.out.println("!skipped = true");
				}
				//DEEBUG
				System.out.println(playOrder.getPlayer().getName());
				//Setup to talk communicate with current player
				Socket currentPlayer = playOrder.getPlayer().getSocket();
				DataInputStream currentIn = new DataInputStream(currentPlayer.getInputStream());
				DataOutputStream currentOut = new DataOutputStream(currentPlayer.getOutputStream());
				
				//ACTION CARDS TAKE EFFECT EVEN ON THE FIRST TURN,
				//EXCEPT FOR WILD DRAW4, WHICH AUTOMATICALLY GETS RESHUFFLED
				//IF IT STARTS A DISCARD PILE
				if(cardGame.lastPlayed().getVal().equals("skip") && !skipped) {
					firstTurn = false;
					//DEBUG
					System.out.println("in skip");
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " skipped");
					}
					//Skip
					playOrder.nextPlayer();
					skipped = true;
					continue;
				}
				else if(cardGame.lastPlayed().getVal().equals("draw2") && !skipped) {
					firstTurn = false;
					//DEBUG
					System.out.println("in draw2");
					//Draw 2 cards, then skip
					for(int i = 0; i < 2; i++) {
						String drawnCard = cardGame.drawCard(playOrder.getPlayer());
						//Notify of card drawn
						currentOut.writeUTF("draw " + drawnCard);
					}
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + "drew2 skipped");
					}
					//Skip
					playOrder.nextPlayer();
					skipped = true;
					continue;
				}
				else if(cardGame.lastPlayed().getVal().equals("draw4") && !skipped) {
					firstTurn = false;
					//DEBUG
					System.out.println("in draw4");
					//Draw 4, then skip
					for(int i = 0; i < 4; i++) {
						String drawnCard = cardGame.drawCard(playOrder.getPlayer());
						//Notify of card drawn
						try {
							currentOut.writeUTF("draw " + drawnCard);
						}
						catch(IOException e) {}
					}
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + "drew4 skipped");
					}
					//Skip
					playOrder.nextPlayer();
					skipped = true;
					continue;
				}
				//Reversing the play order only if on the first round
				else if(cardGame.lastPlayed().getVal().equals("reverse") && firstTurn) {
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
				if(firstTurn) { firstTurn = false;}
				//resetting skipped
				if(skipped) {
					skipped = false;
				}
				//String to store the player's move
				String move = "";
				//Notify the player it is their turn
				//DEBUG
				System.out.println("Sending turn");
				currentOut.writeUTF("turn");
				//Tell everyone who's turn it is
				for(ClientPair client : clients) {
					DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
					outToYou.writeUTF("turn " + playOrder.getPlayer().getName());
				}
				
				//Check if the player has a legal move
				if(cardGame.hasLegalPlay(playOrder.getPlayer())) {
					//DEBUG
					System.out.println(playOrder.getPlayer().getName() + " has a move");
					//They do, they must play a card
					currentOut.writeUTF("play");
					//Receive the card played
					move = currentIn.readUTF();
					//check if its a legal move
					while(!cardGame.isLegalMove(playOrder.getPlayer(), move)) {
						//no, notify, and wait for legal move
						currentOut.writeUTF("illegal move");
						move = currentIn.readUTF();
					}
					//Yes, notify and play
					currentOut.writeUTF("legal");
					//Have to add play to the card string
					move = "play " + move;
					cardGame.makeMove(playOrder.getPlayer(), move);
					
				}
				else {
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
				for(ClientPair client : clients) {
					DataOutputStream outToYou = new DataOutputStream(client.getSocket().getOutputStream());
					if(move.equals("drew")) {
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " drew");
					}
					else {
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " used " + move);
					}
				}
				//Check for uno being "called"
				
				//Check if the player won
					//yes, notify everyone, end game
					//no, keep going
				if(cardGame.won(playOrder.getPlayer())) {
					//yes, notify everyone and end game
					for(ClientPair client : clients) {
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
				if(cardGame.lastPlayed().getVal().equals("reverse")) {
					playOrder.reverseOrder();
				}
				
				//Go to the next player, and repeat.
				playOrder.nextPlayer();
			}
			
			//DEBUG END
			for(ClientPair client : clients) {
				DataOutputStream out = new DataOutputStream(client.getSocket().getOutputStream());
				out.writeUTF("disconnect");
				client.getSocket().close();
			}
		}
		catch(IOException e) {System.out.println("No client");}
		
		
	}
}

//Thread to set up the clients by adding their details to clients
class clientSetup implements Runnable {
	//FIELDS
	Socket clientSock;
	
	ArrayList<ClientPair> clients;
	
	DataInputStream in;
	DataOutputStream out;
	
	//CONSTRUCTOR
	clientSetup(Socket cliSock, DataInputStream input, DataOutputStream output, 
				ArrayList<ClientPair> clientStuff) {
		clientSock = cliSock;
		clients = clientStuff;
		
		in = input;
		out = output;
	}
	//@Override
	public void run()
	{
		try{
			String name = in.readUTF();
			clients.add(new ClientPair(clientSock, name));
			out.writeUTF("Connected!");
		}
		catch(IOException e) {}
		//Printing clients connected
		System.out.println("Clients connected:");
		for(ClientPair player : clients) {
			System.out.println(player.getName());
		}
	}
}

class leaderControl implements Runnable {
	//FIELDS
	DataInputStream inFromLead = null;
	DataOutputStream outToLead = null;
	Thread thisSetup = null;
	
	//CONSTRUCTOR
	leaderControl(Thread cliSet, DataInputStream datStream, DataOutputStream outStream) {
		inFromLead = datStream;
		outToLead = outStream;
		thisSetup = cliSet;
	}
	
	public void run() {
		//Wait until the client setup is done before sending messages
		try{
			thisSetup.join();
		} catch(InterruptedException e) {}
		
		String messFromLead = "";
		//Tell leader they are indeed the leader
		try {
			outToLead.writeUTF("leader");
		}
		catch(IOException e) {}
		
		while(!messFromLead.equals("start")) {
			try {
				//Telling the lead to respond
				outToLead.writeUTF("respond");
				messFromLead = inFromLead.readUTF();
			}
			catch(IOException e) {}
		}
		return;
	}
}