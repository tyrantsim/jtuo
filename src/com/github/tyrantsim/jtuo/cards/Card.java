package com.github.tyrantsim.jtuo.cards;

import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;

import java.util.ArrayList;
import java.util.HashMap;

public class Card implements Cloneable {

    int id;
    int baseId; // The id of the original card if a card is unique and
                // alt/upgraded. The own id of the card otherwise.
    CardType type = CardType.ASSAULT;
    CardCategory category = CardCategory.NORMAL;

    String name = "";
    int level = 1;
    Faction faction = Faction.IMPERIAL;
    int rarity = 1;
    int fusionLevel;
    int set;

    private int attack;
    private int health;
    private int delay;

    ArrayList<SkillSpec> skills = new ArrayList<>();
    ArrayList<SkillSpec> skillsOnPlay = new ArrayList<>();
    ArrayList<SkillSpec> skillsOnDeath = new ArrayList<>();
    int[] skillValue = new int[Skill.values().length];
    SkillTrigger[] skillTrigger = new SkillTrigger[Skill.values().length];

    Card topLevelCard = this; // [TU] corresponding full-level card
    int recipeCost;
    HashMap<Card, Integer> recipeCards = new HashMap<>();
    HashMap<Card, Integer> usedForCards = new HashMap<>();

    public Card() {
        super();
    }
    
    public Card(int id, String name) {
        super();
        this.id = id;
        this.baseId = id;
        this.name = name;
    }

    public Card(int id, int baseId, CardType type, CardCategory category, String name, int level, Faction faction, int rarity, int fusionLevel, int set, int attack, int health, int delay, ArrayList<SkillSpec> skills,
            ArrayList<SkillSpec> skillsOnPlay, ArrayList<SkillSpec> skillsOnDeath, int[] skillValue, SkillTrigger[] skillTrigger, Card topLevelCard, int recipeCost, HashMap<Card, Integer> recipeCards, HashMap<Card, Integer> usedForCards) {
        super();
        this.id = id;
        this.baseId = baseId;
        this.type = type;
        this.category = category;
        this.name = name;
        this.level = level;
        this.faction = faction;
        this.rarity = rarity;
        this.fusionLevel = fusionLevel;
        this.set = set;
        this.attack = attack;
        this.health = health;
        this.delay = delay;
        this.skills = skills;
        this.skillsOnPlay = skillsOnPlay;
        this.skillsOnDeath = skillsOnDeath;
        this.skillValue = skillValue;
        this.skillTrigger = skillTrigger;
        this.topLevelCard = topLevelCard;
        this.recipeCost = recipeCost;
        this.recipeCards = recipeCards;
        this.usedForCards = usedForCards;
    }

    void set(Card card) {
        this.id = card.id;
        this.baseId = card.baseId;
        this.type = card.type;
        this.category = card.category;
        this.name = card.name;
        this.level = card.level;
        this.faction = card.faction;
        this.rarity = card.rarity;
        this.fusionLevel = card.fusionLevel;
        this.set = card.set;
        this.attack = card.attack;
        this.health = card.health;
        this.delay = card.delay;
        this.skills = card.skills;
        this.skillsOnPlay = card.skillsOnPlay;
        this.skillsOnDeath = card.skillsOnDeath;
        this.skillValue = card.skillValue;
        this.skillTrigger = card.skillTrigger;
        this.topLevelCard = card.topLevelCard;
        this.recipeCost = card.recipeCost;
        this.recipeCards = card.recipeCards;
        this.usedForCards = card.usedForCards;
    }

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
        switch (trigger) {
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
        if (x != 0) {
            skillValue[id.ordinal()] = x;
        } else if (n != 0) {
            skillValue[id.ordinal()] = n;
        } else if (cardId != 0) {
            skillValue[id.ordinal()] = cardId;
        } else {
            skillValue[id.ordinal()] = 1;
        }

        skillTrigger[id.ordinal()] = trigger;
    }

