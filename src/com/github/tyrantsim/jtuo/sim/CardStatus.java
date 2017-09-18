package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.Faction;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
import com.github.tyrantsim.jtuo.skills.SkillUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.tyrantsim.jtuo.util.Utils.safeMinus;

public class CardStatus implements Cloneable {

    private Card card;
    private int index;
    private int player;
    private int delay;
    private int hp;
    private CardStep step;
    private int permHealthBuff;
    private int permAttackBuff;
    private int tempAttackBuff;

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
    private int enraged;
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
        enraged = 0;
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

    int skill(Skill skillId) {
        return SkillUtils.isActivationSkillWithX(skillId)
                ? safeMinus(skillBaseValue(skillId), sabotaged)
                : skillBaseValue(skillId);
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

    int getAttackPower() {
        return safeMinus(card.getAttack() + safeMinus(permAttackBuff, subdued), corrodedWeakened) + tempAttackBuff;
    }

    int protectedValue() {
        return protectedBy + protectedByStasis;
    }

    void addPermAttackBuff(int incBy) {
        this.permAttackBuff += incBy;
    }

    void addTempAttackBuff(int incBy) { this.tempAttackBuff += incBy; }

    void addProtection(int protection) {
        this.protectedBy += protection;
    }

    void reduceDelay() {
        delay--;
    }

    boolean isAlive() {
        return hp > 0;
    }

    boolean canAct() {
        return isAlive() && !isJammed();
    }

    boolean isActive() {
        return canAct() && getDelay() == 0;
    }

    boolean canBeHealed() { return isAlive() && hp < getMaxHP(); }

    boolean isDominion() {
        return getCard().getCategory() == CardCategory.DOMINION_ALPHA;
    }

    int skillBaseValue(Skill skillId) {
        return card.getSkillValue()[skillId.ordinal() + primarySkillOffset[skillId.ordinal()]]
                + (skillId == Skill.BERSERK ? enraged : 0)
                + (skillId == Skill.COUNTER ? entrapped : 0);
    }

    boolean hasSkill(Skill skillId) {
        return skillBaseValue(skillId) != 0;
    }

    int getEvolvedSkillOffset(Skill skillId) {
        return evolvedSkillOffset[skillId.ordinal()];
    }

    void clearPrimarySkillOffset() {
        Arrays.fill(primarySkillOffset, 0);
    }

    void clearEvolvedSkillOffset() {
        Arrays.fill(evolvedSkillOffset, 0);
    }

    void clearEnhancedValue() {
        Arrays.fill(enhancedValue, 0);
    }

    public boolean isGilian() {
        return (card.getId() >= 25054 && card.getId() <= 25063) // Gilian Commander
                || (card.getId() >= 38348 && card.getId() <= 38388); // Gilian assaults plus the Gil's Shard
    }

    public boolean isAliveGilian() {
        return isAlive() && isGilian();
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

    int getIndex() {
        return index;
    }

    void setPlayer(int player) {
        this.player = player;
    }

    public int getPlayer() {
        return player;
    }

    int getDelay() {
        return delay;
    }

    void setStep(CardStep step) {
        this.step = step;
    }

    int getPermAttackBuff() {
        return permAttackBuff;
    }

    void setTempAttackBuff(int tempAttackBuff) {
        this.tempAttackBuff = tempAttackBuff;
    }

    public int getCorrodedRate() {
        return corrodedRate;
    }

    public void setCorrodedWeakened(int corrodedWeakened) {
        this.corrodedWeakened = corrodedWeakened;
    }

    public int getCorrodedWeakened() {
        return corrodedWeakened;
    }

    public int getSubdued() {
        return subdued;
    }

    public void setEnfeebled(int enfeebled) {
        this.enfeebled = enfeebled;
    }

    public int getEnfeebled() {
        return enfeebled;
    }

    public void setEvaded(int evaded) {
        this.evaded = evaded;
    }

    public int getEvaded() {
        return evaded;
    }

    public void setInhibited(int inhibited) {
        this.inhibited = inhibited;
    }

    public int getInhibited() {
        return inhibited;
    }

    public void setSabotaged(int sabotaged) {
        this.sabotaged = sabotaged;
    }

    public int getSabotaged() {
        return sabotaged;
    }

    public void setPaybacked(int paybacked) {
        this.paybacked = paybacked;
    }

    public int getPaybacked() {
        return paybacked;
    }

    public void setTributed(int tributed) {
        this.tributed = tributed;
    }

    public int getTributed() {
        return tributed;
    }

    public int getPoisoned() {
        return poisoned;
    }

    public void setProtectedBy(int protectedBy) {
        this.protectedBy = protectedBy;
    }

    public int getProtectedBy() {
        return protectedBy;
    }

    public void setProtectedByStasis(int protectedByStasis) {
        this.protectedByStasis = protectedByStasis;
    }

    public int getProtectedByStasis() {
        return protectedByStasis;
    }

    public int[] getPrimarySkillOffset() { return primarySkillOffset; }

    public void setEnraged(int enranged) {
        this.enraged = enranged;
    }

    public int getEnraged() {
        return enraged;
    }

    public void setEntrapped(int entrapped) {
        this.entrapped = entrapped;
    }

    public int getEntrapped() {
        return entrapped;
    }

    public void setJammed(boolean jammed) {
        this.jammed = jammed;
    }

    public boolean isJammed() {
        return jammed;
    }

    public void setOverloaded(boolean overloaded) {
        this.overloaded = overloaded;
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public boolean isRushAttempted() {
        return rushAttempted;
    }

    public void setSundered(boolean sundered) {
        this.sundered = sundered;
    }

    public boolean isSundered() {
        return sundered;
    }

    public void setHP(int hp) {
        this.hp = hp;
    }

    public int getHP() {
        return hp;
    }

    public void setSkillCd(Skill skill, int cd) {
        skillCd[skill.ordinal()] = cd;
    }

    public void cooldownSkillCd(Skill skill) { skillCd[skill.ordinal()]--; }

    public void setSkillCd(int[] skillCd) { this.skillCd = skillCd; }

    public int getSkillCd(Skill skill) {
        return skillCd[skill.ordinal()];
    }

    public int getEnhanced(Skill skillId) {
        return enhancedValue[skillId.ordinal() + primarySkillOffset[skillId.ordinal()]];
    }

    public boolean hasAttacked() { return step == CardStep.ATTACKED; }

    public CardStep getStep() { return step; }

    public boolean isActiveNextTurn() { return canAct() && delay <= 1; }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setPermHealthBuff(int permHealthBuff) {
        this.permHealthBuff = permHealthBuff;
    }

    public void setPermAttackBuff(int permAttackBuff) {
        this.permAttackBuff = permAttackBuff;
    }

    public void setCorrodedRate(int corrodedRate) {
        this.corrodedRate = corrodedRate;
    }

    public void setSubdued(int subdued) {
        this.subdued = subdued;
    }

    public void setPoisoned(int poisoned) {
        this.poisoned = poisoned;
    }

    public void setPrimarySkillOffset(int[] primarySkillOffset) {
        this.primarySkillOffset = primarySkillOffset;
    }

    public void setPrimarySkillOffset(int skillPosition, int offset) { primarySkillOffset[skillPosition] = offset; }

    public void setEvolvedSkillOffset(int[] evolvedSkillOffset) {
        this.evolvedSkillOffset = evolvedSkillOffset;
    }

    public void setEvolvedSkillOffset(int skillPosition, int offset) { evolvedSkillOffset[skillPosition] = offset; }

    public void setEnhancedValue(int[] enhancedValue) {
        this.enhancedValue = enhancedValue;
    }

    public void addEnhancedValue(int skillPosition, int inc) { enhancedValue[skillPosition] += inc; }

    public void setRushAttempted(boolean rushAttempted) {
        this.rushAttempted = rushAttempted;
    }

    public CardStatus clone() {
        try {
            CardStatus copy = (CardStatus) super.clone();
            copy.set(card);
            copy.setIndex(index);
            copy.setPlayer(player);
            copy.setDelay(delay);
            copy.setHP(hp);
            copy.setStep(step);
            copy.setPermHealthBuff(permHealthBuff);
            copy.setPermAttackBuff(permAttackBuff);
            copy.setTempAttackBuff(tempAttackBuff);
            copy.setCorrodedRate(corrodedRate);
            copy.setCorrodedWeakened(corrodedWeakened);
            copy.setSubdued(subdued);
            copy.setEnfeebled(enfeebled);
            copy.setEvaded(evaded);
            copy.setInhibited(inhibited);
            copy.setSabotaged(sabotaged);
            copy.setPaybacked(paybacked);
            copy.setTributed(tributed);
            copy.setPoisoned(poisoned);
            copy.setProtectedBy(protectedBy);
            copy.setProtectedByStasis(protectedByStasis);
            copy.setEnraged(enraged);
            copy.setEntrapped(entrapped);

            copy.setPrimarySkillOffset(primarySkillOffset.clone());
            copy.setEvolvedSkillOffset(evolvedSkillOffset.clone());
            copy.setEnhancedValue(enhancedValue.clone());
            copy.setSkillCd(skillCd.clone());

            copy.setJammed(jammed);
            copy.setOverloaded(overloaded);
            copy.setRushAttempted(rushAttempted);
            copy.setSundered(sundered);

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
    
    public String description() {
        String desc = "P" + player + " ";
        switch(card.getType()) {
        case COMMANDER: desc += "Commander "; break;
        case ASSAULT: desc += "Assault " + index + " "; break;
        case STRUCTURE: desc += "Structure " + index + " "; break;
        //case cardtypes: assert(false); break;
        }
        desc += "[" + getCard().getName();
        switch (getCard().getType())
        {
        case ASSAULT:
            desc += " att:[[" + getCard().getAttack() + "(base)";
            if (getPermAttackBuff() > 0){
                desc += "+[" + getPermAttackBuff() + "(perm)";
                if (getSubdued() > 0) { desc += "-" + getSubdued() + "(subd)"; }
                desc += "]";
            }
            if (getCorrodedWeakened() > 0) { desc += "-" + getCorrodedWeakened() + "(corr)"; }
            desc += "]";
            if (tempAttackBuff != 0) { desc += (tempAttackBuff > 0 ? "+" : "") + tempAttackBuff + "(temp)"; }
            desc += "]=" + getAttackPower();
        case STRUCTURE:
        case COMMANDER:
            desc += " hp:" + getHP();
            break;
        default:
            assert(false);
            break;
        }
        if (getDelay() > -1) { desc += " cd:" + getDelay(); }
        // Status w/o value
        if (jammed) { desc += ", jammed"; }
        if (overloaded) { desc += ", overloaded"; }
        if (sundered) { desc += ", sundered"; }
        // Status w/ value
        if (corrodedWeakened > 0 || corrodedRate >0) { desc += ", corroded " + corrodedWeakened + " (rate: " + corrodedRate + ")"; }
        if (subdued > 0) { desc += ", subdued " + subdued; }
        if (enfeebled > 0) { desc += ", enfeebled " + enfeebled; }
        if (inhibited> 0) { desc += ", inhibited " + inhibited; }
        if (sabotaged> 0) { desc += ", sabotaged " + sabotaged; }
        if (poisoned> 0) { desc += ", poisoned " + poisoned; }
        if (getProtectedBy() > 0) { desc += ", protected " + protectedBy; }
        if (protectedByStasis >0) { desc += ", stasis " + protectedByStasis; }
        if (enraged >0) { desc += ", enraged " + enraged; }
        if (entrapped>0) { desc += ", entrapped " + entrapped; }
//        if(step != CardStep::none) { desc += ", Step " + static_cast<int>(step); }
        SkillTrigger s_triggers[] = { SkillTrigger.PLAY, SkillTrigger.ACTIVATE, SkillTrigger.DEATH };
        for (SkillTrigger trig: s_triggers) {
            List<SkillSpec> card_skills = 
                (trig == SkillTrigger.PLAY) ? getCard().getSkillsOnPlay() :
                (trig == SkillTrigger.ACTIVATE) ? getCard().getSkills() :
                (trig == SkillTrigger.DEATH) ? getCard().getSkillsOnDeath() :
                new ArrayList<SkillSpec>();

            // emulate Berserk/Counter by status Enraged/Entrapped unless such skills exist (only for normal skill triggering)
            if (trig == SkillTrigger.ACTIVATE)
            {
                if (enraged > 0 && !card_skills.contains(Skill.BERSERK)) {
                    SkillSpec ss = new SkillSpec(Skill.BERSERK, enraged, Faction.ALL_FACTIONS, 0, 0, Skill.NO_SKILL, Skill.NO_SKILL, false, 0, trig);
                    card_skills.add(ss);
                }
                if (entrapped > 0 && !card_skills.contains(Skill.COUNTER)) {
                    SkillSpec ss = new SkillSpec(Skill.COUNTER, entrapped, Faction.ALL_FACTIONS, 0, 0, Skill.NO_SKILL, Skill.NO_SKILL, false, 0, trig);
                    card_skills.add(ss);
                }
            }
            for (SkillSpec ss : card_skills) {
                String skill_desc = "";
                // TODO: replace c++ code
                // if (evolved_skill_offset[ss.id]) { skill_desc += "->" +
                // skill_names[ss.id + evolved_skill_offset[ss.id]]; }
                // if (enhanced_value[ss.id]) { skill_desc += " +" +
                // enhanced_value[ss.id]; }
                if (!skill_desc.isEmpty()) {
                    desc += ", " + ( //
                    (trig == SkillTrigger.PLAY) ? "(On Play)" : //
                            (trig == SkillTrigger.ATTACKED) ? "(On Attack)" : //
                                    (trig == SkillTrigger.DEATH) ? "(On Death)" : "" + //
                                            ss.getId().getDescription() + skill_desc); //
                }
            }
        }
        return desc + "]";
    }


}