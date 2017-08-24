package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.util.Pair;

import java.util.List;
import java.util.Map;

public class Decks {

    List<Deck> decks;
    Map<Pair<DeckType, Integer>, Deck> byTypeId;
    Map<String, Deck> byName;

}
