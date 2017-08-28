package com.github.tyrantsim.jtuo.util;

import com.github.tyrantsim.jtuo.cards.Card;

import java.util.ArrayList;
import java.util.List;

public class OptionalCardPool {

    private int amount;
    private int replicates;
    private List<Card> pool;

    public OptionalCardPool() {
        this(0, 0, new ArrayList<Card>());
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

    // TODO: override equals and hashcode methods

}
