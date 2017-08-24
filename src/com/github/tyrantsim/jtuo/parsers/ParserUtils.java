package com.github.tyrantsim.jtuo.parsers;

public class ParserUtils {

    /* Method from read.h */

    // Check if a line from a text file should be ignored (empty or commented)
    public static boolean isLineIgnored(String line) {
        return line.trim().isEmpty() || line.trim().startsWith("//");
    }

}
