package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimulationData {

    Random random;
    Cards cards;
    Decks decks;
    Deck yourDeck;
    Hand yourHand;
    List<Deck> enemyDecks = new ArrayList<>();
    List<Hand> enemyHands = new ArrayList<>();
    List<Double> factors = new ArrayList<>();
    GameMode gameMode;
    PassiveBGE[] yourBGEffects = new PassiveBGE[PassiveBGE.values().length];
    PassiveBGE[] enemyBGEffects = new PassiveBGE[PassiveBGE.values().length];
    List<SkillSpec> yourBGSkills = Collections.emptyList();
    List<SkillSpec> enemyBGSkills = Collections.emptyList();

    public SimulationData(Random random) {
        this.random = random;
    }

    public SimulationData(Random random, Cards cards, Decks decks, int numEnemyDecks, ArrayList<Double> factors,
                          GameMode gameMode, PassiveBGE[] yourBGEffects, PassiveBGE[] enemyBGEffects,
                          ArrayList<SkillSpec> yourBGSkills, ArrayList<SkillSpec> enemyBGSkills) {
        this.random = random;
        this.cards = cards;
        this.decks = decks;
        this.enemyDecks = new ArrayList<>(numEnemyDecks);
        this.factors = factors;
        this.gameMode = gameMode;
        this.yourBGEffects = yourBGEffects;
        this.enemyBGEffects = enemyBGEffects;
        this.yourBGSkills = yourBGSkills;
        this.enemyBGSkills = enemyBGSkills;
    }

    public void setDecks(Deck yourDeck, List<Deck> enemyDecks) {
        this.yourDeck = yourDeck.clone();
        this.yourHand = new Hand(this.yourDeck);

        enemyHands.clear();
        this.enemyDecks = new ArrayList<>(enemyDecks);
        for (Deck enemyDeck : this.enemyDecks) {
            enemyHands.add(new Hand(enemyDeck));
        }
    }

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
            Results result = FieldSimulator.play(fd);
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

    // Getters & Setters

}
