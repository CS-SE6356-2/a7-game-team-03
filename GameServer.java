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
			cardGame.dealCards();
			
			//Get a PlayerQueue to run in order
			PlayerQueue playOrder = cardGame.sortPlayersInPlayOrder();
			
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