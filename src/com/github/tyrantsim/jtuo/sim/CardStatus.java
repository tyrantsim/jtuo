package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.skills.Skill;

public class CardStatus {

    Card card;
    int index;
    int player;
    int delay;
    int hp;
    CardStep step;
    int permHealthBuff;
    int permAttackBuff;

    int corrodedRate;
    int corrodedWeakened;
    int subdued;
    int enfeebled;
    int evaded;
    int inhibited;
    int sabotaged;
    int paybacked;
    int tributed;
    int poisoned;
    int protectedBy;
    int protectedByStasis;
    int enranged;
    int entrapped;

    int[] primarySkillOffset;
    int[] evolvedSkillOffset = new int[Skill.values().length];
    int[] enhancedValue = new int[Skill.values().length];
    int[] skillCd = new int[Skill.values().length];

    boolean jammed;
    boolean overloaded;
    boolean rushAttempted;
    boolean sundered;

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

    public boolean isAlive() {
        return hp > 0;
    }

    // Getters & Setters
    public Card getCard() {
        return card;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

}
