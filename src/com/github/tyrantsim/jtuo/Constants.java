package com.github.tyrantsim.jtuo;

public final class Constants {
    private Constants() {}

    public static final String VERSION = "0.1";

    public static final String DATA = "data";
    public static final String ASSETS = "http://mobile.tyrantonline.com/assets/";
    public static final String GITHUB = "https://raw.githubusercontent.com/brikikeks/tyrant_optimize/merged/data/";
//    UrlDownloadToFile, *0 https://raw.githubusercontent.com/brikikeks/tyrant_optimize/merged/data/raids.xml, data\raids.xml
//        if ErrorLevel
//        {
//            MsgBox, Error downloading raids.xml.
//            had_error := true
//        }
//        UrlDownloadToFile, *0 https://raw.githubusercontent.com/dsuchka/tyrant_optimize/merged/data/bges.txt, data\bges.txt

    public static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static final int DEFAULT_THREAD_NUMBER = 4;
    public static final int DEFAULT_TURN_LIMIT = 50;
}
