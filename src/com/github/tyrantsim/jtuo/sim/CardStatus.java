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

    int[] primarySkillOffset = new int[Skill.values().length];
    int[] evolvedSkillOffset = new int[Skill.values().length];
    int[] enhancedValue = new int[Skill.values().length];
    int[] skillCd = new int[Skill.values().length];

    boolean jammed;
    boolean overloaded;
    boolean rushAttempted;
    boolean sundered;

    void set(Card card) {
        // TODO: implement this
    }

    final int skill(Skill skillId) {
        // TODO: implement this
        return -1;
    }

    // Getters & Setters
    public Card getCard() {
        return card;
    }

}
