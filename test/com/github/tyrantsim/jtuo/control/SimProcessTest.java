package com.github.tyrantsim.jtuo.control;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;

public class SimProcessTest {

    @Test
    public void testSimple() {
        CardsParser.initialize();

        Deck yourDeck = new Deck();
        yourDeck.setCommander(CardsParser.getCardCopy(1000));
        yourDeck.setCards(Arrays.asList(
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1)
        ));

        Deck enemyDeck = new Deck();
        enemyDeck.setCommander(CardsParser.getCardCopy(1000));
        enemyDeck.setCards(Arrays.asList(
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1),
                CardsParser.getCardCopy(1)
        )); // Hope we can win that way!

        SimProcess p = new SimProcess(1, yourDeck, Collections.singletonList(enemyDeck), Collections.singletonList(1.0));
        EvaluatedResults results = p.evaluate(100);
        assertEquals(1.0D, results.getWins());
        assertEquals(0.0D, results.getLosses());
        assertEquals(0.0D, results.getDraws());
    }

}
