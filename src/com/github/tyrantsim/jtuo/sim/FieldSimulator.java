package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.Deque;
import java.util.List;

public class FieldSimulator {

    public static int turnLimit = Integer.MAX_VALUE;

    public static Results play(Field field) {
        field.getPlayer(0).getCommander().setPlayer(0);
        field.getPlayer(1).getCommander().setPlayer(1);
        field.setTapi(field.getGameMode() == GameMode.SURGE ? 1 : 0);
        field.setTipi(field.getGameMode() == GameMode.SURGE ? 0 : 1);
        field.setTap(field.getPlayer(field.getTapi()));
        field.setTip(field.getPlayer(field.getTipi()));
        field.setEnd(false);

        playDominionAndFortresses(field);

        while (field.getTurn() <= turnLimit && !field.isEnd()) {
            field.setCurrentPhase(FieldPhase.PLAYCARD_PHASE);

            // Initialize stuff, remove dead cards
            // reduce timers & perform triggered skills (like Summon)
            field.prepareAction();
            turnStartPhase(field); // summon may postpone skills tp be resolved
            resolveSkill(field); // resolve postponed skills recursively
            field.finalizeAction();

            boolean bgeMegamorphosis = field.getBGEffects(field.getTapi())[PassiveBGE.MEGAMORPHOSIS.ordinal()] != null;

            // Play a card
            Card playedCard = field.getTap().getDeck().next();
            if (playedCard != null) {
                simulatePlayCardPhase(field, playedCard);
            }
            if (field.isEnd()) {
                break;
            }

            // Evaluate Passive BGE Heroism skills
            if (field.getBGEffects(field.getTapi())[PassiveBGE.HEROISM.ordinal()] != null) {
                evaluatePassiveBGEHeroismSkills(field);
            }

            // Evaluate activation BGE skills
            for (SkillSpec bgSkill : field.getBGSkills(field.getTapi())) {
                field.prepareAction();
                // TODO: Maybe new instance of field.tap.getCommander is required
                field.skillQueue.addLast(new Pair<CardStatus, SkillSpec>(field.tap.getCommander(), bgSkill));
                resolveSkill(field);
                field.finalizeAction();
            }

            if (field.isEnd()) {
                break;
            }

            evaluateCommander(field);
            if (field.isEnd()) {
                break;
            }

            evaluateStructures(field);
            if (field.isEnd()) {
                break;
            }

            evaluateAssaults(field);

            field.setCurrentPhase(FieldPhase.END_PHASE);
            turnEndPhase(field);
            if(field.isEnd()) {
                break;
            }
            int oldTapi = field.getTapi();
            int oldTipi = field.getTipi();
            field.setTapi(oldTipi);
            field.setTipi(oldTapi);
            field.nextTurn();
        }

        int raidDamage = 0;
        switch (field.getOptimizationMode()) {
            case RAID:
                raidDamage = 15 + (field.getDefender().getTotalCardsDestroyed())
                        - (10 * field.getDefender().getCommander().getHP() / field.getDefender().getCommander().getMaxHP());
                break;
            default: break;
        }

        // You lose
        if (!field.getAttacker().getCommander().isAlive()) {
            switch (field.getOptimizationMode()) {
                case RAID: return new Results(0, 0, 1, raidDamage);
                case BRAWL: return new Results(0, 0, 1, 5);
                case BRAWL_DEFENSE:
                    int enemyBrawlScore = evaluateBrawlScore(field, 1);
                    int maxScore = Cards.MAX_POSSIBLE_SCORE[OptimizationMode.BRAWL_DEFENSE.ordinal()];
                    return new Results(0, 0, 1, maxScore - enemyBrawlScore);
                default: return new Results(0, 0, 1, 0);
            }
        }

        // You win
        if (!field.getDefender().getCommander().isAlive()) {
            switch (field.getOptimizationMode()) {
                case BRAWL: return new Results(1, 0, 0, evaluateBrawlScore(field, 0));
                case BRAWL_DEFENSE:
                    int maxScore = Cards.MAX_POSSIBLE_SCORE[OptimizationMode.BRAWL_DEFENSE.ordinal()];
                    int minScore = Cards.MIN_POSSIBLE_SCORE[OptimizationMode.BRAWL_DEFENSE.ordinal()];
                    return new Results(1, 0, 0, maxScore - minScore);
                case CAMPAIGN:
                    int totalDominionsDestroyed = 0;
                    if (field.getPlayer(0).getDeck().getAlphaDominion() != null) {
                        totalDominionsDestroyed = 1;
                    }
                    for (CardStatus structure : field.getPlayer(0).getStructures()) {
                        if (structure.isDominion()) {
                            totalDominionsDestroyed--;
                        }
                    }
                    int campaignScore = 100 - 10 * (field.getPlayer(0).getTotalCardsDestroyed() - totalDominionsDestroyed);
                    return new Results(1, 0, 0, campaignScore);
                default: return new Results(1, 0, 0, 100);
            }
        }

        // Draw
        if (field.getTurn() > turnLimit) {
            switch (field.getOptimizationMode()) {
                case DEFENSE: return new Results(0, 1, 0, 100);
                case RAID: return new Results(0, 1, 0, raidDamage);
                case BRAWL: return new Results(0, 1, 0, 5);
                case BRAWL_DEFENSE:
                    int maxScore = Cards.MAX_POSSIBLE_SCORE[OptimizationMode.BRAWL_DEFENSE.ordinal()];
                    int minScore = Cards.MIN_POSSIBLE_SCORE[OptimizationMode.BRAWL_DEFENSE.ordinal()];
                    return new Results(1, 0, 0, maxScore - minScore);
                default: return new Results(0, 1, 0, 0);
            }
        }

        // Huh? How did we get here?
        throw new AssertionError();
    }

