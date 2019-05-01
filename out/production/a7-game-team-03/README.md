Team: 
Tyler
Aneesh
Matthew

MAKING A LIST OF CARDS
There are two ways that cards can be generated by a cardList file. The main way is by reading
in a line of the form: value category
The program will simply take the value and category and make a card with those attributes.
The other way is to create a combinatorial block like this:
c
....
....
cvals
....
....
ccats

the initial c signals to the program that it is entering a combinatorial block, and every line forward will
be a single value. When the string "cvals" is read, the program starts reading in categories. "ccats" ends the
block, and the program will create cards in every combination of value and category.
There can be multiple combinatorial blocks in the file, and they can be followed by single card pairs as well.

NOTE: BELOW INSTRUCTIONS ARE FROM UNCHANGED FRAMEWORK

Compilation Instructions:
Make sure you have a Java Development Kit (JDK) installed
Find the JDK (should be in c:\Program Files\Java
Enther this line in command line set path=%path%;C:\Program Files\Java\jdk-9.0.1\bin
Change Directory to the location of this project, into the src folder

To compile and run the project: 
javac ClientLauncher.java ClientController.java ClientGUI.java Card.java CardGame.java Cardpile.java Deck.java Hand.java Player.java PlayerQueue.java
java ClientLauncher
 
To compile and run CardpileTest 
javac CardpileTest.java Cardpile.java Card.java 
java CardpileTest
 
To compile and run DeckTest 
javac DeckTest.java Deck.java Card.java 
java DeckTest
 
To compile and run PlayerQueueTest
Javac PlayerQueueTest.java Player.java Hand.java Card.java
Java PlayerQueueTest
 
To compile and run CardGameTest
Javac CardGameTest.java CardGame.java Deck.java Cardpile.java PlayerQueue.java Player.java Hand.java Card.java
Java CardGameTest
 
To compile and run PlayerHandTest
Javac PlayerHandTest.java Player.java Hand.java Card.java Deck.java
Java PlayerQueueTest