package com.github.tyrantsim.jtuo.sim;

public class Results {

    public long wins;
    public long draws;
    public long losses;
    public long points;
    public double points_lower_bound;
    public double points_upper_bound;
    public long n_sims;
    
    public Results() {}

    public Results(long wins, long draws, long losses, long points) {
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.points = points;
    }

    public Results add(final Results other) {
        wins += other.wins;
        draws += other.draws;
        losses += other.losses;
        points += other.points;
        return this;
    }

    // Getters & Setters
    public long getWins() {
        return wins;
    }

    public long getLosses() {
        return losses;
    }

    public long getDraws() {
        return draws;
    }

    public long getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "Results: [wins = " + wins + ", losses = " + losses + ", draws = " + draws + ", points = " + points + "]";
    }
}
