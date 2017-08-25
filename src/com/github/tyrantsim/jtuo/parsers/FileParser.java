package com.github.tyrantsim.jtuo.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;

public class FileParser {

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
                if (line.contains(":") && !(splitLine = line.split(":"))[1].trim().isEmpty()) {
                    // TODO
                }

            }
        }

    }

    public static Map<Integer, Integer> readCustomCards(String filename, boolean abortOnMissing) {
        // TODO
        return null;
    }

    public static void loadCustomDecks(String filename) {
        // TODO
    }

    public static void addOwnedCard(Map<Integer, Integer> ownedCards, String cardSpec) {
        // TODO
    }

    public static void readOwnedCards(Map<Integer, Integer> ownedCards, String filename) {
        // TODO
    }

    public static void readBgeAliases(Map<String, String> bgeAliases, String filename) {
        // TODO
    }


}
