package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.*;
import java.util.function.Predicate;

public class Field {

    private static final int PLAYER_INDEX_ATTACKER = 0;
    private static final int PLAYER_INDEX_DEFENDER = 1;

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
    int[] yourBGEffects = new int[PassiveBGE.values().length], enemyBGEffects = new int[PassiveBGE.values().length];
    List<SkillSpec> yourBGSkills, enemyBGSkills;
    // With the introduction of on death skills, a single skill can trigger arbitrary many skills.
    // They are stored in this, and cleared after all have been performed.
    Deque<Pair<CardStatus, SkillSpec>> skillQueue = new LinkedList<>();
    List<CardStatus> killedUnits = new ArrayList<>();
    Map<CardStatus, Integer> damagedUnitsToTimes = new HashMap<>();

    // the current phase of the turn: starts with PLAYCARD_PHASE, then COMMANDER_PHASE, STRUCTURES_PHASE, and ASSAULTS_PHASE
    FieldPhase currentPhase;
    // the index of the card being evaluated in the current phase.
    // Meaningless in playcard_phase,
    // otherwise is the index of the current card in players->structures or players->assaults
    int currentCI;

    boolean assaultBloodlusted = false;
    int bloodlustValue;

    public Field(Random random, Cards cards, Hand yourHand, Hand enemyHand, GameMode gameMode,
                 OptimizationMode optimizationMode, int[] yourBGEffects, int[] enemyBGEffects,
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
        this.selectionArray = new ArrayList<>();
    }

    public Field(Random random, Cards cards, Hand yourHand, Hand enemyHand, GameMode gameMode,
                 OptimizationMode optimizationMode, List<SkillSpec> yourBGSkills, List<SkillSpec> enemyBGSkills) {
        this.end = false;
        this.random = random;
        this.cards = cards;
        this.players = new Hand[]{ yourHand, enemyHand };
        this.turn = 1;
        this.gameMode = gameMode;
        this.optimizationMode = optimizationMode;
        this.yourBGSkills = yourBGSkills;
        this.enemyBGSkills = enemyBGSkills;
        this.assaultBloodlusted = false;
        this.selectionArray = new ArrayList<>();
    }

    public void prepareAction() {
        damagedUnitsToTimes.clear();
    }

    public void finalizeAction() {
        for (CardStatus dmgStatus : damagedUnitsToTimes.keySet()) {
            int dmg = damagedUnitsToTimes.get(dmgStatus);

            if (dmg == 0 || !dmgStatus.isAlive()) continue;

            int barrierBase = dmgStatus.skill(Skill.BARRIER);
            if (barrierBase != 0) {
                int protectValue = barrierBase * dmg;
                dmgStatus.addProtection(protectValue);
            }
        }
    }

    void addSkillToQueue(CardStatus status, SkillSpec skillSpec) {
        this.skillQueue.add(new Pair<>(status, skillSpec));
    }

    public void nextTurn() {
        turn++;
    }

    public boolean hasBGEffect(int playerIndex, PassiveBGE effect) {
        return getBGEffects(playerIndex)[effect.ordinal()] != 0;
    }

    public int getBGEffectValue(int playerIndex, PassiveBGE effect) {
        return getBGEffects(playerIndex)[effect.ordinal()];
    }

    void addBloodlust(int value) {
        this.bloodlustValue += value;
    }

    void incDamagedUnitsToTimes(CardStatus status) {
        int dmg = damagedUnitsToTimes.getOrDefault(status, 0) + 1;
        damagedUnitsToTimes.put(status, dmg);
    }

    public int getDamagedUnitsToTimes(CardStatus status) {
        return damagedUnitsToTimes.getOrDefault(status, 0);
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
        return getPlayer(PLAYER_INDEX_DEFENDER);
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

    public List<CardStatus> getKilledUnits() {
        return killedUnits;
    }

    public void setCurrentPhase(FieldPhase phase) {
        this.currentPhase = phase;
    }

    public FieldPhase getCurrentPhase() {
        return currentPhase;
    }

    public int[] getBGEffects(int playerIndex) {
        if (playerIndex == PLAYER_INDEX_ATTACKER) {
            return yourBGEffects;
        } else if (playerIndex == PLAYER_INDEX_DEFENDER) {
            return enemyBGEffects;
        } else {
            throw new AssertionError("Unknown playerIndex: " + playerIndex);
        }
    }

    public List<SkillSpec> getBGSkills(int playerIndex) {
        if (playerIndex == PLAYER_INDEX_ATTACKER) {
            return yourBGSkills;
        } else if (playerIndex == PLAYER_INDEX_DEFENDER) {
            return enemyBGSkills;
        } else {
            throw new AssertionError("Unknown playerIndex: " + playerIndex);
        }
    }

    public Random getRandom() { return random; }

    Deque<Pair<CardStatus, SkillSpec>> getSkillQueue() {
        return skillQueue;
    }

    void setCurrentCI(int currentCI) {
        this.currentCI = currentCI;
    }

    int getCurrentCI() {
        return currentCI;
    }

    void setAssaultBloodlusted(boolean assaultBloodlusted) {
        this.assaultBloodlusted = assaultBloodlusted;
    }

    boolean isAssaultBloodlusted() {
        return assaultBloodlusted;
    }

    void setBloodlustValue(int bloodlustValue) {
        this.bloodlustValue = bloodlustValue;
    }

    public CardStatus getLeftAssault(CardStatus status) {
        return getLeftAssault(status, 1);
    }

    public CardStatus getLeftAssault(CardStatus status, int n) {
        if (status.getIndex() >= n) {
            CardStatus leftAssault = players[status.getPlayer()].getAssaults().get(status.getIndex() - n);
            if (leftAssault.isAlive())
                return leftAssault;
        }
        return null;
    }

    public CardStatus getRightAssault(CardStatus status) {
        return getRightAssault(status, 1);
    }

    public CardStatus getRightAssault(CardStatus status, int n) {
        List<CardStatus> assaults = players[status.getPlayer()].getAssaults();
        if (status.getIndex() + n < assaults.size()) {
            CardStatus rightAssault = assaults.get(status.getIndex() + n);
            if (rightAssault.isAlive())
                return rightAssault;
        }
        return null;
    }

    public void shuffleSelection() { Collections.shuffle(selectionArray, random); }

    /**
     * @param cards = list of cards to select from
     * @param pred = functor that holds the condition of adding selection
     * @return new selection array size
     */
    int makeSelectionArray(List<CardStatus> cards, Predicate<CardStatus> pred) {
        selectionArray.clear();
        for (CardStatus c: cards) {
            if (pred.test(c))
                selectionArray.add(c);
        }
        return selectionArray.size();
    }

}
