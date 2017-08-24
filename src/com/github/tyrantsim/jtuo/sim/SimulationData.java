package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.ArrayList;
import java.util.Random;

public class SimulationData {

    Random re;
    Cards cards;
    Decks decks;
    Hand yourHand;
    ArrayList<Deck> enemyDecks;
    ArrayList<Hand> enemyHands;
    ArrayList<Double> factors;
    GameMode gamemode;
    PassiveBGE[] yourBGEffects, enemyBGEffects;
    ArrayList<SkillSpec> yourBGSkills, enemyBGSkills;

    public ArrayList<Results> evaluate() {
        ArrayList<Results> res = new ArrayList<>(enemyHands.size());
        for (Hand enemyHand : enemyHands) {
            yourHand.reset(re);
            enemyHand.reset(re);
            Field fd = new Field();
            // TODO: a lot of setters here
            Results result = fd.play(); // ???
            if (!TyrantOptimize.modeOpenTheDeck) {
                // are there remaining (unopened) cards ?
                if (fd.getPlayers()[1].deck.getShuffledCards().size() != 0) {
                    // apply min score (there are unopened cards, so mission failed)
                    result.points = Cards.MIN_POSSIBLE_SCORE[TyrantOptimize.optimizationMode.ordinal()];
                }
            }
            res.add(result);
        }
        return res;
    }

}
