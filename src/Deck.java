/*
	Programmer: Tyler Heald
	Date: 3/30/2019
	Description:
	The Deck class contains the cards needed for playing a game.
	When the Deck object is initialized with certain parameters from an external
	file, it creates cards with the proper values. The only thing the deck can do
	is return cards, remove cards, and shuffle itself.
	
	File cardList:
	The file that contains cards to make for the deck is in the format:
	#ncards
	value1 category1
	value2 category2
	...
	valuen categoryn
	
	METHODS:
	shuffle()
		Shuffles the deck using Fisher-Yates
	getCardAt(int i)
		Gets the card at a given index
	getNumOfCards()
		Gets the number of cards in the deck
	
	TODO:
	Add customization options to reading the cardList for easier processing.
	Example: C as the header so the program can generate cards with values in
	(values X category). Would make it easier to write a file for decks like the
	typical 52 card deck.
*/

import java.io.*;
import java.util.*;

public class Deck {
	//DATA FIELDS
	int numOfCards = 0;
	ArrayList<Card> cards = new ArrayList<Card>();
	
	/****	CONSTRUCTORS	****/
	public Deck(File cardList)
	{
		//Creating a scanner to read in the cardList
		Scanner input = new Scanner(System.in);
		try {
			input = new Scanner(cardList);
		}	catch(FileNotFoundException e) {
			//File not found, output error and exit
			System.out.println("cardList file not found!");
			System.exit(1);
		}
		
		//Initializing Strings to hold the value and category of cards
		String val;
		String category;
		
		//Read in all the cards for deckSize
		while(input.hasNextLine())
		{
			//Getting the value, then finishing the line with category
			val = input.next();
			//Checking if all combinations are requested
			while(val.equals("c")) {
				//Going to next line
				val = input.nextLine();
								
				//Setting up ArrayLists for combining elements
				ArrayList<String> vals = new ArrayList<String>();
				ArrayList<String> cats = new ArrayList<String>();
				
				//running until a "cvals" ends the block of values
				while(true) {
					val = input.nextLine();
					if(val.equals("cvals")) {break;}
					//System.out.println(val);
					vals.add(val);
				}
				//running until a "ccats" ends the block of categorys
				while(true) {
					category = input.nextLine();
					if(category.equals("ccats")) {break;}
					//System.out.println(category);
					cats.add(category);
				}
				
				//Got all the vals and cats, make cards of em
				for(String value : vals) {
					for(String categor : cats) {
						numOfCards ++;
						cards.add(new Card(value, categor));
					}
				}
				
				//Ending if there's nothing else in the file
				if(!input.hasNextLine()) {
					return;
				}
				//Getting next val so the order isn't messed up outside
				val = input.next();
				//If its another combination, will automaticall go back to the top
			}
			category = input.nextLine();
			
			//putting the card in cards in order of appearance
			cards.add(new Card(val, category));
			//Incrementing the number of cards
			numOfCards ++;
		}
	}
	/****	FUNCTIONS	****/
	void shuffle()
	{
		//Making a Random object to run Fisher-Yates shuffle
		Random rand = new Random();
		
		//For loop to run over each item in the array
		for(int i = numOfCards-1; i > 0; i --)
		{
			//Getting a random index 0 <= j < i
			int j = rand.nextInt(i+1);
			Card temp = cards.get(i);
			cards.set(i, cards.get(j));
			cards.set(j, temp);
		}
	}
	/****	GETTERS/SETTERS	****/
	//getCardAt returns the ith card, starting from 0
	public Card getCardAt(int i)
	{
		return cards.get(i);
	}
	//Self-explanatory
	public int getNumOfCards()
	{
		return numOfCards;
	}
}