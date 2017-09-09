package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.decks.Deck;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeckParserTest {

    @Test
    public void testParseDeck() {
        String deck = "Cyrus, Infantry-1, Infantry-2, Infantry";
        Map<String, Double> deckMap = DeckParser.expandDeckToList(deck);
        assertEquals(1, deckMap.keySet().size());
        assertEquals((Object) 1.0, deckMap.get("Cyrus, Infantry-1, Infantry-2, Infantry"));
    }

    @Test
    public void testGetCommanderSpec() throws Exception {
        CardsParser.initialize();

        int halcyonId = DeckParser.parseCardSpec("Halcyon-1").getCardId();
        assertTrue(CardsParser.getCardCopy(halcyonId).getType() == CardType.COMMANDER);

        int barracusId = DeckParser.parseCardSpec("Barracus-1").getCardId();
        assertTrue(CardsParser.getCardCopy(barracusId).getType() == CardType.COMMANDER);

        int barracusUpgradedId = DeckParser.parseCardSpec("Barracus").getCardId();
        assertEquals(barracusUpgradedId, CardsParser.getCardCopy(barracusId).getTopLevelCard().getId());
        assertTrue(CardsParser.getCardCopy(barracusUpgradedId).getType() == CardType.COMMANDER);
    }

}
