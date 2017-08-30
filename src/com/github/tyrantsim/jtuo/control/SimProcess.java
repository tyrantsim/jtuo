package com.github.tyrantsim.jtuo.control;

import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.sim.Results;
import com.github.tyrantsim.jtuo.sim.SimulationData;

import java.util.List;
import java.util.Random;

public class SimProcess {

    private Random random = new Random();
    private int numThreads;
    private Deck yourDeck;
    private List<Deck> enemyDecks;
    private List<Double> enemyDeckFactors;

    private EvaluatedResults evaluatedResults = null;

    public SimProcess(int optNumThreads, Deck yourDeck, List<Deck> enemyDecks, List<Double> enemyDeckFactors) {
        this.numThreads = optNumThreads;
        this.yourDeck = yourDeck;
        this.enemyDecks = enemyDecks;
        this.enemyDeckFactors = enemyDeckFactors;
    }

    public EvaluatedResults evaluate(int iterations) {
        evaluatedResults = new EvaluatedResults();
        evaluatedResults.totalBattles = iterations;

        // TODO: implement normal multithreading
        Results results = new Results();
        for (int i = 0; i < evaluatedResults.totalBattles; i++) {
            SimulationData simulationData = new SimulationData(random);
            simulationData.setDecks(yourDeck, enemyDecks);

            results.add(simulationData.evaluate().get(0));
        }
        evaluatedResults.results.add(results);

        return evaluatedResults;
    }

    // Getters & Setters
    public List<Double> getFactors() {
        return enemyDeckFactors;
    }

}
