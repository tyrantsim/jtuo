package com.github.tyrantsim.jtuo;

import com.github.tyrantsim.jtuo.control.ConsoleLauncher;

public class Main {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-version")) {
            ConsoleLauncher.printVersion();
            return;
        }
        if (args.length <= 1) {
            // In future, start GUI here
            ConsoleLauncher.printUsage();
        } else {
            ConsoleLauncher.run(args);
        }
    }

}
