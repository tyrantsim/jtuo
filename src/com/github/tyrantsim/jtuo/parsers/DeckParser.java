package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;
import java.util.regex.Pattern;

public class DeckParser {

    /*
     *  Note:   DeckList data structure is Map<String, Double>
     *          The key is the deck name, the value is the factor (weight of deck out of all decks)
     */

    private static Set<String> expandingDecks = new HashSet<String>();

    public static Map<String, Deck> decksByName = new HashMap<String, Deck>();


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
        Deck deck = findDeckById(deckName);

        if (deck != null) {

            deckStr = deck.getDeckString();

            if (deckStr.contains(";") || deckStr.contains(":") || findDeckById(deckName) != null) {
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
            for (String key: decksByName.keySet()) {
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
            res.put(deckName, 1.0d);
            return res;
        }

    }

    public static Map<String, Double> parseDeckList(String listString) {

        Map<String, Double> res = new HashMap<String, Double>();

        if (listString.contains(";")) {
            for (String token: listString.split(";")) {

                double factor = 1.0d;
                String deckName;

                if (token.contains(":")) {
                    String[] splitToken = token.split(":");
                    deckName = splitToken[0];
                    try {
                        factor = Double.parseDouble(splitToken[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("WARNING: Is ':' a typo? Skip deck [" + token + "]");
                    }
                } else {
                    deckName = token;
                }

                Map<String, Double> deckList = expandDeckToList(deckName);
                for (String key: deckList.keySet())
                    res.put(key, deckList.get(key) * factor);

            }
        }

        return res;

    }

    public static Map<String, Double> parseCardSpec(final String cardSpec, int cardId, int cardNum, char numSign, char mark) {
        // TODO
        return null;
    }

    public static Pair<List<Integer>, Map<Integer, Character>> stringToIds(String deckString, String desc) {
        // TODO
        return null;
    }

    public static Deck findDeckById(String deckName) {
        return decksByName.getOrDefault(deckName, null);
    }

}
