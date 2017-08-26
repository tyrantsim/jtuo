package com.github.tyrantsim.jtuo.skills;

public class SkillUtils {

    public static boolean isActivationHarmfulSkill(final Skill skillId) {
        switch (skillId) {
            case ENFEEBLE:
            case JAM:
            case MORTAR:
            case SIEGE:
            case STRIKE:
            case SUNDER:
            case WEAKEN:
                return true;
            default:
                return false;
        }
    }

    public static boolean isActivationHostileSkill(final Skill skillId) {
        switch (skillId) {
            case MIMIC:
                return true;
            default:
                return isActivationHarmfulSkill(skillId);
        }
    }

    public static boolean isActivationHelpfulSkill(final Skill skillId) {
        switch (skillId) {
            case ENHANCE:
            case EVOLVE:
            case HEAL:
            case MEND:
            case OVERLOAD:
            case PROTECT:
            case RALLY:
            case ENRAGE:
            case ENTRAP:
            case RUSH:
                return true;
            default:
                return false;
        }
    }

    public static boolean isActivationAlliedSkill(final Skill skillId) {
        return isActivationHelpfulSkill(skillId);
    }

    public static boolean isActivationSkill(final Skill skillId) {
        return isActivationHostileSkill(skillId)
                || isActivationAlliedSkill(skillId);
    }

    public static boolean isActivationSkillWithX(final Skill skillId) {
        switch (skillId) {
            case ENFEEBLE:
            case MORTAR:
            case SIEGE:
            case STRIKE:
            case SUNDER:
            case WEAKEN:
            case ENHANCE:
            case MIMIC:
            case HEAL:
            case MEND:
            case PROTECT:
            case RALLY:
            case ENRAGE:
            case ENTRAP:
            case RUSH:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDefensiveSkill(final Skill skillId) {
        switch (skillId) {
            case ARMORED:
            case AVENGE:
            case CORROSIVE:
            case COUNTER:
            case EVADE:
            case SUBDUE:
            case PAYBACK:
            case REVENGE:
            case TRIBUTE:
            case REFRESH:
            case WALL:
                return true;
            default:
                return false;
        }
    }

    public static boolean isCombatModifierSkill(final Skill skillId) {
        switch (skillId) {
            case LEGION:
            case PIERCE:
            case RUPTURE:
            case SWIPE:
            case DRAIN:
            case VENOM:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDamageDependantSkill(final Skill skillId) {
        switch (skillId) {
            case BERSERK:
            case INHIBIT:
            case SABOTAGE:
            case LEECH:
            case POISON:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTriggeredSkill(final Skill skillId) {
        switch (skillId) {
            case ALLEGIANCE:
            case FLURRY:
            case VALOR:
            case STASIS:
            case SUMMON:
                return true;
            default:
                return false;
        }
    }



}