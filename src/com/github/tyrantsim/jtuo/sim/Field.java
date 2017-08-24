package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.Random;

public class Field {

    boolean end;
    Random random;
    // players[0]: the attacker, players[1]: the defender
    Hand[] players = new Hand[2];
    int tapi; // current turn's active player index
    int tipi; // and inactive
    Hand tap;
    Hand tip;
    ArrayList<CardStatus> selectionArray;
    int turn = 1;
    GameMode gameMode;
    OptimizationMode optimizationMode;
    PassiveBGE[] bg_effects = new PassiveBGE[2]; // passive BGE
    ArrayList<SkillSpec> bgSkills = new ArrayList<>(2);
    // With the introduction of on death skills, a single skill can trigger arbitrary many skills.
    // They are stored in this, and cleared after all have been performed.
    Deque<Pair<CardStatus, SkillSpec>> skillQueue;
    ArrayList<CardStatus> killedUnits;
    Map<CardStatus, Integer> damagedUnitsToItems;

    // the current phase of the turn: starts with PLAYCARD_PHASE, then COMMANDER_PHASE, STRUCTURES_PHASE, and ASSAULTS_PHASE
    FieldPhase currentPhase;
    // the index of the card being evaluated in the current phase.
    // Meaningless in playcard_phase,
    // otherwise is the index of the current card in players->structures or players->assaults
    int currentCI;

    boolean assaultBloodlusted = false;
    int bloodlustValue;


}
