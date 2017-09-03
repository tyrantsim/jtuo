package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class DeckTest {

    private Random random = new Random();

    @BeforeClass
    public static void loadCards() {
        CardsParser.initialize();
    }

    @Test
    public void testShuffle() {
        Card commander = CardsParser.getCardCopy(1000);

        Deck deck = new Deck();
        deck.setDeckStrategy(DeckStrategy.RANDOM);
        deck.setCommander(commander);
        deck.setCards(Arrays.asList(
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy()
        ));
        deck.setUpgradePoints(2);

        // Check shuffle randomness
        List<Card> oldCards = new ArrayList<>();
        List<Card> newCards = new ArrayList<>();
        deck.shuffle(random);
        oldCards.addAll(deck.getShuffledCards());
        deck.shuffle(random);
        newCards.addAll(deck.getShuffledCards());
        assertNotEquals(oldCards, newCards);

        // Check shuffled deck size and commander
        assertEquals(10, deck.getShuffledCards().size());
        assertTrue(deck.getShuffledCommander().getName().startsWith("Cyrus"));

        // Check upgrade points distribution
        deck.setCommander(CardsParser.cards.get(1000).clone());
        deck.setCards(Arrays.asList(
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy(),
                getRandomFirstLevelCardCopy()
        ));
        deck.setUpgradePoints(2);
        deck.shuffle(random);

        int upgradedBy = 0;
        for (Card card : deck.getShuffledCards()) {
            upgradedBy += card.getLevel() - 1;
        }
        upgradedBy += deck.getShuffledCommander().getLevel() - 1;
        assertEquals(2, upgradedBy);
    }

    private Card getRandomFirstLevelCardCopy() {
        Integer[] cards = CardsParser.cards.keySet().toArray(new Integer[]{});
        int randomIndex = random.nextInt(cards.length);
        Card randomCard = CardsParser.cards.get(cards[randomIndex]).clone();
        if (randomCard.getLevel() == 1) {
            return randomCard;
        } else {
            return getRandomFirstLevelCardCopy();
        }
    }

}
