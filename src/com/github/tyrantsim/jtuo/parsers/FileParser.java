package com.github.tyrantsim.jtuo.parsers;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FileParser {

    private static Map<Integer, Integer> customCards = new HashMap<Integer, Integer>();

    public static void readCardAbbrs(String filename) throws FileNotFoundException {

        File file = new File(filename);

        try (Scanner sc = new Scanner(file)) {

            int numLine = 0;
            while (sc.hasNext()) {
                numLine++;
                String line = sc.nextLine().trim();

                if (ParserUtils.isLineIgnored(line))
                    continue;

                String[] splitLine;
                if (line.contains(":") && !(splitLine = line.split(":"))[0].trim().isEmpty() && !splitLine[1].trim().isEmpty()) {
                    if (Cards.cardsByName.containsKey(splitLine[0].trim())) {
                        System.err.println("Warning in card abbreviation file " + filename + " at line "
                                + numLine + ": ignored because the name has been used by an existing card.");
                    } else {
                        // OK - put card name in abbrs map
                        Cards.playerCardsAbbr.put(Cards.simplifyName(splitLine[0].trim()), splitLine[1].trim());
                    }
                } else {
                    System.err.println("Error in card abbreviation file " + filename + " at line "
                            + numLine + ", could not read the name.");
                }
            }
        }
    }

    public static Map<Integer, Integer> readCustomCards(String filename, boolean abortOnMissing) throws Exception {

        File file = new File(filename);

        int numLine = 0;
        try (Scanner sc = new Scanner(file)) {

            while (sc.hasNext()) {
                numLine++;
                String line = sc.nextLine().trim();

                if (ParserUtils.isLineIgnored(line))
                    continue;

                int cardId = 0;
                int cardNum = 1;
                char numSign = 0;
                char mark = 0;
                DeckParser.parseCardSpec(line, cardId, cardNum, numSign, mark);
                customCards.put(cardId, cardNum);
            }

        } catch (Exception e) {
            if (abortOnMissing) {
                throw e;
            } else {
                System.err.print("Exception while parsing the custom cards file " + filename);
                if (numLine > 0)
                    System.err.print(" at line " + numLine);
                System.err.println(": " + e.getMessage());
            }
        }

        return customCards;
    }

    public static void loadCustomDecks(String filename) {

        File file = new File(filename);

        int numLine = 0;
        try (Scanner sc = new Scanner(file)) {

            while (sc.hasNext()) {
                numLine++;
                String line = sc.nextLine().trim();

                if (ParserUtils.isLineIgnored(line))
                    continue;

                String[] splitLine;
                if (line.contains(":") && !(splitLine = line.split(":"))[0].trim().isEmpty() && !splitLine[1].trim().isEmpty()) {

                    Deck deck = DeckParser.findDeckById(splitLine[0].trim());

                    if (deck != null)
                        System.err.println("Warning in custom deck file " + filename + " at line " + numLine
                                + ", name conflicts, overrides " + deck.shortDescription());

                    // TODO: Not finished, figure this part out

                } else {
                    System.err.println("Error in custom deck file " + filename + " at line "
                            + numLine + ", could not read the deck name.");
                }
            }
        } catch (Exception e) {
            System.err.print("Exception while parsing the custom cards file " + filename);
            if (numLine > 0)
                System.err.print(" at line " + numLine);
            System.err.println(": " + e.getMessage());
        }

    }

    public static void addOwnedCard(Map<Integer, Integer> ownedCards, String cardSpec) throws Exception {

        int cardId = 0;
        int cardNum = 1;
        char numSign = 0;
        char mark = 0;
        DeckParser.parseCardSpec(cardSpec, cardId, cardNum, numSign, mark);
        Cards.getCardById(cardId);
        assert (mark == 0);

        if (numSign == 0) {
            ownedCards.put(cardId, cardNum);
        } else if (numSign == '+') {
            ownedCards.put(cardId, ownedCards.getOrDefault(cardId, 0) + cardNum);
        } else if (numSign == '-') {
            ownedCards.put(cardId, (ownedCards.getOrDefault(cardId, 0) > cardNum)
                    ? ownedCards.get(cardId) - cardNum : 0);
        }
    }

    public static void readOwnedCards(Map<Integer, Integer> ownedCards, String filename) {

        File file = new File(filename);

        if (!file.exists()) {

            // Parse as string
            try {
                for (String cardSpec: filename.split("\\s*,\\s*"))
                    addOwnedCard(ownedCards, cardSpec);
            } catch (Exception e) {
                System.err.println("Error: Failed to parse owned cards: '" + filename
                        + "' is neither a file nor a valid set of cards (" + e.getMessage() + ")");
            }

        } else {

            // Parse as file
            int numLine = 0;
            try (Scanner sc = new Scanner(file)) {

                while (sc.hasNext()) {
                    numLine++;
                    String line = sc.nextLine().trim();

                    if (ParserUtils.isLineIgnored(line))
                        continue;

                    try {
                        addOwnedCard(ownedCards, line);
                    } catch (Exception e) {
                        System.err.println("Error in owned cards file " + filename + " at line "
                                + numLine +" while parsing card '" + line + "': " + e.getMessage());
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("Ownedcards file not found: " + filename);
            }
        }

    }

    public static void readBgeAliases(Map<String, String> bgeAliases, String filename) {
        // TODO
    }

}