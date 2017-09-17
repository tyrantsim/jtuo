package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SimulationDataTest {

    @Test
    public void simpleAlwaysWinTest() {
        CardsParser.initialize();
        FieldSimulator.turnLimit = 50;
        Random random = new Random();

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

        SimulationData sim = new SimulationData(random);
        sim.setDecks(yourDeck, Collections.singletonList(enemyDeck));

        ArrayList<Results> results = sim.evaluate(new TyrantOptimize());
        assertEquals(1, results.size());

        Results result = results.get(0);
        assertEquals(1, result.getWins());
        assertEquals(0, result.getLosses());
        assertEquals(0, result.getDraws());
        assertEquals(100, result.getPoints());
    }

}
