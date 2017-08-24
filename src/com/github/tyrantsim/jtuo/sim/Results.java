package com.github.tyrantsim.jtuo.sim;

public class Results {

    long wins;
    long draws;
    long losses;
    long points;

    Results add(final Results other) {
        wins += other.wins;
        draws += other.draws;
        losses += other.losses;
        points += other.points;
        return this;
    }

}
