package com.github.tyrantsim.jtuo.cards;

import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.*;

@SuppressWarnings("unchecked")
public class Cards {


    /* Arrays and maps from tyrant.h */

    // Card upgrade SP cost
    public static final int[] UPGARDE_COST = {0, 5, 15, 30, 75, 150};

    // Dominion cost 2D array of maps
    public static Map<Card, Integer>[][] dominionCost = new HashMap[3][7];

    // Dominion refund 2D array of maps
    public static Map<Card, Integer>[][] dominionRefund = new HashMap[3][7];

    // Minimum possible score in different game modes
    public static final int[] MIN_POSSIBLE_SCORE = {0, 0, 0, 10, 5, 5, 5, 0};

    // Maximum possible score in different game modes
    public static final int[] MAX_POSSIBLE_SCORE = {100, 100, 100, 100, 65, 65, 100, 100};

    /* End of arrays and maps from tyrant.h */


    /* Data initialization from cards.h */

    // All cards list
    public static List<Card> allCards = new ArrayList<Card>(50000);

    // Cards by ID map
    public static Map<Integer, Card> cardsById = new HashMap<Integer, Card>();

    // Unordered set of player cards
    public static Set<Card> playerCards = new HashSet<Card>();

    // Cards by name map
    public static Map<String, Card> cardsByName = new HashMap<String, Card>();

    // List of player commanders
    public static List<Card> playerCommanders = new ArrayList<Card>();

    // List of player assaults
    public static List<Card> playerAssaults = new ArrayList<Card>();

    // List of player structures
    public static List<Card> playerStructures = new ArrayList<Card>();

    // Map of player cards abbreviations
    public static Map<String, String> playerCardsAbbr = new HashMap<String, String>();

    // Unordered set of visible cards (???hand maybe???)
    public static Set<Integer> visibleCardset = new HashSet<Integer>();

    // Unordered set of ambiguous names (???)
    public static Set<String> ambiguousNames = new HashSet<String>();

    /* End of data from cards.h */


    /* Start of cards.cpp functions */

    public static String simplifyName(final String cardName) {
        // Remove all characters ; : , " ' ! whitespace
        return cardName.replaceAll("[\\s;:,\"'!]", "").toLowerCase();
    }

    public static Card getCardById(final int id) throws Exception {
        if (!cardsById.containsKey(id))
            throw new Exception("No card with ID " + id);
        else
            return cardsById.get(id);
    }

    public static String getCardNameByIdSafe(final int id) {
        try {
            return getCardById(id).getName();
        } catch (Exception e) {
            return "UnknownCard.id[" + id + "]";
        }
    }

    public static Card getCardByName(String name) throws Exception {
        if (cardsByName.containsKey(simplifyName(name)))
            return cardsByName.get(simplifyName(name));
        throw new Exception("Unknown card name: " + name);
    }

    public static void organize() {

        cardsById.clear();
        playerCards.clear();
        cardsByName.clear();
        playerCommanders.clear();
        playerAssaults.clear();
        playerStructures.clear();

        // Round 1: set cardsById
        for (Card card: allCards)
            cardsById.put(card.id, card);

        // Round 2: depend on cards_by_id / by_id(); update m_name, [TU] m_top_level_card etc.; set cards_by_name;
        for (Card card: allCards) {

            // Remove delimiters from card names
            card.name = card.name.replaceAll("[;:,]", "");

            // set m_top_level_card for non base cards
            try {
                card.topLevelCard = getCardById(card.baseId).topLevelCard;
            } catch (Exception e) {
                // TODO: Exception handling
            }

            // Cards available ("visible") to players have priority
            if (card.equals(card.topLevelCard))
                addCard(card, card.name + "-" + card.level);
            else
                card.name += "-" + card.level;
            addCard(card, card.name);

        }

        // Round 3: depend on summon skill card_id check that card_id
        for (Card card: allCards) {

            int summonCardId = card.skillValue[Skill.SUMMON.ordinal()];

            if (summonCardId != 0) {
                try {
                    getCardById(summonCardId);
                } catch (Exception e) {
                    System.err.println("WARNING: Card [" + card.id + "] (" + card.name
                            + ") summons an unknown card [" + summonCardId + "] (Removing invalid skill Summon)");
                    card.skills.removeIf(ss -> ss.getId() == Skill.SUMMON);
                    card.skillValue[Skill.SUMMON.ordinal()] = 0;
                }
            }
        }

    }

    public static void fixDominionRecipes() {
        for (Card card: allCards) {
            if (card.category == CardCategory.DOMINION_ALPHA) {
                Map<Card, Integer> domCost = dominionCost[card.fusionLevel][card.level];
                for (Card key: domCost.keySet()) {
                    // except basic Alpha Dominion (id 50001 & 50002 for lvl 1 & 2 respectively)
                    if (card.id != 50001 && card.id != 50002)
                        card.recipeCards.replace(key, domCost.get(key));
                }
                card.recipeCost = 0; // no SP required
            }
        }
    }

    public static void addCard(Card card, final String name) {

        String simpleName = simplifyName(name);

        // Detect card duplicates
        if (cardsByName.containsKey(simpleName)) {

            // Match param card set and card by name set
            if (visibleCardset.contains(cardsByName.get(simpleName).set)
                    == visibleCardset.contains(card.set)) {

                // Both cards have the same name in the same set
                ambiguousNames.add(simpleName);
            }

        } else {

            // No duplicates - Go!
            ambiguousNames.remove(simpleName);
            cardsByName.put(simpleName, card);

            if (visibleCardset.contains(card.set) && !playerCards.contains(card)) {

                playerCards.add(card);

                switch (card.type) {
                    case ASSAULT:
                        playerAssaults.add(card);
                        break;
                    case COMMANDER:
                        playerCommanders.add(card);
                        break;
                    case STRUCTURE:
                        playerStructures.add(card);
                        break;
                }

            }

        }

    }

    public static void eraseFusionRecipe(int id) throws Exception {
        cardsById.get(getCardById(id).baseId).recipeCards.clear();
    }

    /* End of cards.cpp functions */

    public static String cardDescription(Card c) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(c.getId());
        sb.append("] ");
        sb.append(c.getName());
        sb.append(" (");
        sb.append(Rarity.values()[c.getRarity()]);
        sb.append(" ");
        sb.append(c.getFaction());
        sb.append(" ");
        sb.append(c.getType());
        sb.append(") ");
        sb.append(c.getAttack());
        sb.append("/");
        sb.append(c.getHealth());
        sb.append("/");
        sb.append(c.getDelay());
        sb.append(" >>SKILLS:");

        for (SkillSpec s: c.getSkillsOnPlay()) {
            sb.append(" [");
            sb.append(s.description());
            sb.append("]");
        }

        for (SkillSpec s: c.getSkillsOnAttacked()) {
            sb.append(" [");
            sb.append(s.description());
            sb.append("]");
        }

        for (SkillSpec s: c.getSkills()) {
            sb.append(" [");
            sb.append(s.description());
            sb.append("]");
        }

        for (SkillSpec s: c.getSkillsOnDeath()) {
            sb.append(" [");
            sb.append(s.description());
            sb.append("]");
        }

        return sb.toString();

    }

}
