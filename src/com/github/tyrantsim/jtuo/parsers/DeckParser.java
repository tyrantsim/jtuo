package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.CardSpec;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;
import java.util.regex.Pattern;

public class DeckParser {

    /*
     *  Note:   DeckList data structure is Map<String, Double>
     *          The key is the deck name, the value is the factor (weight of deck out of all decks)
     */

    private static Set<String> expandingDecks = new HashSet<String>();


    /* Methods from read.cpp */

    public static Map<String, Double> normalize(Map<String, Double> deckList) {

        double factorSum = 0.0d;

        // Calculate sum of factors from deckList
        for (String key: deckList.keySet())
            factorSum += deckList.get(key);

        if (factorSum > 0) {
            // Divide factors by sum
            for (String key: deckList.keySet())
                deckList.replace(key, deckList.get(key) / factorSum);
        }

        return deckList;

    }

    public static Map<String, Double> expandDeckToList(String deckName) {

        if (expandingDecks.contains(deckName)) {
            // Circular reference, return empty decklist
            System.err.println("WARNING: There is a circular referred deck: " + deckName);
            return new HashMap<String, Double>();
        }

        String deckStr = deckName;
        Deck deck = Decks.findDeckByName(deckName);

        if (deck != null) {

            deckStr = deck.getDeckString();

            if (deckStr.contains(";") || deckStr.contains(":") || Decks.findDeckByName(deckName) != null) {
                // Deck name refers to deck list
                expandingDecks.add(deckName);
                Map<String, Double> deckList = parseDeckList(deckStr);
                expandingDecks.remove(deckName);
                return normalize(deckList);
            }
        }

        Map<String, Double> res = new HashMap<String, Double>();

        if (deckStr.length() >= 3 && deckStr.startsWith("/") && deckStr.endsWith("/")) {
            // Deck name is or refers to a regex
            Pattern p = Pattern.compile(deckStr.substring(1, deckStr.length() - 1));

            expandingDecks.add(deckName);
            for (String key: Decks.byName.keySet()) {
                if (p.matcher(key).find()) {
                    Map<String, Double> deckList = expandDeckToList(key);
                    for (String it: deckList.keySet()) {
                        res.put(it, deckList.get(it));
                    }
                }
            }
            expandingDecks.remove(deckName);

            if (res.size() == 0)
                System.err.println("Warning: Regular expression matches nothing: " + deckStr);

            return normalize(res);

        } else {
            // Deck name is a normal deck
            res.put(deckStr, 1.0d);
            return res;
        }

    }

    public static Map<String, Double> parseDeckList(String listString) {

        Map<String, Double> res = new HashMap<String, Double>();

        for (String token: listString.split(";")) {

            double factor = 1.0d;
            String deckName;

            String[] splitToken = token.split(":");
            deckName = splitToken[0].trim();
            if (splitToken.length > 1) {
                try {
                    factor = Double.parseDouble(splitToken[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("WARNING: Is ':' a typo? Skip deck [" + token + "]");
                }
            }

            Map<String, Double> deckList = expandDeckToList(deckName);
            for (String key: deckList.keySet())
                res.put(key, deckList.get(key) * factor);
        }


        return res;

    }

    public static CardSpec parseCardSpec(final String cardStr) throws Exception {

        CardSpec cardSpec = new CardSpec();

        // Default values
        int cardId = 0;
        int cardNum = 1;
        char numSign = 0;

        // Tokenize card string
        StringTokenizer tokenizer = new StringTokenizer(cardStr, "#(");
        String cardName = (String) tokenizer.nextElement();

        // Check lock mark
        if (cardName.startsWith("!")) {
            cardSpec.setMarked(true);
            cardName = cardName.substring(1);
        } else {
            cardSpec.setMarked(false);
        }

        String simpleName = Cards.simplifyName(cardName);

        // Check if string is abbreviation
        if (Cards.playerCardsAbbr.containsKey(simpleName))
            simpleName = Cards.simplifyName(Cards.playerCardsAbbr.get(simpleName));

        // Get card ID
        if (Cards.cardsByName.containsKey(simpleName)) {

            // Check if string is a card name
            cardId = Cards.cardsByName.get(simpleName).getId();
            if (Cards.ambiguousNames.contains(simpleName))
                System.err.println("WARNING: There are multiple cards named " + cardName
                        + " in cards.xml. [" + cardId + "] is used.");

        } else if (simpleName.contains("[") && simpleName.indexOf('[') < simpleName.indexOf(']')) {

            // Check ID number between '[]' quotes
            try {
                cardId = Integer.parseInt(simpleName.substring(
                        simpleName.indexOf('[') + 1, simpleName.indexOf(']')).trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: Squared brackets must contain a card ID number");
            }

        }

        // Parse second part of cardStr - after # or (
        if (tokenizer.hasMoreElements()) {
            String cardAmount = ((String) tokenizer.nextElement()).trim();

            // Get numSign (for adding or removing cards)
            if (cardAmount.startsWith("+") || cardAmount.startsWith("-") || cardAmount.startsWith("$"))
                numSign = cardAmount.charAt(0);

            // Get cardNum (amount of cards)
            try {
                cardNum = Integer.parseInt(cardAmount.replaceAll("[^\\d]", ""));
            } catch (NumberFormatException e) { /* Ignore */}

        }

        if (cardId == 0)
            throw new Exception("Unknown card: " + cardName);

        // Assign values to CardSpec object
        cardSpec.setCardId(cardId);
        cardSpec.setCardNum(cardNum);
        cardSpec.setNumSign(numSign);

        return cardSpec;
    }

    public static Pair<List<Integer>, Map<Integer, Character>> stringToIds(String deckString, String desc) {

        return null;
    }

}
