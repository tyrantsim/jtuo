package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;

public class DeckParser {

    /*
     *  Note:   DeckList data structure is Map<String, Double>
     *          The key is the deck name, the value is the factor (weight of deck out of all decks)
     */

    /* Fields from read.cpp */

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
        // TODO
        return null;
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




}
