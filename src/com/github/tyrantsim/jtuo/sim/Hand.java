package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.decks.Deck;

import java.util.ArrayList;

public class Hand {

    Deck deck;
    CardStatus commander;
    ArrayList<CardStatus> assaults = new ArrayList<>(15);
    ArrayList<CardStatus> structures = new ArrayList<>(15);
    int stasisFactionBitmap;
    int totalCardsDestroyed;

}
