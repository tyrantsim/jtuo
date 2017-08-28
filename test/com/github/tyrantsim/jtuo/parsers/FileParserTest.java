package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileParserTest {

    @BeforeClass
    public static void loadCards() throws Exception {
        //CardsParser.initialize();
    }

    @Test
    public void testCards() throws Exception {
        //LU4EBRhbHDhTeEhbZKhBLOh
        for (Integer i: Deck.hashToIds("LU4"))
            System.out.println(i);

    }


}