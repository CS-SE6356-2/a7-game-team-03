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
			System.out.println("Game start!");
			//Create the CardGame object with player info
			CardGame cardGame = new CardGame(numOfClients, clients, new File("cardlist"));
			//Deal the cards
			//NOTE: the CardGame will itself notify the client of the cards they have
				//PERHAPS NOT? KEEP SERVER AND LOGIC SEPARATE MAYBE
			cardGame.dealCards();
			
			//Get a PlayerQueue to run in order
			PlayerQueue playOrder = cardGame.sortPlayersInPlayOrder();
			
			//while(true) to just run until someone wins
			while(true) {
				//CHECK FOR ACTION CARD ON DISCARD
				if(cardGame.lastPlayed().getVal().equals("skip")) {
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = client.getSock().getOutputStream();
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " skipped");
					}
					//Skip
					playOrder.nextPlayer();
					continue;
				}
				else if(cardGame.lastPlayed().getVal().equals("draw2")) {
					//Draw 2 cards, then skip
					for(int i = 0; i < 2; i++) {
						String drawnCard = cardGame.drawCard(playOrder.getPlayer());
						//Notify of card drawn
						currentOut.writeUTF("draw " + drawnCard);
					}
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = client.getSock().getOutputStream();
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + "drew2 skipped");
					}
					//Skip
					playOrder.nextPlayer();
					continue;
				}
				else if(cardGame.lastPlayed().getVal().equals("draw4")) {
					//Draw 4, then skip
					for(int i = 0; i < 4; i++) {
						String drawnCard = cardGame.drawCard(playOrder.getPlayer());
						//Notify of card drawn
						currentOut.writeUTF("draw " + drawnCard);
					}
					//Notify all of skippage
					for(ClientPair client : clients) {
						DataOutputStream outToYou = client.getSock().getOutputStream();
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + "drew4 skipped");
					}
					//Skip
					playOrder.nextPlayer();
					continue;
				}
				//String to store the player's move
				String move = "";
				//Notify the player it is their turn
				Socket currentPlayer = playOrder.getPlayer().getSock();
				DataInputStream currentIn = currentPlayer.getInputStream();
				DataOutputStream currentOut = currentPlayer.getOutputStream();
				
				currentOut.writeUTF("turn");
				//Tell everyone who's turn it is
				for(ClientPair client : clients) {
					DataOutputStream outToYou = client.getSock().getOutputStream();
					outToYou.writeUTF("turn " + playOrder.getPlayer().getName());
				}
				
				//Check if the player has a legal move
				if(cardGame.hasLegalPlay(playOrder.getPlayer())) {
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
					cardGame.makeMove(playOrder.getPlayer(), move);
					
				}
				else {
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
					DataOutputStream outToYou = client.getSock().getOutputStream();
					if(move.equals("drew")) {
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " drew");
					}
					else {
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " used " + move);
					}
				}
				//Check if the player won
					//yes, notify everyone, end game
					//no, keep going
				if(cardGame.won(playOrder.getPlayer())) {
					//yes, notify everyone and end game
					for(ClientPair client : clients) {
						DataOutputStream outToYou = client.getSock().getOutputStream();
						outToYou.writeUTF("player " + playOrder.getPlayer().getName() + " won");
					}
				}
				//no, keep going
				
				//Tell everyone who has how many cards
				for(
				
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