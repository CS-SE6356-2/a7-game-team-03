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