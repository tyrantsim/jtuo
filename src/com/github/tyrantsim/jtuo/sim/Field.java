package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;

public class Field {

    private static final int PLAYER_INDEX_ATTACKER = 0;
    private static final int PLAYER_INDEX_DEFFENDER = 1;

    boolean end;
    Random random;
    Cards cards;
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
    PassiveBGE[] yourBGEffects, enemyBGEffects;
    List<SkillSpec> yourBGSkills, enemyBGSkills;
    // With the introduction of on death skills, a single skill can trigger arbitrary many skills.
    // They are stored in this, and cleared after all have been performed.
    Deque<Pair<CardStatus, SkillSpec>> skillQueue;
    List<CardStatus> killedUnits;
    Map<CardStatus, Integer> damagedUnitsToItems = new HashMap<>();

    // the current phase of the turn: starts with PLAYCARD_PHASE, then COMMANDER_PHASE, STRUCTURES_PHASE, and ASSAULTS_PHASE
    FieldPhase currentPhase;
    // the index of the card being evaluated in the current phase.
    // Meaningless in playcard_phase,
    // otherwise is the index of the current card in players->structures or players->assaults
    int currentCI;

    boolean assaultBloodlusted = false;
    int bloodlustValue;

    public Field(Random random, Cards cards, Hand yourHand, Hand enemyHand, GameMode gameMode,
                 OptimizationMode optimizationMode, PassiveBGE[] yourBGEffects, PassiveBGE[] enemyBGEffects,
                 List<SkillSpec> yourBGSkills, List<SkillSpec> enemyBGSkills) {
        this.end = false;
        this.random = random;
        this.cards = cards;
        this.players = new Hand[]{ yourHand, enemyHand };
        this.turn = 1;
        this.gameMode = gameMode;
        this.optimizationMode = optimizationMode;
        this.yourBGEffects = yourBGEffects;
        this.enemyBGEffects = enemyBGEffects;
        this.yourBGSkills = yourBGSkills;
        this.enemyBGSkills = enemyBGSkills;
        this.assaultBloodlusted = false;
    }

    public void prepareAction() {
        damagedUnitsToItems.clear();
    }

    public void finalizeAction() {
        for (CardStatus dmgStatus : damagedUnitsToItems.keySet()) {
            int dmg = damagedUnitsToItems.get(dmgStatus);

            if (dmg == 0 || !dmgStatus.isAlive()) continue;

            int barrierBase = dmgStatus.skill(Skill.BARRIER);
            if (barrierBase != 0) {
                int protectValue = barrierBase * dmg;
                dmgStatus.addProtection(protectValue);
            }
        }
    }

    public void nextTurn() {
        turn++;
    }

    // Getters & Setters
    public Hand[] getPlayers() {
        return players;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public boolean isEnd() {
        return end;
    }

    public Hand getPlayer(int index) {
        return getPlayers()[index];
    }

    public Hand getAttacker() {
        return getPlayer(PLAYER_INDEX_ATTACKER);
    }

    public Hand getDefender() {
        return getPlayer(PLAYER_INDEX_DEFFENDER);
    }

    public void setTapi(int tapi) {
        this.tapi = tapi;
    }

    public int getTapi() {
        return tapi;
    }

    public void setTipi(int tipi) {
        this.tipi = tipi;
    }

    public int getTipi() {
        return tipi;
    }

    public void setTap(Hand tap) {
        this.tap = tap;
    }

    public Hand getTap() {
        return tap;
    }

    public void setTip(Hand tip) {
        this.tip = tip;
    }

    public Hand getTip() {
        return tip;
    }

    public int getTurn() {
        return turn;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public OptimizationMode getOptimizationMode() {
        return optimizationMode;
    }

    public void setCurrentPhase(FieldPhase phase) {
        this.currentPhase = phase;
    }

    public FieldPhase getCurrentPhase() {
        return currentPhase;
    }

    public PassiveBGE[] getBGEffects(int playerIndex) {
        if (playerIndex == PLAYER_INDEX_ATTACKER) {
            return yourBGEffects;
        } else if (playerIndex == PLAYER_INDEX_DEFFENDER) {
            return enemyBGEffects;
        } else {
            throw new AssertionError("Unknown playerIndex: " + playerIndex);
        }
    }

    public List<SkillSpec> getBGSkills(int playerIndex) {
        if (playerIndex == PLAYER_INDEX_ATTACKER) {
            return yourBGSkills;
        } else if (playerIndex == PLAYER_INDEX_DEFFENDER) {
            return enemyBGSkills;
        } else {
            throw new AssertionError("Unknown playerIndex: " + playerIndex);
        }
    }

}
