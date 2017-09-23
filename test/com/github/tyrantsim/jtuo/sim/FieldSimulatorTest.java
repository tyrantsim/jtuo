package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import com.github.tyrantsim.jtuo.parsers.DeckParser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FieldSimulatorTest {

    private static final String yourDeckString = "Cyrus-1, Scarab";
    private static final String enemyDeckString = "Cyrus-1, Beam Gate, Dutiful Veteran, Infantry #3";
    private static final Random random = new Random();
    private static List<Deck> enemyDecks = new ArrayList<>();

    /*
    @BeforeClass
    public static void loadCards() throws Exception {
        CardsParser.initialize();
    }
    */

    @Test
    public void testFieldSim() throws Exception {

        CardsParser.initialize();

        Deck yourDeck;

        try {
            yourDeck = Decks.findDeck(yourDeckString).clone();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error: Deck " + yourDeckString + ": " + e.getMessage());
            return;
        }

        if (yourDeck == null) {
            System.err.println("Error: Invalid attack deck name/hash " + yourDeckString);
            return;
        }

        Map<String, Double> deckListParsed = DeckParser.parseDeckList(enemyDeckString);

        for (String deckStr: deckListParsed.keySet()) {

            // Construct the enemy decks
            Deck deck;
            try {
                deck = Decks.findDeck(deckStr).clone();
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.err.println("Error: Deck " + deckStr + ": " + e.getMessage());
                return;
            }

            if (deck == null) {
                System.err.println("Error: Invalid attack deck name/hash " + deckStr);
                return;
            }

            enemyDecks.add(deck);

        }

        Hand yourHand = new Hand(yourDeck);
        Hand enemyHand = new Hand(enemyDecks.get(0));
        yourHand.reset(random);
        enemyHand.reset(random);

        int[] passiveBGEs = new int[PassiveBGE.values().length];
        //passiveBGEs[PassiveBGE.DEVOUR.ordinal()] = 1;

        Field fd = new Field(
                random,
                null,
                yourHand, enemyHand,
                GameMode.FIGHT,
                OptimizationMode.WINRATE,
                //passiveBGEs, passiveBGEs,
                new ArrayList<>(), new ArrayList<>()
        );
        FieldSimulator.play(fd);
        Results result = FieldSimulator.play(fd);
        System.out.println(result);

        System.out.println("done");
    }

    @Test
    public void testCardSkills() throws Exception {
        CardsParser.initialize();
        Card card = Cards.getCardByName("Alpha Replicant").clone();
        System.out.println(Cards.cardDescription(card));
        System.out.println(card.getCategory());

    }

    @Test
    public void testDeck() throws Exception {
        CardsParser.initialize();
        Deck yourDeck = null;

        try {
            yourDeck = Decks.findDeck(yourDeckString).clone();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error: Deck " + yourDeckString + ": " + e.getMessage());
            return;
        }

        if (yourDeck == null) {
            System.err.println("Error: Invalid attack deck name/hash " + yourDeckString);
            return;
        }

        Hand yourHand = new Hand(yourDeck);
        yourHand.reset(new Random());

        System.out.println(yourHand.getStructures().get(0));


    }

}

