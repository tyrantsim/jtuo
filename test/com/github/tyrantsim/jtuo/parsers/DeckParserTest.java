package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.decks.Deck;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DeckParserTest {

    @Test
    public void testParseDeck() {
        String deck = "Cyrus, Infantry-1, Infantry-2, Infantry";
        Map<String, Double> deckMap = DeckParser.expandDeckToList(deck);
        assertEquals(1, deckMap.keySet().size());
        assertEquals((Object) 1.0, deckMap.get("Cyrus, Infantry-1, Infantry-2, Infantry"));
    }

}
