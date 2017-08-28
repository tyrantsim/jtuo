package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;

public class Deck implements Cloneable {

    private Cards allCards;
    private DeckType deckType = DeckType.DECK;
    private int id;
    private String name;
    private int upgradePoints;
    private int upgradeOpportunities;
    private DeckStrategy strategy = DeckStrategy.RANDOM;

    private Card commander;
    private Card alphaDominion;
    private int commanderMaxLevel;
    private List<Card> cards = new ArrayList<>();
    // <positions of card, prefix mark>: -1 indicating the commander. E.g, used as a mark to be kept in attacking deck when optimizing.
    private Map<Integer, Integer> cardMarks = new HashMap<>();

    private Card shuffledCommander;
    private List<Card> shuffledForts = new ArrayList<>();
    private List<Card> shuffledCards = new ArrayList<>();

    // card id -> card order
    private HashMap<Integer, List<Integer>> order = new HashMap<>();
    private int fortsPoolAmount, cardsPoolAmount;
    private List<Card> fortsPool = new ArrayList<>();
    private List<Card> cardsPool = new ArrayList<>();

    private int deckSize;
    private int missionReq;
    private int level;

    private String deckString;
    private Set<Integer> vipCards = new HashSet<>();
    private List<Integer> givenHand = new ArrayList<>();
    private List<Card> fortressCards = new ArrayList<>();
    private List<SkillSpec> effects = new ArrayList<>();

    public Deck(DeckType deckType, int id, String name) {

        this.deckType = deckType;
        this.id = id;
        this.name = name;

        upgradePoints = 0;
        upgradeOpportunities = 0;
        strategy = DeckStrategy.RANDOM;
        commander = null;
        alphaDominion = null;
        shuffledCommander = null;
        deckSize = 0;
        missionReq = 0;

    }

    public Deck() {
        this(null, 0, null);
    }

    public void shuffle(Random random) {
        shuffledCommander = commander;

        shuffledForts.clear();
        shuffledForts.addAll(fortressCards);
        addRandomForts(random);

        shuffledCards.clear();
        shuffledCards.addAll(cards);
        addRandomCards(random);

        distributeUpgradePoints(random);

        if (strategy == DeckStrategy.ORDERED) {
            order.clear();
            int i = 0;
            for (Card card : cards) {
                order.get(card.getId()).add(i);
                i++;
            }
        }

        if (strategy != DeckStrategy.EXACT_ORDERED) {
            Collections.shuffle(shuffledCards, random);
            Collections.shuffle(shuffledForts, random);
        }
    }

    private void addRandomForts(Random random) {
        if (strategy != DeckStrategy.RANDOM) {
            throw new RuntimeException("Support only random strategy for raid/quest deck.");
        }
        Collections.shuffle(fortsPool, random);
        for (int i = 0; i < fortsPoolAmount; i++) {
            shuffledForts.add(fortsPool.get(i));
        }
    }

    private void addRandomCards(Random random) {
        if(strategy != DeckStrategy.RANDOM) {
            throw new RuntimeException("Support only random strategy for raid/quest deck");
        }
        Collections.shuffle(cardsPool, random);
        for (int i = 0; i < cardsPoolAmount; i++) {
            shuffledCards.add(cardsPool.get(i));
        }
    }

    private void distributeUpgradePoints(Random random) {
        int remainingUpgradePoints = upgradePoints;
        ArrayList<Card> cardsToUpgrade = new ArrayList<>();
        cardsToUpgrade.add(shuffledCommander);
        cardsToUpgrade.addAll(shuffledForts);
        cardsToUpgrade.addAll(shuffledCards);

        while (remainingUpgradePoints > 0 && cardsToUpgrade.size() > 0) {
            int randomIndex = random.nextInt(cardsToUpgrade.size());
            Card card = cardsToUpgrade.get(randomIndex);
            if (card.isTopLevelCard()) {
                cardsToUpgrade.remove(randomIndex);
            }
            card.upgradeSelf();
            remainingUpgradePoints--;
        }

        shuffledCommander = cardsToUpgrade.get(0);
    }

    public String shortDescription() {

        String desc = Decks.getDeckTypeAsString(deckType);

        if (id > 0)
            desc += " #" + id;

        if (!name.isEmpty())
            desc += " \"" + name + "\"";

        if (deckString.isEmpty()) {
            // TODO: Add hash to string
        } else
            desc += ": " + deckString;

        return desc;
    }

    public Card next() {
        // TODO: implement this
        return null;
    }

    @Override
    public Deck clone() {
        try {
            Deck copy = ((Deck) super.clone());
            copy.cards = new ArrayList<>(cards);
            copy.cardMarks = new HashMap<>(cardMarks);
            copy.shuffledForts = new ArrayList<>(shuffledForts);
            copy.shuffledCards = new ArrayList<>(shuffledCards);
            copy.order = new HashMap<>(order);
            copy.fortsPool = new ArrayList<>(fortsPool);
            copy.cardsPool = new ArrayList<>(cardsPool);
            copy.vipCards = new HashSet<>(vipCards);
            copy.givenHand = new ArrayList<>(givenHand);
            copy.fortressCards = new ArrayList<>(fortressCards);
            copy.effects = new ArrayList<>(effects);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    // Getters & Setters
    void setUpgradePoints(int upgradePoints) {
        this.upgradePoints = upgradePoints;
    }

    void setDeckStrategy(DeckStrategy strategy) {
        this.strategy = strategy;
    }

    public void setCommander(Card commander) {
        this.commander = commander;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public void setDeckString(String deckString) { this.deckString = deckString; }

    public List<Card> getShuffledCards() {
        return shuffledCards;
    }

    public Card getShuffledCommander() {
        return shuffledCommander;
    }

    public String getDeckString() {
        return deckString;
    }

    public List<Card> getCards() {
        return cards;
    }

    public int getId() { return id; }

}
