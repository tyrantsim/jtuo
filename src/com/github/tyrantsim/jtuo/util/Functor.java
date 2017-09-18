package com.github.tyrantsim.jtuo.util;

import com.github.tyrantsim.jtuo.sim.CardStatus;

public interface Functor {
    boolean match(CardStatus c);
}
