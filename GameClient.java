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
		Thread listen = new Thread(new ListenThread(out, in, gui));
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
	
	ListenThread(DataOutputStream 0, DataInputStream i, ClientGUI g) {
		out = o;
		in = i;
		gui = g;
	}
	public void run() {
		//Listen for messages from the server
		String message = "";
		try{
			message = in.readUTF();
		}
		//Splitting the message into its parts
		String[] messParts = message.split(" ");
		
		//Checking what the message isLeader
		if(messParts[0].equals()) {
			
		}
	}
}