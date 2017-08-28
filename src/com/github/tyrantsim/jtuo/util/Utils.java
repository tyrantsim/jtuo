package com.github.tyrantsim.jtuo.util;

public class Utils {

    public static int safeMinus(int x, int y) {
        return (x > y) ? (x - y) : 0;
    }

}
