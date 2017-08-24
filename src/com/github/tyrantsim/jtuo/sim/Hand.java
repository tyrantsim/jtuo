package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.skills.Skill;

import java.util.ArrayList;
import java.util.Random;

class Hand {

    Deck deck;
    CardStatus commander;
    ArrayList<CardStatus> assaults = new ArrayList<>(15);
    ArrayList<CardStatus> structures = new ArrayList<>(15);
    int stasisFactionBitmap;
    int totalCardsDestroyed;

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

}
