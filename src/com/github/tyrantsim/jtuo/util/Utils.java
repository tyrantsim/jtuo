package com.github.tyrantsim.jtuo.util;

public class Utils {

    public static int safeMinus(int x, int y) {
        return (x > y) ? (x - y) : 0;
    }

    public static int[] cloneArray(int[] src) {
        int[] dst = new int[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }


}
