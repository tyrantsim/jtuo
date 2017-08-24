package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;

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
    Deque<Card> shuffledCards;

    // card id -> card order
    HashMap<Integer, List<Integer>> order;
    ArrayList<Variable> variableForts;
    ArrayList<Variable> variableCards;

    private class Variable {
        int amount;
        int replicates;
        ArrayList<Card> cardList;
    }

    int deckSize;
    int missionReq;
    int level;

    String deckString;
    Set<Integer> vipCards;
    ArrayList<Integer> givenHand;
    ArrayList<Card> fortressCards;
    ArrayList<SkillSpec> effects;

    public void shuffle(Random re) {
        shuffledCommander = commander;
        shuffledForts.clear();
        shuffledForts.addAll(fortressCards);
        shuffledCards.addAll(cards);
        if(!variableForts.isEmpty()) {
            if(strategy != DeckStrategy.RANDOM) {
                throw new RuntimeException("Support only random strategy for raid/quest deck.");
            }
            for(Variable cardPool : variableForts) {
                int amount = cardPool.amount;
                int replicates = cardPool.replicates;
                ArrayList<Card> cardList = cardPool.cardList;
                partialShuffle(cardList, amount, re);
                for(int rep = 0; rep < replicates; ++rep) {
                    shuffledForts.add(cardList.get(amount));
                }
            }
        }
        if(!variableForts.isEmpty()) {
            if(strategy == DeckStrategy.RANDOM) {
                throw new RuntimeException("Support only random strategy for raid/quest deck");
            }
            for(Variable cardPool : variableCards) {
                int amount = cardPool.amount;
                int replicates = cardPool.replicates;
                ArrayList<Card> cardList = cardPool.cardList;
                partialShuffle(cardList, amount, re);
                for(int rep = 0; rep < replicates; ++rep) {
                    shuffledCards.add(cardList.get(amount));
                }
            }
        }
        if(upgradePoints > 0) {
            int remainingUpgradePoints = upgradePoints;
            ArrayList<Pair<Deque<Card>, Integer>> upCards;
            Deque<Card> commanderStorage = new LinkedList<>();
            commanderStorage.add(shuffledCommander);
            // TODO: finish this
        }
        if(strategy == DeckStrategy.ORDERED) {
            // TODO: implement this
        }
        if(strategy != DeckStrategy.EXACT_ORDERED) {
            // TODO: implement this
        }
    }

    void partialShuffle(List<Card> cardList, int middle, Random re) {
        // TODO: implement this
    }

    // Getters & Setters
    public Deque<Card> getShuffledCards() {
        return shuffledCards;
    }

    public Card getShuffledCommander() {
        return shuffledCommander;
    }

}
