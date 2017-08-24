package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.*;

public class Deck {

    Cards allCards;
    DeckType deckType = DeckType.DECK;
    int id;
    String name;
    int upgradePoints;
    int upgradeOpportunities;
    DeckStrategy strategy = DeckStrategy.RANDOM;

    Card commander;
    Card alphaDominion;
    int commanderMaxLevel;
    ArrayList<Card> cards;
    HashMap<Integer, Integer> cardMarks; // <positions of card, prefix mark>: -1 indicating the commander. E.g, used as a mark to be kept in attacking deck when optimizing.

    Card shuffledCommander;
    Deque<Card> shuffledForts;
    Deque<Card> suffledCards;

    // card id -> card order
    HashMap<Integer, List<Integer>> order;
    // Skipped: variable_forts, variable_cards
    int deckSize;
    int missionReq;
    int level;

    String deckString;
    Set<Integer> vipCards;
    ArrayList<Integer> givenHand;
    ArrayList<Card> fortressCards;
    ArrayList<SkillSpec> effects;

}
