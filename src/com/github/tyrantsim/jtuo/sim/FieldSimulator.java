package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
import com.github.tyrantsim.jtuo.skills.SkillUtils;
import com.github.tyrantsim.jtuo.util.Pair;

import java.util.Deque;
import java.util.List;

import static com.github.tyrantsim.jtuo.util.Utils.safeMinus;

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

        // Setup faction marks (bitmap) for stasis (skill Statis / Passive BGE TemporalBacklash)
        // unless Passive BGE Megamorphosis is enabled
        if (!bgeMegamorphosis) {
            playedFactionMask = (1 << playedCard.getFaction().ordinal());
            boolean temporalBacklash = field.hasBGEffect(field.getTapi(), PassiveBGE.TEMPORALBACKLASH);
            if (playedStatus.skill(Skill.STASIS) != 0 || (temporalBacklash && playedStatus.skill(Skill.COUNTER) == 0)) {
                field.getTap().updateStasisFactions(playedFactionMask);
            }
        }

        // Evaluate Passive BGE Oath-of-Loyalty
        int allegianceValue;
        if (field.hasBGEffect(field.getTapi(), PassiveBGE.OATH_OF_LOYALTY) && playedStatus.skill(Skill.ALLEGIANCE) > 0) {
            allegianceValue = playedStatus.skill(Skill.ALLEGIANCE);
            // count structures with same faction (except fortresses, dominions and other non-normal structures)
            for (CardStatus status : field.getTap().getStructures()) {
                boolean sameFaction = status.getCard().getFaction() == playedCard.getFaction();
                if (status.getCard().getCategory() == CardCategory.NORMAL && (bgeMegamorphosis || sameFaction)) {
                    sameFactionCardsCount++;
                }
            }

            // apply Passive BGE Oath-of-Loyalty when multiplier isn't zero
            if (sameFactionCardsCount != 0) {
                int bgeValue = allegianceValue * sameFactionCardsCount;
                playedStatus.addPermAttackBuff(bgeValue);
                playedStatus.extHP(bgeValue);
            }
        }

        // Summarize stasis when
        // 1. Passive BGE Megamorphosis is enabled
        // 2. current faction is marked for it
        boolean factionApply = (field.getTap().getStasisFactionBitmap() & playedFactionMask) != 0;
        if (playedCard.getDelay() > 0 && playedCard.getType() == CardType.ASSAULT && (bgeMegamorphosis || factionApply)) {
            int stackedStasis = 0;
            if (bgeMegamorphosis || field.getTap().getCommander().getCard().getFaction() == playedCard.getFaction()) {
                stackedStasis = field.getTap().getCommander().skill(Skill.STASIS);
            }

            for (CardStatus status : field.getTap().getStructures()) {
                if (bgeMegamorphosis || status.getCard().getFaction() == playedCard.getFaction()) {
                    stackedStasis += status.skill(Skill.STASIS);
                }
            }

            for (CardStatus status : field.getTap().getAssaults()) {
                if (bgeMegamorphosis || status.getCard().getFaction() == playedCard.getFaction()) {
                    stackedStasis += status.skill(Skill.STASIS);
                }

                if(field.hasBGEffect(field.getTapi(), PassiveBGE.TEMPORALBACKLASH) && status.skill(Skill.COUNTER) != 0) {
                    stackedStasis += (status.skill(Skill.COUNTER) + 1) / 2;
                }
            }

            playedStatus.setProtectedByStasis(stackedStasis);

            if (!bgeMegamorphosis && stackedStasis == 0) {
                field.getTap().setStasisFactionBitmap(field.getTap().getStasisFactionBitmap() & playedFactionMask);
            }
        }
    }

    private static void evaluatePassiveBGEHeroismSkills(Field field) {
        // TODO: implement this
    }

    private static void evaluateCommander(Field field) {
        field.setCurrentPhase(FieldPhase.COMMANDER_PHASE);
        evaluateSkills(field, field.getTap().getCommander(), field.getTap().getCommander().getCard().getSkills());
    }

    private static void evaluateStructures(Field field) {
        field.setCurrentPhase(FieldPhase.STRUCTURES_PHASE);
        field.setCurrentCI(0);
        for (int i = 0; !field.isEnd() && (field.getCurrentCI() < field.getTap().getStructures().size()); i++) {
            field.setCurrentCI(i);
            CardStatus currentStatus = field.getTap().getStructures().get(i);
            if (currentStatus.isActive()) {
                evaluateSkills(field, currentStatus, currentStatus.getCard().getSkills());
            }
        }
    }

    private static void evaluateAssaults(Field field) {
        field.setCurrentPhase(FieldPhase.ASSAULTS_PHASE);
        field.setBloodlustValue(0);
        for (int i = 0; !field.isEnd() && (field.getCurrentCI() < field.getTap().getAssaults().size()); i++) {
            field.setCurrentCI(i);
            CardStatus currentStatus = field.getTap().getAssaults().get(i);
            Boolean attacked = false;

            if (!currentStatus.isAlive()) {
                // Passive BGE: Halted orders
                // TODO: implement this
            } else {
                currentStatus.setProtectedByStasis(0);
                field.setAssaultBloodlusted(false);
                currentStatus.setStep(CardStep.ATTACKING);
                attacked = evaluateSkills(field, currentStatus, currentStatus.getCard().getSkills(), attacked);
                if (field.isEnd()) break;
                if (!currentStatus.isAlive()) continue;
            }

            if (currentStatus.getCorrodedRate() != 0) {
                if (attacked) {
                    int v = Math.min(currentStatus.getCorrodedRate(), currentStatus.getAttackPower());
                    int corrosion = Math.min(v,
                            currentStatus.getCard().getAttack() + currentStatus.getPermAttackBuff() - currentStatus.getCorrodedWeakened()
                    );
                    currentStatus.setCorrodedWeakened(currentStatus.getCorrodedWeakened() + corrosion);
                }
            }

            currentStatus.setStep(CardStep.ATTACKED);
        }
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

    private static void evaluateSkills(Field field, CardStatus card, List<SkillSpec> skills) {
        evaluateSkills(field, card, skills, null);
    }

    /**
     * @return if card has attacked
     */
    private static boolean evaluateSkills(Field field, CardStatus status, List<SkillSpec> skills, Boolean attacked) {
        int numActions = 1;
        for (int actionIndex = 0; actionIndex < numActions; actionIndex++) {
            field.prepareAction();

            for (SkillSpec ss : skills) {
                if (!SkillUtils.isActivationSkill(ss.getId())) continue;
                if (status.getSkillCd(ss.getId()) > 0) continue;
                field.getSkillQueue().add(new Pair<>(status, ss));
                resolveSkill(field);
            }

            if (status.getCard().getType() == CardType.ASSAULT) {
                if (status.canAct()) {
                    if (attackPhase(field)) {
                        attacked = true;
                        if (field.isEnd()) {
                            return attacked;
                        }
                    }
                }
            }
            field.finalizeAction();

            // Flurry
            if (status.canAct() && status.hasSkill(Skill.FLURRY) && status.getSkillCd(Skill.FLURRY) == 0) {
                numActions += status.skillBaseValue(Skill.FLURRY);
                for (SkillSpec ss : skills) {
                    Skill evolvedSkillId = Skill.values()[ss.getId().ordinal() + status.getEvolvedSkillOffset(ss.getId())];
                    if (evolvedSkillId == Skill.FLURRY) {
                        status.setSkillCd(ss.getId(), ss.getC());
                    }
                }
            }

        }

        return attacked;
    }

    /**
     * @return true if actually attacks
     */
    private static boolean attackPhase(Field field) {
        CardStatus attStatus = field.getTap().getAssaults().get(field.getCurrentCI()); // attacking card
        List<CardStatus> defAssaults = field.getTip().getAssaults();

        if (attStatus.getAttackPower() == 0) {
            return false;
        }

        int attDmg = 0;
        if (field.getCurrentCI() < defAssaults.size() && defAssaults.get(field.getCurrentCI()).isAlive()) {
            CardStatus defStatus = defAssaults.get(field.getCurrentCI());
            attDmg = performAttack(field, attStatus, defStatus);
            int swipeValue = attStatus.skill(Skill.SWIPE);
            int drainValue = attStatus.skill(Skill.DRAIN);
            if (swipeValue != 0 || drainValue != 0) {
                boolean criticalReach = field.hasBGEffect(field.getTapi(), PassiveBGE.CRITICALREACH);
                int drainTotalDmg = attDmg;
                int adjSize = 1 + (criticalReach ? 1 : 0);
                int hostIdx = defStatus.getIndex();
                int fromIdx = safeMinus(hostIdx, adjSize);
                int tillIdx = Math.min(hostIdx + adjSize, safeMinus(defAssaults.size(), 1));

                for (; fromIdx <= tillIdx; fromIdx++) {
                    if (fromIdx == hostIdx) continue;
                    CardStatus adjStatus = defAssaults.get(fromIdx);
                    if (!adjStatus.isAlive()) continue;
                    int swipeDmg = safeMinus(swipeValue + drainValue + defStatus.getEnfeebled(), defStatus.protectedValue());
                    removeHP(field, adjStatus, swipeDmg);
                    drainTotalDmg += swipeDmg;
                }

                if (drainValue != 0 && skillCheck(field, Skill.DRAIN, attStatus, null)) {
                    attStatus.addHP(drainTotalDmg);
                }
                prependOnDeath(field);
                resolveSkill(field);
            }
        } else {
            // might be blocked by walls
            attDmg = attackCommander(field, attStatus);
        }

        // Passive BGE: Bloodlust
        if (field.hasBGEffect(field.getTapi(), PassiveBGE.BLOODLUST) && !field.isAssaultBloodlusted() && attDmg > 0) {
            field.addBloodlust(field.getBGEffects(field.getTapi())[PassiveBGE.BLOODLUST.ordinal()] != null ? 1 : 0);
            field.setAssaultBloodlusted(true);
        }

        return true;
    }

    private static int performAttack(Field field, CardStatus attacker, CardStatus defender) {
        // TODO: implement this
        return 0;
    }

    private static void removeHP(Field field, CardStatus adjStatus, int swipeDmg) {
        // TODO: implement this
    }

    private static boolean skillCheck(Field field, Skill skill, CardStatus c, CardStatus ref) {
        return SkillUtils.isDefensiveSkill(skill) || c.isAlive();
    }

    private static void prependOnDeath(Field field) {
        // TODO: implement this
    }

    private static int attackCommander(Field field, CardStatus attStatus) {
        // TODO: implement this
        return 0;
    }

}
