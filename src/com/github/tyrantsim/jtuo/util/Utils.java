package com.github.tyrantsim.jtuo.util;

public class Utils {

    public static int safeMinus(int x, int y) {
        return (x > y) ? (x - y) : 0;
    }

    public static <T> boolean arrayContains(T[] arr, T obj) {
        for (T t: arr) {
            if (t.equals(obj))
                return true;
        }
        return false;
    }

}
