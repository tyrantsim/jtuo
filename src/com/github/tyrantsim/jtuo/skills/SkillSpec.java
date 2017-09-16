package com.github.tyrantsim.jtuo.skills;

import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.cards.Faction;

public class SkillSpec implements Cloneable {

    public SkillSpec(Skill id, float x, Faction y, int n, int c, Skill s, Skill s2, boolean all, int cardId, SkillTrigger trigger) {
        super();
        this.id = id;
        this.x = x;
        this.y = y;
        this.n = n;
        this.c = c;
        this.s = s;
        this.s2 = s2;
        this.all = all;
        this.cardId = cardId;
        this.trigger = trigger;
    }

    public SkillSpec() {
    }

    Skill id;
    float x;
    Faction y;
    int n;
    int c;
    Skill s;
    Skill s2;
    boolean all;
    int cardId;
    SkillTrigger trigger;


    public Skill getId() {
        return id;
    }

    public void setId(Skill id) {
        this.id = id;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public Faction getY() {
        return y;
    }

    public void setY(Faction y) {
        this.y = y;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public Skill getS() {
        return s;
    }

    public void setS(Skill s) {
        this.s = s;
    }

    public Skill getS2() {
        return s2;
    }

    public void setS2(Skill s2) {
        this.s2 = s2;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public SkillTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(SkillTrigger trigger) {
        this.trigger = trigger;
    }

    public SkillSpec clone() {
        try {
            SkillSpec copy = (SkillSpec) super.clone();
            copy.setId(id);
            copy.setX(x);
            copy.setY(y);
            copy.setN(n);
            copy.setC(c);
            copy.setS(s);
            copy.setS2(s2);
            copy.setAll(all);
            copy.setCardId(cardId);
            copy.setTrigger(trigger);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String description() {

        return ((trigger == SkillTrigger.PLAY) ? "(On Play)"
                : (trigger == SkillTrigger.ATTACK) ? "(On Attacked)"
                : (trigger == SkillTrigger.DEATH) ? "(On Death)" : "")
                + id.toString()
                + (cardId == 0 ? "" : " " + Cards.getCardNameByIdSafe(cardId) + " id[" + cardId + "]")
                + (all ? " all" : n == 0 ? "" : " " + n)
                + (y == Faction.ALL_FACTIONS ? "" : " " + y.toString())
                + (s == Skill.NO_SKILL ? "" : " " + s.toString())
                + (s2 == Skill.NO_SKILL ? "" : " " + s2.toString())
                + (x == 0 ? "" : " " + x)
                + (c == 0 ? "" : " every " + c);
    }
    
    @Override
    public String toString() {
        return id + " " + trigger + "-" + cardId;
    }

}
