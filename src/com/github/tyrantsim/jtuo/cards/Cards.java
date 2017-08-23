package com.github.tyrantsim.jtuo.cards;

import com.github.tyrantsim.jtuo.skills.Skill;

import java.util.*;

@SuppressWarnings("unchecked")
public class Cards {


    /* Arrays and maps from tyrant.h */

    // Card upgrade SP cost
    int[] upgradeCost = {0, 5, 15, 30, 75, 150};

    // Dominion cost 2D array of maps
    Map<Card, Integer>[][] dominionCost = new HashMap[3][7];

    // Dominion refund 2D array of maps
    Map<Card, Integer>[][] dominionRefund = new HashMap[3][7];

    // Minimum possible score in different game modes
    int[] minPossibleScore = {0, 0, 0, 10, 5, 5, 5, 0};

    // Maximum possible score in different game modes
    int[] maxPossibleScore = {100, 100, 100, 100, 65, 65, 100, 100};

    /* End of arrays and maps from tyrant.h */


    /* Data initialization from cards.h */

    // All cards list
    List<Card> allCards = new ArrayList<Card>();

    // Cards by ID map
    Map<Integer, Card> cardsById = new HashMap<Integer, Card>();

    // Unordered set of player cards
    Set<Card> playerCards = new HashSet<Card>();

    // Cards by name map
    Map<String, Card> cardsByName = new HashMap<String, Card>();

    // List of player commanders
    List<Card> playerCommanders = new ArrayList<Card>();

    // List of player assaults
    List<Card> playerAssaults = new ArrayList<Card>();

    // List of player structures
    List<Card> playerStructures = new ArrayList<Card>();

    // Map of player cards abbreviations
    Map<String, String> playerCardsAbbr = new HashMap<String, String>();

    // Unordered set of visible cards (???hand maybe???)
    Set<Integer> visibleCardset = new HashSet<Integer>();

    // Unordered set of ambiguous names (???)
    Set<String> ambiguousNames = new HashSet<String>();

    /* End of data from cards.h */


    /* Start of cards.cpp functions */

    public static String simplifyName(final String cardName) {
        // Remove all characters ; : , " ' ! whitespace
        return cardName.replaceAll("[\\s;:,\"'!]", "").toLowerCase();
    }

    public static List<String> getAbbreviations(final String name) {

        // TODO
        return null;
    }

    final Card getCardById(final int id) throws Exception {
        if (!cardsById.containsKey(id))
            throw new Exception("No card with ID " + id);
        else
            return cardsById.get(id);
    }

    void organize() {

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

    void fixDominionRecipes() {
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

    void addCard(Card card, final String name) {

        // Detect card duplicates
        if (!cardsByName.containsKey(name)) {

            // No duplicates - Go!
            // TODO

        } else {

            // Match param card and card by name
            // TODO

        }

    }

    void eraseFusionRecipe(int id) throws Exception {
        cardsById.get(getCardById(id).baseId).recipeCards.clear();
    }

    /* End of cards.cpp functions */


}
