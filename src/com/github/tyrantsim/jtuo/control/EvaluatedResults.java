package com.github.tyrantsim.jtuo.control;

import com.github.tyrantsim.jtuo.sim.Results;

import java.util.ArrayList;
import java.util.List;

public class EvaluatedResults {

    List<Results> results = new ArrayList<>();
    int totalBattles = 0;

    public double getWins() {
        double n = 0;
        for (Results res : results) {
            n += res.getWins();
        }
        return n / totalBattles;
    }

    public double getDraws() {
        double n = 0;
        for (Results res : results) {
            n += res.getDraws();
        }
        return n / totalBattles;
    }

    public double getLosses() {
        double n = 0;
        for (Results res : results) {
            n += res.getLosses();
        }
        return n / totalBattles;
    }

    // Getters & Setters
    public List<Results> getResults() {
        return results;
    }

    public int getTotalBattles() {
        return totalBattles;
    }

}
