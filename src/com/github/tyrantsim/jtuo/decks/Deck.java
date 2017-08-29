package com.github.tyrantsim.jtuo.decks;

import com.github.tyrantsim.jtuo.Constants;
import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.parsers.DeckParser;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.OptionalCardPool;
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
    private Map<Integer, Boolean> cardMarks = new HashMap<>();

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

    // Optional card pools
    OptionalCardPool variableForts = new OptionalCardPool();
    OptionalCardPool variableCards = new OptionalCardPool();

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

    public void set(List<Integer> ids, Map<Integer, Boolean> marks) {

        commander = null;
        strategy = DeckStrategy.RANDOM;
        int nonDeckCardsSeen = 0;

        for (Integer id: ids) {
            Card card;
            try {
                card = Cards.getCardById(id);
            } catch (Exception e) {
                System.err.println("WARNING: " + e.getMessage());
                continue;
            }

            if (card.getType() == CardType.COMMANDER) {
                if (commander == null) {
                    commander = card;
                    if (marks.containsKey(-1))
                        cardMarks.put(-1, Boolean.TRUE);
                } else {
                    nonDeckCardsSeen++;
                    System.err.println("WARNING: Ignoring additional commander " + card.getName()
                            + " (" + commander.getName() + " already in deck)");
                }
            } else if (card.getCategory() == CardCategory.DOMINION_ALPHA) {
                addDominion(card, false);
                nonDeckCardsSeen++;
            } else if (card.getCategory() == CardCategory.FORTRESS_DEFENSE
                    || card.getCategory() == CardCategory.FORTRESS_SIEGE) {
                fortressCards.add(card);
                nonDeckCardsSeen++;
            } else {
                cards.add(card);
                int markDst = cards.size() - 1;
                int markSrc = markDst + nonDeckCardsSeen;

                if (marks.containsKey(markSrc))
                    cardMarks.put(markDst, Boolean.TRUE);
            }
        }

        if (commander == null)
            throw new RuntimeException("While constructing a deck: no commander found");

        commanderMaxLevel = commander.getTopLevelCard().getLevel();
        deckSize = cards.size();

    }

    public void addDominions(String deckString, boolean overrideDom) {
        // TODO: implement this
    }

    public void addDominion(Card domCard, boolean overrideDom) {
        // TODO: implement this
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

    public String hash() {

        List<Card> deckAllCards = new ArrayList<Card>();

        deckAllCards.add(commander);
        if (alphaDominion != null) deckAllCards.add(alphaDominion);
        deckAllCards.addAll(cards);

        if (strategy == DeckStrategy.RANDOM) {
            Collections.sort(deckAllCards.subList(deckAllCards.size() - cards.size(),
                    deckAllCards.size()), Comparator.comparingInt(Card::getId));
        }

        return encodeDeck(deckAllCards);

    }

    public String encodeDeck(List<Card> cards) {

        StringBuilder sb = new StringBuilder();

        for (Card card: cards) {
            int factor = card.getId();
            while (factor >= 32) {
                sb.append(Constants.BASE64_CHARS.toCharArray()[factor % 32]);
            }
            factor /= 32;
            sb.append(Constants.BASE64_CHARS.toCharArray()[factor + 32]);
        }

        return sb.toString();

    }

    public static void hashToIds(String hash, List<Integer> ids) {

        String chars = Constants.BASE64_CHARS;

        int charat = 0;

        while (charat < hash.length()) {
            String pc = hash.substring(charat);

            int id = 0;
            int factor = 1;
            String p;
            int d = chars.indexOf(pc.charAt(0));

            while (d < 32) {
                id += factor * d;
                factor *= 32;
                if (++charat < hash.length()) {
                    pc = hash.substring(charat);
                    p = chars.substring(chars.indexOf(pc.charAt(0)));
                    d = chars.indexOf(p.charAt(0));
                }
            }
            id += factor * (d - 32);
            charat++;
            ids.add(id);
        }
    }

    public String shortDescription() {

        String desc = Decks.getDeckTypeAsString(deckType);

        if (id > 0)
            desc += " #" + id;

        if (!name.isEmpty())
            desc += " \"" + name + "\"";

        if (deckString.isEmpty()) {
            desc += ": " + hash();
        } else
            desc += ": " + deckString;

        return desc;
    }

    public String mediumDescription() {

        StringBuilder sb = new StringBuilder(shortDescription() + "\n");

        if (commander != null)
            sb.append(commander.getName());
        else
            sb.append("No commander");

        if (alphaDominion != null) {
            sb.append(", ");
            sb.append(alphaDominion.getName());
        }

        for (Card card: fortressCards) {
            sb.append(", ");
            sb.append(card.getName());
        }

        for (Card card: cards) {
            sb.append(", ");
            sb.append(card.getName());
        }

        int numPoolCards;
        if ((numPoolCards = variableForts.getAmount() * variableForts.getReplicates()) > 0) {
            sb.append(", and ");
            sb.append(numPoolCards);
            sb.append(" fortresses from pool");
        }

        if ((numPoolCards = variableCards.getAmount() * variableCards.getReplicates()) > 0) {
            sb.append(", and ");
            sb.append(numPoolCards);
            sb.append(" cards from pool");
        }

        if (upgradePoints > 0) {
            sb.append(" +");
            sb.append(upgradePoints);
            sb.append("/");
            sb.append(upgradeOpportunities);
        }

        return sb.toString();

    }

    public String longDescription() {

        StringBuilder sb = new StringBuilder(mediumDescription() + "\n");

        if (commander != null)
            showUpgrades(sb, commander, commanderMaxLevel, "");
        else
            sb.append("No commander\n");

        for (Card card: fortressCards)
            showUpgrades(sb, card, card.getTopLevelCard().getLevel(), "");

        for (Card card: cards)
            showUpgrades(sb, card, card.getTopLevelCard().getLevel(), "");

        if (variableForts.getReplicates() > 0) {
            sb.append(variableForts.getReplicates());
            sb.append(" copies of each of ");
        }
        sb.append(variableForts.getAmount());
        sb.append(" in:\n");
        for (Card card: variableForts.getPool())
            showUpgrades(sb, card, card.getTopLevelCard().getLevel(), "  ");

        if (variableCards.getReplicates() > 0) {
            sb.append(variableCards.getReplicates());
            sb.append(" copies of each of ");
        }
        sb.append(variableCards.getAmount());
        sb.append(" in:\n");
        for (Card card: variableCards.getPool())
            showUpgrades(sb, card, card.getTopLevelCard().getLevel(), "  ");

        return sb.toString();
    }

    public void showUpgrades(StringBuilder sb, Card card, int cardMaxLevel, String leadingChars) {
        // TODO: implement this
    }

    public void resolve() {
        if (commander != null)
            return;

        Pair<List<Integer>, Map<Integer, Boolean>> idMarks = DeckParser.stringToIds(deckString, shortDescription());
        set(idMarks.getFirst(), idMarks.getSecond());
        deckString = "";

    }

    public void shrink(int deckLength) {
        if (cards.size() > deckLength)
            cards.subList(deckLength, cards.size()).clear();
    }

    public void setVipCards(String deckString) {
        vipCards.addAll(DeckParser.stringToIds(deckString, "vip").getFirst());
    }

    public void setGivenHand(String deckString) {
        givenHand = DeckParser.stringToIds(deckString, "hand").getFirst();
    }

    public void addForts(String deckString) {
        Pair<List<Integer>, Map<Integer, Boolean>> idMarks = DeckParser.stringToIds(deckString, "fortress_cards");
        for (Integer id: idMarks.getFirst()) {
            try {
                fortressCards.add(Cards.getCardById(id));
            } catch (Exception e) {
                System.err.println("Warning: " + e.getMessage());
            }
        }
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

    public Card getAlphaDominion() { return alphaDominion; }

}
