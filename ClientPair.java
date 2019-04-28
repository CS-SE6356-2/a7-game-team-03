/*	Programmer: Tyler Heald
	Date: 4/28/2019
	Description:
		The ClientPair class is just a data storage class that
		makes it easier to store a clients name together with the
		socket they are connected on.
	Methods:
		get/setName
		get/setSocket
*/
import java.net.*;

//HELPER DATA CLASS FOR STORING CLIENT INFO
//Pair class to store socket and string for client connections
public class ClientPair {
	String name;
	Socket clientSock;		
	//CONSTRUCTORS
	ClientPair() {
		name = "";
		clientSock = null;
	}
	ClientPair(String n, Socket cs) {
		name = n;
		clientSock = cs;
	}
	ClientPair(Socket cs, String n) {
		name = n;
		clientSock = cs;
	}
	
	//GETTERS AND SETTERS
	public void setName(String n) {
		name = n;
	}
	public void setSocket(Socket cs) {
		clientSock = cs;
	}
	public String getName() {
		return name;
	}
	public Socket getSocket() {
		return clientSock;
	}
}