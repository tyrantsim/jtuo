package com.github.tyrantsim.jtuo.skills;

public enum Skill {

    // Placeholder for no-skill:
    NO_SKILL("no skill"),

    // Activation (harmful):
    ENFEEBLE("Enfeeble"), JAM("jam"), BESIEGE("mortar"), SIEGE("siege"), STRIKE("strike"), SUNDER(""), WEAKEN(""),

    // Activation (helpful):
    ENHANCE("enhance"), EVOLVE("evolve"), HEAL("heal"), MEND("MEND"), OVERLOAD("OVERLOAD"), PROTECT("PROTECT"), RALLY("RALLY"),
    ENRAGE("ENRAGE"), ENTRAP("ENTRAP"), RUSH("RUSH"),

    // Activation (unclassified/polymorphic):
    MIMIC("mimic"),

    // Defensive:
    ARMORED("ARMORED"), AVENGE("AVENGE"), CORROSIVE("CORROSIVE"), COUNTER("COUNTER"), EVADE("EVADE"), SUBDUE("SUBDUE"),
    PAYBACK("PAYBACK"), REVENGE("REVENGE"), TRIBUTE("TRIBUTE"), REFRESH("REFRESH"), WALL("WALL"), BARRIER("BARRIER"),

    // Combat-Modifier:
    COALITION("COALITION"), LEGION("LEGION"), PIERCE("PIERCE"), RUPTURE("RUPTURE"), SWIPE("SWIPE"), DRAIN("DRAIN"), VENOM("VENOM"),

    // Damage-Dependent:
    BERSERK("BERSERK"), INHIBIT("INHIBIT"), SABOTAGE("SABOTAGE"), LEECH("LEECH"), POISON("POISON"),

    // Triggered:
    ALLEGIANCE("ALLEGIANCE"), FLURRY("FLURRY"), VALOR("VALOR"), STASIS("STASIS"), SUMMON("SUMMON");

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
