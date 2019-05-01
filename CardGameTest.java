import java.io.File;
import java.net.Socket;
import java.util.ArrayList;

public class CardGameTest {

	public static void main(String[] args) 
	{
		ArrayList<ClientPair> players = new ArrayList<ClientPair>();
		for(int i = 0; i < 3; i++)
		{
			players.add(new ClientPair("Player " + i, new Socket()));
			System.out.println("Added "+players.get(i).getName());
		}
		CardGame game = new CardGame(players.size(),players,new File("cardlist"));
		//createPlayers is tested within the constructor
		
		//Assign player 2 as dealer
		game.assignDealear("Player 2");
		//Shuffle the cards
		game.shuffleCards();
		//Deal the cards
		//Runs into an exception because the players dont have actual sockets to send
		//data to
		game.dealCards();
		
		//Get the list of cards from each player and see they have been sorted as the dealer goes last
		for(Player p: game.sortPlayersInPlayOrder())
			System.out.println(p.getName()+" has these cards: "+p.getCardListForUTF());
		
	}

}