    void addSkill(SkillTrigger trigger, Skill id, int x, Faction y, int n, int c, Skill s) {
        addSkill(trigger, id, x, y, n, c, s, Skill.NO_SKILL, false, 0);
    }

    public boolean isTopLevelCard() {
        return equals(topLevelCard);
    }

    boolean isLowLevelCard() {
        return id == baseId;
    }

    public Card upgraded() {
        return isTopLevelCard() ? this : usedForCards.keySet().iterator().next();
    }

    public Card downgraded() {
        return isLowLevelCard() ? this : recipeCards.keySet().iterator().next();
    }

    public void upgradeSelf() {
        set(upgraded());
    }

    public void addUsedForCard(Card usedFor, int amount) {
        this.usedForCards.put(usedFor, amount);
    }

    @Override
    public Card clone() {
        try {
            Card copy = (Card) super.clone();
            copy.skills = new ArrayList<>(this.skills);
            copy.skillsOnPlay = new ArrayList<>(this.skillsOnPlay);
            copy.skillsOnDeath = new ArrayList<>(this.skillsOnDeath);
            copy.recipeCards = new HashMap<>(this.recipeCards);
            copy.usedForCards = new HashMap<>(this.usedForCards);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Card object cloning is not supported");
        }
    }

    @Override
    public String toString() {
        return baseId + "/" + id + ": " + type.name() + " " + faction.name() + " " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Card card = (Card) o;

        return id == card.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    // Getters & Setters
    public Faction getFaction() {
        return faction;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBaseId() {
        return baseId;
    }

    public void setBaseId(int baseId) {
        this.baseId = baseId;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public CardCategory getCategory() {
        return category;
    }

    public void setCategory(CardCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRarity() {
        return rarity;
    }

    public void setRarity(int rarity) {
        this.rarity = rarity;
    }

    public int getFusionLevel() {
        return fusionLevel;
    }

    public void setFusionLevel(int fusionLevel) {
        this.fusionLevel = fusionLevel;
    }

    public int getSet() {
        return set;
    }

    public void setSet(int set) {
        this.set = set;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public ArrayList<SkillSpec> getSkills() {
        return skills;
    }

    public void setSkills(ArrayList<SkillSpec> skills) {
        this.skills = skills;
    }

    public ArrayList<SkillSpec> getSkillsOnPlay() {
        return skillsOnPlay;
    }

    public void setSkillsOnPlay(ArrayList<SkillSpec> skillsOnPlay) {
        this.skillsOnPlay = skillsOnPlay;
    }

    public ArrayList<SkillSpec> getSkillsOnDeath() {
        return skillsOnDeath;
    }

    public void setSkillsOnDeath(ArrayList<SkillSpec> skillsOnDeath) {
        this.skillsOnDeath = skillsOnDeath;
    }

    public int[] getSkillValue() {
        return skillValue;
    }

    public void setSkillValue(int[] skillValue) {
        this.skillValue = skillValue;
    }

    public SkillTrigger[] getSkillTrigger() {
        return skillTrigger;
    }

    public void setSkillTrigger(SkillTrigger[] skillTrigger) {
        this.skillTrigger = skillTrigger;
    }

    public Card getTopLevelCard() {
        return topLevelCard;
    }

    public void setTopLevelCard(Card topLevelCard) {
        this.topLevelCard = topLevelCard;
    }

    public int getRecipeCost() {
        return recipeCost;
    }

    public void setRecipeCost(int recipeCost) {
        this.recipeCost = recipeCost;
    }

    public HashMap<Card, Integer> getRecipeCards() {
        return recipeCards;
    }

    public void setRecipeCards(HashMap<Card, Integer> recipeCards) {
        this.recipeCards = recipeCards;
    }

    public HashMap<Card, Integer> getUsedForCards() {
        return usedForCards;
    }

    public void setUsedForCards(HashMap<Card, Integer> usedForCards) {
        this.usedForCards = usedForCards;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }



}
