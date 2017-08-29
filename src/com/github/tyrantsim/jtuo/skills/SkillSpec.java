package com.github.tyrantsim.jtuo.skills;

import com.github.tyrantsim.jtuo.cards.Faction;

public class SkillSpec implements Cloneable {

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
            // TODO: implement this
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
    
    @Override
    public String toString() {
        return id + " " + trigger + "-" + cardId;
    }
}
