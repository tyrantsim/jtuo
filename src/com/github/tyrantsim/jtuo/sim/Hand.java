package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.skills.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Hand {

    private Deck deck;
    private CardStatus commander = new CardStatus();
    private List<CardStatus> assaults = new ArrayList<>(15);
    private List<CardStatus> structures = new ArrayList<>(15);
    private int stasisFactionBitmap;
    private int totalCardsDestroyed;

    public Hand(Deck deck) {
        this.deck = deck;
    }

    void reset(Random random) {
        assaults.clear();
        structures.clear();
        deck.shuffle(random);
        commander.set(deck.getShuffledCommander());
        totalCardsDestroyed = 0;
        if (commander.skill(Skill.STASIS) != 0) {
            stasisFactionBitmap |= (1 << commander.getCard().getFaction().ordinal());
        }
    }

    // Getters & Setters
    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public Deck getDeck() {
        return deck;
    }

    public CardStatus getCommander() {
        return commander;
    }

    List<CardStatus> getAssaults() {
        return assaults;
    }

    List<CardStatus> getStructures() {
        return structures;
    }

}
