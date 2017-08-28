package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

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
                simulatePlayCardPhase(field);
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
                // TODO: implement this
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

        Hand[] players = field.getPlayers();
        int raidDamage = 0;
        switch (field.getOptimizationMode()) {
            // TODO: implement this
        }

        if (!field.getAttacker().getCommander().isAlive()) {
            // TODO: implement this
        }

        if (!field.getDefender().getCommander().isAlive()) {
            // TODO: implement this
        }

        if (field.getTurn() > turnLimit) {
            // TODO: implement this
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
        // TODO: implement this
    }

    private static void simulatePlayCardPhase(Field field) {
        // TODO: implement this
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

    private static void turnEndPhase(Field field) {
        // TODO: implement this
    }

    private static void cooldownSkills(CardStatus card) {
        // TODO: implement this
    }

    private static void checkAndPerformValor(Field field, CardStatus status) {
        // TODO: implement this
    }

    private static void checkAndPerformSummon(Field field, CardStatus status) {
        // TODO: implement this
    }

}
