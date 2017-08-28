package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Decks {

    public static List<Deck> decks = new ArrayList<Deck>();
    public static Map<String, Deck> byName = new HashMap<String, Deck>();
    Map<Pair<DeckType, Integer>, Deck> byTypeId;

    public static Deck findDeckByName(String deckName) {
        return byName.getOrDefault(deckName, null);
    }

    public static void addDeck(Deck deck, String deckName) {
        byName.put(deckName, deck);
        byName.put(Cards.simplifyName(deckName), deck);
    }

    public static String getDeckTypeAsString(DeckType deckType) {
        switch (deckType) {
            case CUSTOM_DECK: return "Custom Deck";
            case RAID: return "Raid";
            case MISSION: return "Mission";
            case CAMPAIGN: return "Campaign";
            case DECK: default: return "Deck";
        }
    }

}