    private static void playDominionAndFortresses(Field field) {
        // TODO: implement this
    }

    private static void turnStartPhase(Field field) {
        // Active player's commander card
        cooldownSkills(field.getTap().getCommander());

        // Active player's assault skills:
        // update index
        // reduce delay; reduce skill cooldown
        List<CardStatus> assaults = field.getTap().getAssaults();
        for (int index = 0; index < assaults.size(); index++) {
            CardStatus status = assaults.get(index);
            status.setIndex(index);
            if (status.getDelay() > 0) {
                status.reduceDelay();
                if (status.getDelay() == 0) {
                    checkAndPerformValor(field, status);
                    checkAndPerformSummon(field, status);
                }
            } else {
                cooldownSkills(status);
            }
        }

        // Active player's structure cards:
        // update index
        // reduce delay; reduce skill cooldown
        List<CardStatus> structures = field.getTap().getStructures();
        for (int index = 0; index < structures.size(); index++) {
            CardStatus status = structures.get(index);
            status.setIndex(index);
            if (status.getDelay() > 0) {
                status.reduceDelay();
                if (status.getDelay() == 0) {
                    checkAndPerformSummon(field, status);
                }
            } else {
                cooldownSkills(status);
            }
        }

        // Defending player's assault cards:
        // update index
        for (int index = 0; index < assaults.size(); index++) {
            CardStatus status = assaults.get(index);
            status.setIndex(index);
        }

        // Defending player's structure cards:
        for (int index = 0; index < structures.size(); index++) {
            CardStatus status = structures.get(index);
            status.setIndex(index);
        }
    }

    private static void resolveSkill(Field field) {
        while (!field.skillQueue.isEmpty()) {

            Pair<CardStatus, SkillSpec> skillInstance = field.skillQueue.pop();
            CardStatus status = skillInstance.getFirst();
            SkillSpec ss = skillInstance.getSecond();

            if (status.getCard().getSkillTrigger()[ss.getId().ordinal()] == SkillTrigger.ACTIVATE) {
                if (!status.isAlive() || status.isJammed())
                    continue;
            }

            // Is summon? (non-activation skill)
            if (ss.getId() == Skill.SUMMON) {
                checkAndPerformSummon(field, status);
                continue;
            }

            SkillSpec modifiedSkill = ss.clone();

            // Apply evolve
            // TODO: implement this


            // Apply sabotage (only for X-based activation skills)
            // TODO: implement this


            // Apply enhance
            // TODO: implement this


            // Perform skill (if it is still applicable)
            // TODO: implement this

        }
    }

    private static void simulatePlayCardPhase(Field field, Card playedCard) {
        boolean bgeMegamorphosis = field.getBGEffects(field.getTapi())[PassiveBGE.MEGAMORPHOSIS.ordinal()] != null;

        int playedFactionMask = 0;
        int sameFactionCardsCount = 0;

        // Begin 'Play Card' phase action
        field.prepareAction();

        // Play selected card
        CardStatus playedStatus = null;
        switch (playedCard.getType()) {
            case ASSAULT:
                playedStatus = new PlayCard(playedCard, field, field.getTapi(), field.getTap().getCommander()).op();
                break;
            case STRUCTURE:
                playedStatus = new PlayCard(playedCard, field, field.getTapi(), field.getTap().getCommander()).op();
                break;
            default:
                throw new RuntimeException("Unknown card type: " + playedCard.getType());
        }
        resolveSkill(field); // resolve postponed skills recursively

        // End 'Play Card' phase action
        field.finalizeAction();

        // 1. Evaluate skill Allegiance & count assaults with same faction (structures will be counted later)
        // 2. Passive BGE Cold Sleep
        for (CardStatus status : field.getTap().getAssaults()) {
            if (status.equals(playedStatus)) { continue; } // Except itself

            if (bgeMegamorphosis || (status.getCard().getFaction() == playedCard.getFaction())) {
                sameFactionCardsCount++;
                int allegianceValue = status.skill(Skill.ALLEGIANCE);
                if (allegianceValue != 0) {
                    if (!status.isSundered()) {
                        status.addPermAttackBuff(allegianceValue);
                    }
                    status.extHP(allegianceValue);
                }
            }

            if (field.getBGEffects(field.getTapi())[PassiveBGE.COLDSLEEP.ordinal()] != null) {
                int bgeValue = (status.getProtectedByStasis() + 1) / 2;
                status.addHP(bgeValue);
            }
        }

        // TODO: finish this
        
    }

    private static void evaluatePassiveBGEHeroismSkills(Field field) {
        // TODO: implement this
    }

    private static void evaluateCommander(Field field) {
        // TODO: implement this
    }

    private static void evaluateStructures(Field field) {
        // TODO: implement this
    }

    private static void evaluateAssaults(Field field) {
        // TODO: implement this
    }

    private static int evaluateBrawlScore(Field field, int player) {
        // TODO: implement this
        return -1;
    }

    private static void turnEndPhase(Field field) {
        // TODO: implement this
    }

    private static void cooldownSkills(CardStatus card) {
        // TODO: implement this
    }

    public static void checkAndPerformValor(Field field, CardStatus status) {
        // TODO: implement this
    }

    private static void checkAndPerformSummon(Field field, CardStatus status) {
        // TODO: implement this
    }

}
