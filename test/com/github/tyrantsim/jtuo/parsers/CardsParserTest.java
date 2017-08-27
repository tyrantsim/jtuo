package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.Card;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CardsParserTest {

    @Test
    public void testLoadCard() {
        CardsParser.initialize();
        Card infantry = CardsParser.cards.get(1);

        assertEquals(1, infantry.getId());
        assertEquals("Infantry-1", infantry.getName());
        assertEquals(1, infantry.getLevel());

        assertEquals(2, infantry.upgraded().getId());
        assertEquals("Infantry-2", infantry.upgraded().getName());
        assertEquals(2, infantry.upgraded().getLevel());

        assertEquals(3, infantry.getTopLevelCard().getId());
        assertEquals("Infantry", infantry.getTopLevelCard().getName());
        assertEquals(3, infantry.getTopLevelCard().getLevel());
    }

    @Test
    public void testCardUsedFor() {
        CardsParser.initialize();
        Card infantry = CardsParser.cards.get(1);

        assertEquals(1, infantry.getLevel());
        assertEquals(1, infantry.getUsedForCards().size());
        assertEquals(2, infantry.upgraded().getLevel());
    }

}
