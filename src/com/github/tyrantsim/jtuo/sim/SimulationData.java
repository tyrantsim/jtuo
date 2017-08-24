package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
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
    PassiveBGE[] your_bg_effects, enemy_bg_effects;
    ArrayList<SkillSpec> your_bg_skills, enemy_bg_skills;

}
