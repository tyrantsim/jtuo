package com.github.tyrantsim.jtuo.skills;

public enum Skill {

    // Placeholder for no-skill:
    NO_SKILL("No Skill"),

    // Activation (harmful):
    ENFEEBLE("Enfeeble"), JAM("Jam"), BESIEGE("Mortar"), SIEGE("Siege"), STRIKE("Strike"), SUNDER("Sunder"), WEAKEN("Weaken"),

    // Activation (helpful):
    ENHANCE("Enhance"), EVOLVE("Evolve"), HEAL("Heal"), MEND("Mend"), OVERLOAD("Overload"), PROTECT("Protect"), RALLY("Rally"),
    ENRAGE("Enrage"), ENTRAP("Entrap"), RUSH("Rush"),

    // Activation (unclassified/polymorphic):
    MIMIC("Mimic"),

    // Defensive:
    ARMORED("Armor"), AVENGE("Avenge"), CORROSIVE("Corrosive"), COUNTER("Counter"), EVADE("Evade"), SUBDUE("Subdue"),
    PAYBACK("Payback"), REVENGE("Revenge"), TRIBUTE("Tribute"), REFRESH("Refresh"), WALL("Wall"), BARRIER("Barrier"),

    // Combat-Modifier:
    COALITION("Coalition"), LEGION("Legion"), PIERCE("Pierce"), RUPTURE("Rupture"), SWIPE("Swipe"), DRAIN("Drain"), VENOM("Venom"),

    // Damage-Dependent:
    BERSERK("Berserk"), INHIBIT("Inhibit"), SABOTAGE("Sabotage"), LEECH("Leech"), POISON("Poison"),

    // Triggered:
    ALLEGIANCE("Allegiance"), FLURRY("Flurry"), VALOR("Valor"), STASIS("Stasis"), SUMMON("Summon");

    private String description;
    
    Skill(String description) {
        this.setDescription(description);
    }
    public String getDescription() {
        return description;
    }
    
    private void setDescription(String description) {
        this.description = description;
    }
}
