package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillUtils;
import com.github.tyrantsim.jtuo.util.Utils;

import java.util.Arrays;

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

    boolean isDominion() {
        return getCard().getCategory() == CardCategory.DOMINION_ALPHA;
    }

    int skillBaseValue(Skill skillId) {
        return card.getSkillValue()[skillId.ordinal() + primarySkillOffset[skillId.ordinal()]]
                + (skillId == Skill.BERSERK ? enranged : 0)
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

    public void setEnranged(int enranged) {
        this.enranged = enranged;
    }

    public int getEnranged() {
        return enranged;
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

    public void setSkillCd(int[] skillCd) { this.skillCd = skillCd; }

    int getSkillCd(Skill skill) {
        return skillCd[skill.ordinal()];
    }

    public int getEnhanced(Skill skillId) {
        return enhancedValue[skillId.ordinal() + primarySkillOffset[skillId.ordinal()]];
    }

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

    public void setEvolvedSkillOffset(int[] evolvedSkillOffset) {
        this.evolvedSkillOffset = evolvedSkillOffset;
    }

    public void setEnhancedValue(int[] enhancedValue) {
        this.enhancedValue = enhancedValue;
    }


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
            copy.setEnranged(enranged);
            copy.setEntrapped(entrapped);

            copy.setPrimarySkillOffset(Utils.cloneArray(primarySkillOffset));
            copy.setEvolvedSkillOffset(Utils.cloneArray(evolvedSkillOffset));
            copy.setEnhancedValue(Utils.cloneArray(enhancedValue));
            copy.setSkillCd(Utils.cloneArray(skillCd));

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
        desc += "[" + m_card->m_name;
        switch (m_card->m_type)
        {
        case assault:
            desc += " att:[[" + to_string(m_card->m_attack) + "(base)";
            if (m_perm_attack_buff)
            {
                desc += "+[" + to_string(m_perm_attack_buff) + "(perm)";
                if (m_subdued) { desc += "-" + to_string(m_subdued) + "(subd)"; }
                desc += "]";
            }
            if (m_corroded_weakened) { desc += "-" + to_string(m_corroded_weakened) + "(corr)"; }
            desc += "]";
            if (m_temp_attack_buff) { desc += (m_temp_attack_buff > 0 ? "+" : "") + to_string(m_temp_attack_buff) + "(temp)"; }
            desc += "]=" + to_string(attack_power());
        case structure:
        case commander:
            desc += " hp:" + to_string(m_hp);
            break;
        case CardType::num_cardtypes:
            assert(false);
            break;
        }
        if (m_delay) { desc += " cd:" + to_string(m_delay); }
        // Status w/o value
        if (m_jammed) { desc += ", jammed"; }
        if (m_overloaded) { desc += ", overloaded"; }
        if (m_sundered) { desc += ", sundered"; }
        // Status w/ value
        if (m_corroded_weakened || m_corroded_rate) { desc += ", corroded " + to_string(m_corroded_weakened) + " (rate: " + to_string(m_corroded_rate) + ")"; }
        if (m_subdued) { desc += ", subdued " + to_string(m_subdued); }
        if (m_enfeebled) { desc += ", enfeebled " + to_string(m_enfeebled); }
        if (m_inhibited) { desc += ", inhibited " + to_string(m_inhibited); }
        if (m_sabotaged) { desc += ", sabotaged " + to_string(m_sabotaged); }
        if (m_poisoned) { desc += ", poisoned " + to_string(m_poisoned); }
        if (m_protected) { desc += ", protected " + to_string(m_protected); }
        if (m_protected_stasis) { desc += ", stasis " + to_string(m_protected_stasis); }
        if (m_enraged) { desc += ", enraged " + to_string(m_enraged); }
        if (m_entrapped) { desc += ", entrapped " + to_string(m_entrapped); }
//        if(m_step != CardStep::none) { desc += ", Step " + to_string(static_cast<int>(m_step)); }
        Skill::Trigger s_triggers[] = { Skill::Trigger::play, Skill::Trigger::activate, Skill::Trigger::death };
        for (const Skill::Trigger& trig: s_triggers)
        {
            std::vector<SkillSpec> card_skills(
                (trig == Skill::Trigger::play) ? m_card->m_skills_on_play :
                (trig == Skill::Trigger::activate) ? m_card->m_skills :
                (trig == Skill::Trigger::death) ? m_card->m_skills_on_death :
                std::vector<SkillSpec>());

            // emulate Berserk/Counter by status Enraged/Entrapped unless such skills exist (only for normal skill triggering)
            if (trig == Skill::Trigger::activate)
            {
                if (m_enraged && !std::count_if(card_skills.begin(), card_skills.end(), [](const SkillSpec ss) { return (ss.id == Skill::berserk); }))
                {
                    SkillSpec ss{Skill::berserk, m_enraged, allfactions, 0, 0, Skill::no_skill, Skill::no_skill, false, 0,};
                    card_skills.emplace_back(ss);
                }
                if (m_entrapped && !std::count_if(card_skills.begin(), card_skills.end(), [](const SkillSpec ss) { return (ss.id == Skill::counter); }))
                {
                    SkillSpec ss{Skill::counter, m_entrapped, allfactions, 0, 0, Skill::no_skill, Skill::no_skill, false, 0,};
                    card_skills.emplace_back(ss);
                }
            }
            for (const auto& ss : card_skills)
            {
                std::string skill_desc;
                if (m_evolved_skill_offset[ss.id]) { skill_desc += "->" + skill_names[ss.id + m_evolved_skill_offset[ss.id]]; }
                if (m_enhanced_value[ss.id]) { skill_desc += " +" + to_string(m_enhanced_value[ss.id]); }
                if (!skill_desc.empty())
                {
                    desc += ", " + (
                        (trig == Skill::Trigger::play) ? "(On Play)" :
                        (trig == Skill::Trigger::death) ? "(On Death)" :
                        std::string("")) + skill_names[ss.id] + skill_desc;
                }
            }
        }
        return desc + "]";
    }


}