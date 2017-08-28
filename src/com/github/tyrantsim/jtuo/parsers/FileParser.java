package com.github.tyrantsim.jtuo.parsers;

import static com.github.tyrantsim.jtuo.parsers.DeckParser.parseCardSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.github.tyrantsim.jtuo.cards.CardSpec;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.DeckType;
import com.github.tyrantsim.jtuo.decks.Decks;


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

                if (line.contains(":") && !line.substring(line.indexOf(":") + 1).trim().isEmpty()
                        && !line.substring(0, line.indexOf(":")).trim().isEmpty()) {

                    if (Cards.cardsByName.containsKey(Cards.simplifyName(line.substring(0, line.indexOf(":")).trim()))) {
                        System.err.println("Warning in card abbreviation file " + filename + " at line "
                                + numLine + ": ignored because the name has been used by an existing card.");
                    } else {
                        // OK - put card name in abbrs map
                        Cards.playerCardsAbbr.put(Cards.simplifyName(line.substring(0, line.indexOf(":")).trim()),
                                line.substring(line.indexOf(":") + 1).trim());
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

                CardSpec cardSpec = parseCardSpec(line);
                customCards.put(cardSpec.getCardId(), cardSpec.getCardNum());
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

                if (line.contains(":") && !line.substring(line.indexOf(":") + 1).trim().isEmpty()
                        && !line.substring(0, line.indexOf(":")).trim().isEmpty()) {

                    Deck deck = Decks.findDeckByName(line.substring(0, line.indexOf(":")).trim());

                    if (deck != null)
                        System.err.println("Warning in custom deck file " + filename + " at line " + numLine
                                + ", name conflicts, overrides " + deck.shortDescription());

                    deck = new Deck(DeckType.CUSTOM_DECK, numLine, line.substring(0, line.indexOf(":")).trim());
                    deck.setDeckString(line.substring(line.indexOf(":") + 1).trim());
                    Decks.decks.add(deck);
                    Decks.addDeck(deck, line.substring(0, line.indexOf(":")).trim());
                    Decks.addDeck(deck, "Custom Deck #" + deck.getId());

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

    public static void addOwnedCard(Map<Integer, Integer> ownedCards, String cardStr) throws Exception {

        CardSpec cardSpec = DeckParser.parseCardSpec(cardStr);

        int cardId = cardSpec.getCardId();
        int cardNum = cardSpec.getCardNum();
        char numSign = cardSpec.getNumSign();

        Cards.getCardById(cardId);

        if (cardSpec.getNumSign() == 0) {
            ownedCards.put(cardId, cardNum);
        } else if (numSign == '+') {
            ownedCards.put(cardId, ownedCards.getOrDefault(cardId, 0) + cardNum);
        } else if (numSign == '-') {
            ownedCards.put(cardId, (ownedCards.getOrDefault(cardId, 0) > cardNum)
                    ? ownedCards.get(cardId) - cardNum : 0);

            if (ownedCards.get(cardId) == 0)
                ownedCards.remove(cardId);
        }
    }

    public static void readOwnedCards(Map<Integer, Integer> ownedCards, String filename) {

        File file = new File(filename);

        if (!file.exists()) {

            // Parse as string
            try {
                for (String cardSpec: filename.split("\\s*,\\s*"))
                    addOwnedCard(ownedCards, cardSpec.trim());
            } catch (Exception e) {
                System.err.println("Error: Failed to parse owned cards: '" + filename
                        + "' is neither a file nor a valid set of cards (" + e.getMessage() + ")");
            }

        } else {

            // Parse as file
            int numLine = 0;
            try (Scanner sc = new Scanner(file)) {

                while (sc.hasNextLine()) {
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

        File file = new File(filename);

        int numLine = 0;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                numLine++;
                String line = sc.nextLine();

                if (ParserUtils.isLineIgnored(line))
                    continue;

                if (line.contains(":") && !line.substring(line.indexOf(":") + 1).trim().isEmpty()
                        && !line.substring(0, line.indexOf(":")).trim().isEmpty()) {

                    String[] splitLine = line.split(":");
                    bgeAliases.put(Cards.simplifyName(splitLine[0].trim()), splitLine[1].trim());
                } else {
                    System.err.println("Error in BGE file " + filename + " at line "
                            + numLine + ", could not read the name.");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("BGE file not found: " + filename);
        } catch (Exception e) {
            System.err.print("Exception while parsing the BGE file " + filename);
            if (numLine > 0)
                System.err.print(" at line " + numLine);
            System.err.println(": " + e.getMessage());
        }

    }

}
