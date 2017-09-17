package com.github.tyrantsim.jtuo.util;

import java.text.DecimalFormat;

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

    public static String formatPercentage(double value, int width) {

        DecimalFormat decFormat = new DecimalFormat("0.00");
        String num = decFormat.format(value);
        String padding = "";

        for (int i = 0; i <= width - num.length(); i++)
            padding += " ";

        return padding + num;

    }

}
