package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.decks.Deck;
import org.junit.Test;

import java.util.Map;

public class DeckParserTest {

    @Test
    public void testParseDeck() {
        String deck = "Cyrus, Infantry-1, Infantry-2, Infantry";
        Map<String, Double> deckMap = DeckParser.expandDeckToList(deck);
        deckMap.keySet().forEach(key -> {
            Object value = deckMap.get(key);
            System.out.println("Key: " + key + ", value: " + value);
        });
    }

}
