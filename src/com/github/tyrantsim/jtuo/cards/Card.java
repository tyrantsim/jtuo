package com.github.tyrantsim.jtuo.cards;

import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;

import java.util.ArrayList;
import java.util.HashMap;

public class Card {

    int id;
    int baseId; // The id of the original card if a card is unique and alt/upgraded. The own id of the card otherwise.
    CardType type = CardType.ASSAULT;
    CardCategory category = CardCategory.NORMAL;

    String name = "";
    int level;
    Faction faction = Faction.IMPERIAL;
    int rarirty = 1;
    int fusionLevel;
    int set;

    int attack;
    int health;
    int delay;

    ArrayList<SkillSpec> skills;
    ArrayList<SkillSpec> skillsOnPlay;
    ArrayList<SkillSpec> skillsOnDeath;
    int[] skillValue = new int[Skill.values().length];
    SkillTrigger[] skillTrigger = new SkillTrigger[Skill.values().length];

    Card topLevelCard = this; // [TU] corresponding full-level card
    int recipeCost;
    HashMap<Card, Integer> recipeCards;
    HashMap<Card, Integer> usedForCards;

    void addSkill(SkillTrigger trigger, Skill id, int x, Faction y, int n, int c, Skill s, Skill s2, boolean all, int cardId) {
        SkillSpec spec = new SkillSpec();
        spec.setId(id);
        spec.setX(x);
        spec.setY(y);
        spec.setN(n);
        spec.setS(s);
        spec.setS2(s2);
        spec.setAll(all);
        spec.setCardId(cardId);

        // remove previous copy of such skill.id
        skills.removeIf(ss -> ss.getId() == id);
        skillsOnPlay.removeIf(ss -> ss.getId() == id);
        skillsOnDeath.removeIf(ss -> ss.getId() == id);

        // add a new one
        switch(trigger) {
            case ACTIVATE:
                skills.add(spec);
                break;
            case PLAY:
                skillsOnPlay.add(spec);
                break;
            case DEATH:
                skillsOnDeath.add(spec);
                break;
            default:
                throw new AssertionError("No storage for skill with trigger " + trigger);
        }

        // setup value
        if(x != 0) {
            skillValue[id.ordinal()] = x;
        } else if(n != 0) {
            skillValue[id.ordinal()] = n;
        } else if(cardId != 0) {
            skillValue[id.ordinal()] = cardId;
        } else {
            skillValue[id.ordinal()] = 1;
        }

        skillTrigger[id.ordinal()] = trigger;
    }

    void addSkill(SkillTrigger trigger, Skill id, int x, Faction y, int n, int c, Skill s) {
        addSkill(trigger, id, x, y, n, c, s, Skill.NO_SKILL, false, 0);
    }

    boolean isTopLevelCard() {
        return equals(topLevelCard);
    }

    boolean isLowLevelCard() {
        return id == baseId;
    }

    Card upgraded() {
        return isTopLevelCard() ? this : usedForCards.keySet().iterator().next();
    }

    Card downgraded() {
        return isLowLevelCard() ? this : recipeCards.keySet().iterator().next();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        return id == card.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

}
