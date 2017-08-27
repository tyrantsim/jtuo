package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.ArrayList;
import java.util.Random;

public class SimulationData {

    Random random;
    Cards cards;
    Decks decks;
    Hand yourHand;
    ArrayList<Deck> enemyDecks;
    ArrayList<Hand> enemyHands;
    ArrayList<Double> factors;
    GameMode gameMode;
    PassiveBGE[] yourBGEffects, enemyBGEffects;
    ArrayList<SkillSpec> yourBGSkills, enemyBGSkills;

    public ArrayList<Results> evaluate() {
        ArrayList<Results> res = new ArrayList<>(enemyHands.size());
        for (Hand enemyHand : enemyHands) {
            yourHand.reset(random);
            enemyHand.reset(random);
            Field fd = new Field(
                    random,
                    cards,
                    yourHand, enemyHand,
                    gameMode,
                    TyrantOptimize.optimizationMode,
                    yourBGEffects, enemyBGEffects,
                    yourBGSkills, enemyBGSkills
            );
            Results result = TyrantOptimize.play(fd); // ???
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
