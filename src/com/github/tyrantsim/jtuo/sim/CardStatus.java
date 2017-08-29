package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.skills.Skill;

import static com.github.tyrantsim.jtuo.util.Utils.safeMinus;

public class CardStatus {

    private Card card;
    private int index;
    private int player;
    private int delay;
    private int hp;
    private CardStep step;
    private int permHealthBuff;
    private int permAttackBuff;

    private int corrodedRate;
    private int corrodedWeakened;
    private int subdued;
    private int enfeebled;
    private int evaded;
    private int inhibited;
    private int sabotaged;
    private int paybacked;
    private int tributed;
    private int poisoned;
    private int protectedBy;
    private int protectedByStasis;
    private int enranged;
    private int entrapped;

    private int[] primarySkillOffset;
    private int[] evolvedSkillOffset = new int[Skill.values().length];
    private int[] enhancedValue = new int[Skill.values().length];
    private int[] skillCd = new int[Skill.values().length];

    private boolean jammed;
    private boolean overloaded;
    private boolean rushAttempted;
    private boolean sundered;

    void set(Card card) {
        this.card = card;

        index = 0;
        player = 0;
        delay = card.getDelay();
        hp = card.getHealth();
        step = CardStep.NONE;
        permHealthBuff = 0;
        permAttackBuff = 0;
        corrodedRate = 0;
        corrodedWeakened = 0;
        subdued = 0;
        enfeebled = 0;
        evaded = 0;
        inhibited = 0;
        sabotaged = 0;
        paybacked = 0;
        tributed = 0;
        poisoned = 0;
        protectedBy = 0;
        protectedByStasis = 0;
        enranged = 0;
        entrapped = 0;

        jammed = false;
        overloaded = false;
        rushAttempted = false;
        sundered = false;

        primarySkillOffset = new int[Skill.values().length];
        evolvedSkillOffset = new int[Skill.values().length];
        enhancedValue = new int[Skill.values().length];
        skillCd = new int[Skill.values().length];
    }

    final int skill(Skill skillId) {
        // TODO: implement this
        return -1;
    }

    int getMaxHP() {
        return card.getHealth() + safeMinus(permHealthBuff, subdued);
    }

    int addHP(int value) {
        hp = Math.min(hp + value, getMaxHP());
        return hp;
    }

    int extHP(int value) {
        permHealthBuff += value;
        return addHP(value);
    }

    void addPermAttackBuff(int incBy) {
        this.permAttackBuff += incBy;
    }

    void addProtection(int protection) {
        this.protectedBy += protection;
    }

    void reduceDelay() {
        delay--;
    }

    boolean isAlive() {
        return hp > 0;
    }

    boolean isDominion() {
        return getCard().getCategory() == CardCategory.DOMINION_ALPHA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CardStatus that = (CardStatus) o;

        return index == that.index && player == that.player && card.equals(that.card);
    }

    @Override
    public int hashCode() {
        int result = card.hashCode();
        result = 31 * result + index;
        result = 31 * result + player;
        return result;
    }

    // Getters & Setters
    public Card getCard() {
        return card;
    }

    void setIndex(int index) {
        this.index = index;
    }

    void setPlayer(int player) {
        this.player = player;
    }

    int getDelay() {
        return delay;
    }

    public int getCorrodedRate() {
        return corrodedRate;
    }

    public int getCorrodedWeakened() {
        return corrodedWeakened;
    }

    public int getSubdued() {
        return subdued;
    }

    public int getEnfeebled() {
        return enfeebled;
    }

    public int getEvaded() {
        return evaded;
    }

    public int getInhibited() {
        return inhibited;
    }

    public int getSabotaged() {
        return sabotaged;
    }

    public int getPaybacked() {
        return paybacked;
    }

    public int getTributed() {
        return tributed;
    }

    public int getPoisoned() {
        return poisoned;
    }

    public int getProtectedBy() {
        return protectedBy;
    }

    public int getProtectedByStasis() {
        return protectedByStasis;
    }

    public int getEnranged() {
        return enranged;
    }

    public int getEntrapped() {
        return entrapped;
    }

    public boolean isJammed() {
        return jammed;
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public boolean isRushAttempted() {
        return rushAttempted;
    }

    public boolean isSundered() {
        return sundered;
    }

    public int getHP() { return hp; }

}
