package com.github.tyrantsim.jtuo.util;

public class Utils {

    public static int safeMinus(int x, int y) {
        return (x > y) ? (x - y) : 0;
    }

    public static <T> boolean findInArray(T[] arr, T obj) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(obj))
                return true;
        }
        return false;
    }

}
