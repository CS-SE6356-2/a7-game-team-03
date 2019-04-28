import java.util.*;
import java.io.*;
import java.net.*;

public class GameClient {
	Socket hostSock = null;
	DataInputStream in = null;
	DataOutputStream out = null;
	
	String name = null;
	
	String serverMessage = "";
	
	boolean leader = false;
	
	//CONSTRUCTOR
	GameClient() {
		
	}
	
	/*public static void main(String[] args) {
		String hostName = args[0];
		int port = Integer.parseInt(args[1]);
		Socket hostSock = null;
		
		DataInputStream in = null;
		DataOutputStream out = null;
		
		Scanner input = new Scanner(System.in);
		
		String myMessage = "";
		String serverMessage = "";
		
		try {
			//Connecting to the host
			hostSock = new Socket(hostName, port);
			out = new DataOutputStream(hostSock.getOutputStream());
			in = new DataInputStream(hostSock.getInputStream());
			
			//Prompting user for name
			System.out.println("Enter your name:");
			//Sent to the server as a client label
			out.writeUTF(input.nextLine());
			//Read in the connection message from host
			serverMessage = in.readUTF();
			System.out.println(serverMessage);
			//Resetting serverMessage
			serverMessage = "";
		}
		catch(UnknownHostException e) {System.out.println("no host!");}
		catch(IOException e) { }
		
		while(!serverMessage.equals("disconnect")) {
			//Reading message from server
			try{
				serverMessage = in.readUTF();
			}
			catch(IOException e) {}
			
			//Disconnecting
			if(serverMessage.equals("disconnect")) {
				try{
					hostSock.close();
				} catch(IOException e) {}
				System.out.println("Disconnected from server.");
				System.exit(0);
			}
			
			//Printing the message
			if(!serverMessage.equals("") && !serverMessage.equals("respond")) {
				System.out.println("Host said: " + serverMessage);
			}
			
			//Server requesting input
			if(serverMessage.equals("respond")) {
				myMessage = input.nextLine();
				try{
					//myMessage = input.nextLine();
					out.writeUTF(myMessage);
				}
				catch(IOException e) {}
			}
		}
		
		//Closing the socket
		System.out.println("Disconnecting from server...");
		try{
			hostSock.close();
		}
		catch(IOException e) {}
		System.out.println("Disconnected");
	}*/
	
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