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

import java.util.*;
import java.io.*;
import java.net.*;

public class GameClient {
	Socket hostSock = null;
	DataInputStream in = null;
	DataOutputStream out = null;
	
	ClientGUI gui;
	
	String name = null;
	
	String serverMessage = "";
	
	boolean leader = false;
	
	Hand cards = new Hand();
	
	//CONSTRUCTOR
	GameClient(ClientGUI g) {
		gui = g;
	}
	
	GameClient() {
		
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
			while(!serverMessage.equals("leader") && !serverMessage.equals("player")) {
				serverMessage = in.readUTF();
				System.out.println(serverMessage);
			}
			//Checking specification
			if(serverMessage.equals("leader")) {
				leader = true;
			}
			return true;
		}
		catch(UnknownHostException e) {System.out.println("no host!"); return false;}
		catch(IOException e) { return false;}
	}
	
	//Disconnects from host when GUI calls for it (not currently used)
	boolean disconnectFromHost() {
		if(hostSock != null) {
			try{
				hostSock.close();
			}
			catch(IOException e) {}
			return true;
		}
		return false;
	}
	
	//A method for the leader client to tell the server to start the game
	boolean sendStart() {
		try{
			out.writeUTF("start");
		}
		catch(IOException e) {}
		return true;
	}
	
	//A method to start the client listening constantly for messages from the server
	void listen() {
		Thread listen = new Thread(new ListenThread(out, in, gui, cards));
		listen.start();
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
	DataOutputStream out;
	DataInputStream in;
	
	ClientGUI gui;
	
	Hand cards;
	
	Scanner input = new Scanner(System.in);
	
	ListenThread(DataOutputStream o, DataInputStream i, ClientGUI g, Hand c) {
		out = o;
		in = i;
		gui = g;
		cards = c;
	}
	public void run() {
		//DEBUG
		System.out.println("Hey! Listen!");
		//Listen for messages from the server
		String message = "";
		while(!message.equals("disconnect")) {
			try{
				message = in.readUTF();
			}
			catch(IOException e) {}
			//DEBUG
			System.out.println(message);
			//Splitting the message into its parts
			String[] messParts = message.split(" ");
			
			//Checking what the message header is
			//Draw a card
			if(messParts[0].equals("draw")) {
				//Add the card to the hand
				Card newCard = new Card(messParts[1], messParts[2]);
				//DEBUG
				System.out.println("added card");
				cards.addCard(newCard);
				//Send the new card to the gui
			}
			else if(messParts[0].equals("turn")) {
				//Check if there its someone elses turn
				if(messParts.length > 1) {
					//is someone elses move
				}
				else {
					//Will be receiving unneeded turn [name] message, get it off socket
					try{
						message = in.readUTF();
					}
					catch(IOException e) {}
					//check for "play".
						//yes, play card
						//no, listen for cards drawn
					try{
						message = in.readUTF();
						//DEBUG
						System.out.println("read turn type:");
						System.out.println(message);
					}
					catch(IOException e) {}
					if(message.equals("play")) {
						//this players move
						//Tell the GUI to let a card be played.
						//When one is, GUI will call a method to
						//send the card to the server through client
						//gui.getMove();
						//DEBUG
						System.out.println("printing cards:");
						cards.printCards();
						System.out.println("Enter the card you want to play:");
						String cardPlay = input.nextLine();
						//Send to server
						try{
							out.writeUTF(cardPlay);
						}
						catch(IOException e) {}
						//Checking for illegal move
						try{
							message = in.readUTF();
						}
						catch(IOException e) {}
						while(message.equals("illegal move")) {
							//gui.getMove();
							cards.printCards();
							System.out.println("Enter the card you want to play:");
							cardPlay = input.nextLine();
							try {
								out.writeUTF(cardPlay);
								message = in.readUTF();
							}
							catch(IOException e) {}
						}
						//DEBUG
						System.out.println("made legal move");
						//Move was legal, server will send back what the card was
						//client will remove from hand
						String cardPlayed[] = cardPlay.split(" ");
						LinkedList<Card> temp = new LinkedList<Card>();
						temp.add(new Card(cardPlayed[0], cardPlayed[1]));
						cards.removeCards(temp);
						//DEBUG
						System.out.println("Removed " + cardPlay);
					}
					else {
						//Listen for card drawn
						System.out.println("drawing a card");
						String drawnCard = message;
						//split the string to get the card
						//will be in [1], [2]
						String[] cardParts = drawnCard.split(" ");
						cards.addCard(new Card(cardParts[1], cardParts[2]));
					}
				}
			}
		}
	}
}