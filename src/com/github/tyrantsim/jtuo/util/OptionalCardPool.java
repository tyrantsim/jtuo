package com.github.tyrantsim.jtuo.util;

import com.github.tyrantsim.jtuo.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class OptionalCardPool {

    private int amount;
    private int replicates;
    private List<Card> pool;

    public OptionalCardPool() {
        this(1, 1, new ArrayList<Card>());
    }

    public OptionalCardPool(int amount, int replicates, List<Card> pool) {
        this.amount = amount;
        this.replicates = replicates;
        this.pool = pool;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getReplicates() {
        return replicates;
    }

    public void setReplicates(int replicates) {
        this.replicates = replicates;
    }

    public List<Card> getPool() {
        return pool;
    }

    public void setPool(List<Card> pool) {
        this.pool = pool;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof OptionalCardPool))
            return false;

        OptionalCardPool otherPool = (OptionalCardPool) other;
        if (this.amount == otherPool.amount && this.replicates == otherPool.replicates
                && this.pool.size() == otherPool.pool.size()) {
            for (int i = 0; i < this.pool.size(); i++) {
                if (!this.pool.get(i).equals(otherPool.pool.get(i)))
                    return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + amount;
        result = 37 * result + replicates;
        for (Card card: pool)
            result = 37 * result + card.hashCode();
        return result;
    }
}
