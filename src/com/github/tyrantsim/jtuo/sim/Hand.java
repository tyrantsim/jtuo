package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.skills.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Hand implements Cloneable {

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

    public void updateStasisFactions(int playedFactionMask) {
        this.stasisFactionBitmap |= playedFactionMask;
    }

    public void incTotalCardsDestroyed() {
        totalCardsDestroyed++;
    }

    // Getters & Setters
    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setCommander(CardStatus commander) { this.commander = commander; }

    public CardStatus getCommander() {
        return commander;
    }

    public void setAssaults(List<CardStatus> assaults) { this.assaults = assaults; }

    List<CardStatus> getAssaults() {
        return assaults;
    }

    public void setStructures(List<CardStatus> structures) { this.structures = structures; }

    List<CardStatus> getStructures() {
        return structures;
    }

    void setStasisFactionBitmap(int stasisFactionBitmap) {
        this.stasisFactionBitmap = stasisFactionBitmap;
    }

    int getStasisFactionBitmap() {
        return stasisFactionBitmap;
    }

    public void setTotalCardsDestroyed(int totalCardsDestroyed) { this.totalCardsDestroyed = totalCardsDestroyed; }

    int getTotalCardsDestroyed() {
        return totalCardsDestroyed;
    }

    public Hand clone() {
        try {
            Hand copy = (Hand) super.clone();
            copy.setDeck(deck.clone());
            copy.setCommander(commander.clone());
            copy.setAssaults(new ArrayList<>(assaults));
            copy.setStructures(new ArrayList<>(structures));
            copy.setStasisFactionBitmap(stasisFactionBitmap);
            copy.setTotalCardsDestroyed(totalCardsDestroyed);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}

