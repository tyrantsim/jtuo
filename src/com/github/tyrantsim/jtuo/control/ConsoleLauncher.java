package com.github.tyrantsim.jtuo.control;

import com.github.tyrantsim.jtuo.Constants;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.DeckStrategy;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import com.github.tyrantsim.jtuo.parsers.DeckParser;
import com.github.tyrantsim.jtuo.sim.FieldSimulator;
import com.github.tyrantsim.jtuo.sim.Results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tyrantsim.jtuo.Constants.VERSION;

public class ConsoleLauncher {

    private static final String USAGE = "usage: ./tuo.exe Your_Deck Enemy_Deck [Flags] [Operations]\n" +
            "\n" +
            "Your_Deck:\n" +
            "  the name/hash/cards of a custom deck.\n" +
            "\n" +
            "Enemy_Deck:\n" +
            "  semicolon separated list of defense decks, syntax:\n" +
            "  deck1[:factor1];deck2[:factor2];...\n" +
            "  where deck is the name/hash/cards of a mission, raid, quest or custom deck, and factor is optional. The default factor is 1.\n" +
            "  example: 'fear:0.2;slowroll:0.8' means fear is the defense deck 20% of the time, while slowroll is the defense deck 80% of the time.\n" +
            "\n" +
            "Flags:\n" +
            "  -e \"<effect>\": set the battleground effect; you may use -e multiple times.\n" +
            "  -r: the attack deck is played in order instead of randomly (respects the 3 cards drawn limit).\n" +
            "  -s: use surge (default is fight).\n" +
            "  -t <num>: set the number of threads, default is 4.\n" +
            "  win:     simulate/optimize for win rate. default for non-raids.\n" +
            "  defense: simulate/optimize for win rate + stall rate. can be used for defending deck or win rate oriented raid simulations.\n" +
            "  raid:    simulate/optimize for average raid damage (ARD). default for raids.\n" +
            "Flags for climb:\n" +
            "  -c: don't try to optimize the commander.\n" +
            "  -L <min> <max>: restrict deck size between <min> and <max>.\n" +
            "  -o: restrict to the owned cards listed in \"data/ownedcards.txt\".\n" +
            "  -o=<filename>: restrict to the owned cards listed in <filename>.\n" +
            "  fund <num>: invest <num> SP to upgrade cards.\n" +
            "  target <num>: stop as soon as the score reaches <num>.\n" +
            "\n" +
            "Operations:\n" +
            "  sim <num>: simulate <num> battles to evaluate a deck.\n" +
            "  climb <num>: perform hill-climbing starting from the given attack deck, using up to <num> battles to evaluate a deck.\n" +
            "  reorder <num>: optimize the order for given attack deck, using up to <num> battles to evaluate an order.";

    static class Todo {
        Operation operation;
        int iterations;
    }

    public static void printVersion() {
        System.out.println("Tyrant Unleashed Optimizer (Java) v" + VERSION);
    }

    public static void printUsage() {
        printVersion();
        System.out.println(USAGE);
    }

    public static void printResults(EvaluatedResults results, List<Double> factors) {
        System.out.print("win%: " + (results.getWins() * 100.0) + " (");
        for (Results res : results.getResults()) {
            System.out.print(res.getWins() + " ");
        }
        System.out.println("/ " + results.getTotalBattles() + ")");

        System.out.print("stall%: " + (results.getDraws() * 100.0) + " (");
        for (Results res : results.getResults()) {
            System.out.print(res.getDraws() + " ");
        }
        System.out.println("/ " + results.getTotalBattles() + ")");

        System.out.print("loss%: " + (results.getLosses() * 100.0) + " (");
        for (Results res : results.getResults()) {
            System.out.print(res.getLosses() + " ");
        }
        System.out.println("/ " + results.getTotalBattles() + ")");

        // TODO: print score
    }

    public static void run(String[] args) {
        int optNumThreads = Constants.DEFAULT_THREAD_NUMBER;
        DeckStrategy optYourDeckStrategy = DeckStrategy.RANDOM;
        DeckStrategy optEnemyStrategy = DeckStrategy.RANDOM;

        List<Todo> optTodo = new ArrayList();

        for (int argIndex = 2; argIndex < args.length; argIndex++) {
            String arg = args[argIndex];
            if (arg.equals("threads") || arg.equals("-t")) {
                optNumThreads = Integer.parseInt(args[argIndex + 1]);
                argIndex += 1;
            } else if (arg.equals("turnlimit")) {
                FieldSimulator.turnLimit = Integer.parseInt(args[argIndex + 1]);
                argIndex += 1;
            } else if (arg.equals("sim")) {
                Todo todo = new Todo();
                todo.operation = Operation.SIMULATE;
                todo.iterations = Integer.parseInt(args[argIndex + 1]);
                optTodo.add(todo);
                argIndex += 1;

                if (optTodo.size() < 10) {
                    optNumThreads = 1;
                }
            } else {
                System.err.println("Error: Unknown option " + arg);
                return;
            }
        }

        CardsParser.initialize();

        String yourDeckName = args[0];
        String enemyDeckList = args[1];
        Map<String, Double> deckListParsed = DeckParser.parseDeckList(enemyDeckList);

        Deck yourDeck = null;
        List<Deck> enemyDecks = new ArrayList<>();
        List<Double> enemyDeckFactors = new ArrayList<>();

        try {
            yourDeck = Decks.findDeck(yourDeckName).clone();
        } catch(RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error: Deck " + yourDeckName + ": " + e.getMessage());
            return;
        }

        if (yourDeck == null) {
            System.err.println("Error: Invalid attack deck name/hash " + yourDeckName);
            return;
        }

        yourDeck.setDeckStrategy(optYourDeckStrategy);

        for (String key : deckListParsed.keySet()) {
            Double value = deckListParsed.get(key);
            Deck enemyDeck = null;

            try {
                enemyDeck = Decks.findDeck(key).clone();
            } catch (RuntimeException e) {
                System.err.println("Error: Deck " + value + ": " + e.getMessage());
                return;
            }

            if (enemyDeck == null) {
                System.err.println("Error: Invalid defense deck name/hash " + value);
                return;
            }

            enemyDeck.setDeckStrategy(optEnemyStrategy);

            enemyDecks.add(enemyDeck);
            enemyDeckFactors.add(value);
        }

        System.out.println("Your Deck: " + yourDeck.hash());
        System.out.println(yourDeckName);
        List<String> deckNames = new ArrayList<>();
        deckNames.addAll(deckListParsed.keySet());
        for (int i = 0; i < enemyDecks.size(); i++) {
            System.out.println("Enemy's Deck " + (i + 1) + ": " + enemyDecks.get(i).hash());
            System.out.println(deckNames.get(i));
        }

        SimProcess p = new SimProcess(optNumThreads, yourDeck, enemyDecks, enemyDeckFactors);

        for (Todo op : optTodo) {
            switch(op.operation) {
                case SIMULATE:
                    EvaluatedResults results = p.evaluate(op.iterations);
                    printResults(results, p.getFactors());
                    break;
            }
        }
    }

}
