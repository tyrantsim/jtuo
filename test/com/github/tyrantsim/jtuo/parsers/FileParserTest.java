package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FileParserTest {

    @BeforeClass
    public static void loadCards() throws Exception {
        //CardsParser.initialize();
    }

    @Test
    public void testCards() throws Exception {
        List<Integer> ids = Deck.hashToIds("LU4EBRhbHDhTeEhbZKhBLOh");
        assertEquals((Integer) 25227, ids.get(0));
        assertEquals((Integer) 50212, ids.get(1));
        assertEquals((Integer) 36091, ids.get(2));
        assertEquals((Integer) 37843, ids.get(3));
        assertEquals((Integer) 43835, ids.get(4));
        assertEquals((Integer) 47457, ids.get(5));
    }


}