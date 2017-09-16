package com.github.tyrantsim.jtuo.control;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.sim.Results;
import com.github.tyrantsim.jtuo.sim.SimulationData;

import java.util.List;
import java.util.Random;

public class SimProcess {

    private Random random = new Random();
    public Random getRandom() {
        return random;
    }

    private int numThreads;
    private Deck yourDeck;
    private List<Deck> enemyDecks;
    private List<Double> enemyDeckFactors;

    //private EvaluatedResults evaluatedResults = null;

    public SimProcess(int optNumThreads, Deck yourDeck, List<Deck> enemyDecks, List<Double> enemyDeckFactors) {
        this.numThreads = optNumThreads;
        this.yourDeck = yourDeck;
        this.enemyDecks = enemyDecks;
        this.enemyDeckFactors = enemyDeckFactors;
    }

    public List<Deck> getEnemyDecks() {
        return enemyDecks;
    }

    public void setEnemyDecks(List<Deck> enemyDecks) {
        this.enemyDecks = enemyDecks;
    }

    public EvaluatedResults evaluate(int iterations, EvaluatedResults evaluatedResults) {
        if (iterations <= evaluatedResults.getTotalBattles())
        {
            return evaluatedResults;
        }
//        TyrantOptimize.thread_iterations = iterations - evaluatedResults.getTotalBattles();
//        EvaluatedResults thread_results = evaluatedResults;
//        thread_compare = false;
//        // unlock all the threads
//        main_barrier.wait();
//        // wait for the threads
//        main_barrier.wait();
        return evaluatedResults;
    }

    
    public EvaluatedResults evaluate(int iterations) {
        EvaluatedResults evaluatedResults = new EvaluatedResults();
        evaluatedResults.totalBattles = iterations;

        // TODO: implement normal multithreading
        Results results = new Results();
        for (int i = 0; i < evaluatedResults.totalBattles; i++) {
            SimulationData simulationData = new SimulationData(random);
            simulationData.setDecks(yourDeck, enemyDecks);

            results.add(simulationData.evaluate(new TyrantOptimize()).get(0));
        }
        evaluatedResults.results.add(results);

        return evaluatedResults;
    }

    // Getters & Setters
    public List<Double> getFactors() {
        return enemyDeckFactors;
    }
   
    public EvaluatedResults compare(long  num_iterations, EvaluatedResults evaluated_results, Results best_results) {
        if (num_iterations <= evaluated_results.getTotalBattles()) {
            return evaluated_results;
        }
        
        // TODO: Replace C++ code
        
        long thread_num_iterations = num_iterations - evaluated_results.getTotalBattles();
        EvaluatedResults thread_results = evaluated_results;
        Results thread_best_results = best_results;
        boolean thread_compare = true;
        boolean thread_compare_stop = false;
        // unlock all the threads
        //main_barrier.wait();
//        // wait for the threads
        //main_barrier.wait();
        return evaluated_results;
    }
}
