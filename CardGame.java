import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.net.Socket;

public class CardGame
{
	////Member Variables////
	Player[] players;			//Holds the data for each player
	Deck cardDeck;				//Holds the information for each card
	Cardpile[] piles;			
	
	////Constructor////
	public CardGame(int numOfPlayers, ArrayList<ClientPair> clients,
		File cardList)
	{
		players = new Player[numOfPlayers];		//Create a list of Players
		cardDeck = new Deck(cardList);			//Create the deck of cards. The Card Game class thus has a reference to all cards
		piles = new Cardpile[2];				//Create the list of piles, will give amount that fits a specific card game
		
		//Create Card Piles
		piles[0] = new Cardpile("Draw");
		piles[1] = new Cardpile("Used");
		
		//Create Players
		createPlayers(clients);
	}
	
	/**
	 * 
	 */
	void dealCards()
	{
		int currentCard = 0;
		LinkedList<Card> temp = new LinkedList<Card>();
		for(Player player: players)
		{
			for(;temp.size() < 7; currentCard++)			//Get a list of cards that will be of even size to a player. UNO starts off with players having 7 cards
				temp.add(cardDeck.cards.get(currentCard));		//add card reference to list
				//Give players their cards
			player.addCards(temp);
			temp.clear();									//Clear the list so we can give the next player their cards
		}
		
		//Give rest of cards to draw pile
		for(;currentCard < cardDeck.numOfCards; currentCard++)
			temp.add(cardDeck.cards.get(currentCard));
		piles[0].addCardsOnTop(temp);
		temp.clear();
		
		//Put the first card on top of the draw deck on to the used pile
		piles[1].addCardsOnTop(piles[0].takeCards(1));
	}
	//Method to draw a card for a given player
	String drawCard(Player focusPlayer) {
		//Draw the card
		Card drawnCard = piles[0].takeCards(1);
		//Add to player's hand
		focusPlayer.addCard(drawnCard);
		return drawnCard.toString();
	}
	public void shuffleCards() {cardDeck.shuffle();}
	private void createPlayers(ArrayList<ClientPair> clients)
	{
		for(int i = 0; i < players.length; i++)
		{
			players[i] = new Player(clients.get(i).getName(),"Solo", clients.get(i).getSocket());
		}
	}
	
	/**
	 * Sorts the list of players initially in a game by finding the dealer, adding them and the other players into a circular linked list called playerQueue
	 * @author Chris
	 * @return playerQueue
	 */
	public PlayerQueue sortPlayersInPlayOrder()
	{
		//CLIENTSOCKS AND CLIENTLABELS are automatically sorted within the playerQueue as they are part of the Player object
		
		int dealerNum;	//Track the index of the dealer
		 //Index through array until dealer is found, if not then stop at end of list
		for(dealerNum = 0;dealerNum < players.length && !players[dealerNum].getRole().equals("Dealer"); dealerNum++);
		
		//Move number to next in list as dealer doesn't usually go first
		dealerNum = (dealerNum+1)%players.length;
		//Create the playerQueue
		PlayerQueue playOrder = new PlayerQueue();
		
		for(int i = 0; i < players.length; i++)							//For each player
			playOrder.enqueue(players[(dealerNum+i)%players.length]);	//Starting at the dealer, add them to the queue
		
		return playOrder;	//Return  the queue
	}
	/**
	 * Assigns the given player as the new dealer.
	 * @author Chris
	 * @param newDealer
	 * @return True if a new dealer has been assigned | False if not
	 */
	public boolean assignDealear(String newDealer)
	{
		for(Player p: players)
			if(p.getName().equals(newDealer))
			{
				p.assignRole("Dealer");
				return true;
			}
		return false;
	}

	public boolean isLegalMove(Player focusPlayer, String move) {
		String[] parts = move.split(" ");
		Card prev = this.lastPlayed();
		switch (parts[0]) {
		case "draw":
			for (Card c : focusPlayer.getActiveCards()) {
				if (c.matches(prev)) {
					return false;
				}
			}
			return true;
		case "play":
			Card play = new Card(parts[1], parts[2]);
			return (prev.matches(play) && focusPlayer.getActiveCards().contains(play));
		default:
			return false;
		}
	}
	
	// checks if the player in question has a card that can be played on
	// this particular turn
	boolean hasLegalPlay(Player focusPlayer) {
		for (Card c : focusPlayer.getActiveCards()) {
			if (this.lastPlayed().matches(c)) {
				return true;
			}
		}
		return false;
	}
	
	public void makeMove(Player focusPlayer, String move) {
		//Makes the move. Either will be playing a card, or drawing a card
		if(move.equals("draw")) {
			//Drawing the top card off the draw deck
			focusPlayer.addCards(piles[0].takeCards(1));
			//check if the deck was emptied
			if(piles[0].isEmpty()) {
				//Reshuffle the deck, taking into account all rules
				unoReshuffle(piles[0], piles[1]);
			}
			
		}
	}
	
	//Method to reshuffle the used cards into the draw pile, and
	//put one card into the used. Follows all rules for that process
	void unoReshuffle(Cardpile draw, Cardpile used) {
		//shuffle used, and set draw to it
		used.shuffle();
		draw = used;
		//set used to a new Cardpile
		used = new Cardpile("Used");
		//add top card to it
		used.addCardsOnTop(draw.takeCards(1));
		
		//Check for which card was put into discard
		//Special consideration for wild draw four
		while(used.checkTop().getCategory().equals("Wild") && used.checkTop().getVal().equals("4")) {
			draw.addCardsOnTop(used.takeCards(1));
			draw.shuffle();
			used.addCardsOnTop(draw.takeCards(1));
		}
	}
	
	/**
	 * Checks to see if a player has met the winning conditions
	 * @author Chris
	 * @return
	 */
	public boolean won(Player focusPlayer)
	{
		//Player wins if they have 0 cards
		if(focusPlayer.getNumOfCards() == 0)
			return true;
		return false;
	}
	
	//Returns the last played card (top of the used pile)
	Card lastPlayed() {
		return piles[1].checkTop();
	}

}