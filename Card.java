/*
	Programmer: Tyler Heald
	Date: 3/30/2019
	Description:
	The Card class is meant only to hold the value and category of a Card(number and suite).
	It contains that data, ways to access it, and a method to print its data
	
	METHODS:
	printCard()
		Prints the cards data in the format "value category"
*/

public class Card{
	//DATA FIELDS
	String value;
	String category;
	
	/****	CONSTRUCTORS	****/
	public Card(String v, String c)
	{
		value = v;
		category = c;
	}
	
	public Card(String card) 
	{
		int delimiter = card.indexOf(" ");
		value = card.substring(0, delimiter);
		category = card.substring(delimiter, card.length());
	}

	/****	FUNCTIONS	****/
	//Method to print card stats
	public void printCard()
	{
		System.out.println(value + " " + category);
	}
	/****	GETTERS/SETTERS	****/
	void setVal(String v)
	{
		value = v;
	}
	String getVal()
	{
		return value;
	}
	void setCategory(String c)
	{
		category = c;
	}
	String getCategory()
	{
		return category;
	}

	/* This function assumes that the left hand side is the deck card, while
	 * the right hand side comes from the player.
	 */
	public boolean matches(Card c) {
		if (c == null) {
			return false;
		} else if (c.getVal().equals("Wild")) {
			return true;
		} else {
			return this.value.equals(c.value)
				|| this.category.equals(c.category);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null) {
			return false;
		} else if (!(o instanceof Card)) {
			return false;
		} else {
			Card c = (Card) o;
			return this.value.equals(c.value)
				&& this.category.equals(c.category);
		}
	}
}
