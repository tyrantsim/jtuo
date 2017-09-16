package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.Constants;
import com.github.tyrantsim.jtuo.Main;
import com.github.tyrantsim.jtuo.cards.*;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
import com.github.tyrantsim.jtuo.skills.SkillUtils;
import com.github.tyrantsim.jtuo.util.Pair;

import java.text.MessageFormat;
import java.util.List;

import static com.github.tyrantsim.jtuo.util.Utils.safeMinus;

public class FieldSimulator {

    public static int turnLimit = Constants.DEFAULT_TURN_LIMIT;

    SkillTableElement[] skill_table = new SkillTableElement[Skill.values().length];
    
    private static class SkillTableElement {
        public Field field;
        public CardStatus src; 
        public SkillSpec spec;
        public SkillTableElement(Field field, CardStatus src, SkillSpec spec){
            this.field = field;
            this.src = src;            
            this.spec = spec;
        }
    }
    
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

            boolean bgeMegamorphosis = field.hasBGEffect(field.getTapi(), PassiveBGE.MEGAMORPHOSIS);

            // Play a card
            Card playedCard = field.getTap().getDeck().next();
            if (playedCard != null) {
                simulatePlayCardPhase(field, playedCard);
            }
            if (field.isEnd()) {
                break;
            }

            // Evaluate Passive BGE Heroism skills
            if (field.hasBGEffect(field.getTapi(), PassiveBGE.HEROISM)) {
                evaluatePassiveBGEHeroismSkills(field);
            }

            // Evaluate activation BGE skills
            for (SkillSpec bgSkill : field.getBGSkills(field.getTapi())) {
                field.prepareAction();
                field.skillQueue.addLast(new Pair<>(field.tap.getCommander().clone(), bgSkill.clone()));
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

            Hand tmpHand = field.getTip();
            field.setTip(field.getTap());
            field.setTap(tmpHand);

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

        for (int i = 0, ai = field.getTapi(); i < 2; i++) {

            if (field.getPlayer(ai).getDeck().getAlphaDominion() != null)
                new PlayCard(field.getPlayer(ai).getDeck().getAlphaDominion(), field, ai,
                        field.getPlayer(ai).getCommander()).op();

            for (Card playedCard: field.getPlayer(ai).getDeck().getShuffledForts())
                new PlayCard(playedCard, field, ai, field.getPlayer(ai).getCommander()).op();

            // Swap
            int tmp = field.getTipi();
            field.setTipi(field.getTapi());
            field.setTapi(tmp);

            Hand tmpHand = field.getTip();
            field.setTip(field.getTap());
            field.setTap(tmpHand);

            ai = opponent(ai);
        }
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
            int evolvedOffset = status.getEvolvedSkillOffset(modifiedSkill.getId());
            if (evolvedOffset != 0)
                modifiedSkill.setId(Skill.values()[ss.getId().ordinal() + evolvedOffset]);

            // Apply sabotage (only for X-based activation skills)
            int sabotagedValue = status.getSabotaged();
            if (sabotagedValue > 0 && SkillUtils.isActivationSkillWithX(modifiedSkill.getId()))
                modifiedSkill.setX(modifiedSkill.getX() - Math.min(modifiedSkill.getX(), sabotagedValue));

            // Apply enhance
            int enhancedValue = status.getEnhanced(modifiedSkill.getId());
            if (enhancedValue > 0)
                modifiedSkill.setX(modifiedSkill.getX() + enhancedValue);

            // Perform skill (if it is still applicable)
            if (!(SkillUtils.isActivationSkillWithX(modifiedSkill.getId()) && modifiedSkill.getX() == 0))
            {}//TODO: WTF is skill_table??? from original code: void(*skill_table[Skill.num_skills])(Field*, CardStatus* src, const SkillSpec&);

        }
    }

    private static void simulatePlayCardPhase(Field field, Card playedCard) {
        boolean bgeMegamorphosis = field.hasBGEffect(field.getTapi(), PassiveBGE.MEGAMORPHOSIS);

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

            if (field.hasBGEffect(field.getTapi(), PassiveBGE.COLDSLEEP)) {
                int bgeValue = (status.getProtectedByStasis() + 1) / 2;
                status.addHP(bgeValue);
            }
        }

        // Setup faction marks (bitmap) for stasis (skill Statis / Passive BGE TemporalBacklash)
        // unless Passive BGE Megamorphosis is enabled
        if (!bgeMegamorphosis) {
            playedFactionMask = (1 + playedCard.getFaction().ordinal());
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
        for (int i = 0; !field.isEnd() && (i < field.getTap().getAssaults().size()); i++) {
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
        // Inactive player's assault cards:
        List<CardStatus> assaults = field.getTip().getAssaults();
        for (CardStatus status : assaults) {
            if (status.getHP() <= 0) continue;
            status.setEnfeebled(0);
            status.setProtectedBy(0);
            status.clearPrimarySkillOffset();
            status.clearEvolvedSkillOffset();
            status.clearEnhancedValue();
            status.setEvaded(0); // so far only useful in Inactive turn
            status.setPaybacked(0); // ditto
            status.setEntrapped(0);
        }

        // Inactive player's structure card
        List<CardStatus> structures = field.getTip().getStructures();
        for (CardStatus status : structures) {
            if (status.getHP() <= 0) continue;
            status.setEvaded(0); // so far only useful in Inactive turn
        }

        // Active player's assault cards:
        assaults = field.getTap().getAssaults();
        for (CardStatus status : assaults) {
            if (status.getHP() <= 0) continue;

            int refreshValue = status.skill(Skill.REFRESH);
            if (refreshValue != 0 && skillCheck(field, Skill.REFRESH, status, null)) {
                status.addHP(refreshValue);
            }
            if (status.getPoisoned() > 0) {
                int poisonDmg = safeMinus(status.getPoisoned() + status.getEnfeebled(), status.protectedValue());
                if (poisonDmg > 0) {
                    removeHP(field, status, poisonDmg);
                }
            }
            // end of the opponent's next turn for enemy units
            status.setTempAttackBuff(0);
            status.setJammed(false);
            status.setEnraged(0);
            status.setSundered(false);
            status.setInhibited(0);
            status.setSabotaged(0);
            status.setTributed(0);
            status.setOverloaded(false);
            status.setStep(CardStep.NONE);
        }

        // Active player's structure cards:
        // nothing so far

        prependOnDeath(field); // poison
        resolveSkill(field);
        removeDead(field.getTap().getAssaults());
        removeDead(field.getTap().getStructures());
        removeDead(field.getTip().getAssaults());
        removeDead(field.getTip().getStructures());
    }

    private static void removeDead(List<CardStatus> cards) {
        cards.removeIf(card->!card.isAlive());
    }

    private static void cooldownSkills(CardStatus status) {
        for (SkillSpec skill: status.getCard().getSkills()) {
            if (status.getSkillCd(skill.getId()) > 0) {
                debug(2, "%s reduces timer (%d) of skill %s\n",
                        status.description(), String.valueOf(status.getSkillCd(skill.getId())), skill.getId().toString());
                status.cooldownSkillCd(skill.getId());
            }
        }
    }

    /**
     * @return true if valor triggered
     */
    public static boolean checkAndPerformValor(Field field, CardStatus src) {

        int valorValue = src.skill(Skill.VALOR);

        if (valorValue > 0 && !src.isSundered() && skillCheck(field, Skill.VALOR, src, null)) {

            final Hand opponentPlayer = field.getPlayer(opponent(src.getPlayer()));
            final CardStatus dst = opponentPlayer.getAssaults().size() > src.getIndex()
                    ? opponentPlayer.getAssaults().get(src.getIndex()) : null;

            if (dst == null || dst.getHP() <= 0) {
                debug(1, "%s loses Valor (no blocker)\n", src.description());
                return false;
            } else if (dst.getAttackPower() <= src.getAttackPower()) {
                debug(1, "%s loses Valor (weak blocker %s)\n", src.description(), dst.description());
                return false;
            }

            debug(1, "%s activates Valor %d\n", src.description(), String.valueOf(valorValue));
            src.setPermAttackBuff(src.getPermAttackBuff() + valorValue);
            return true;
        }

        return false;

    }

    /**
     * @return summoned card, null if none
     */
    private static CardStatus checkAndPerformSummon(Field field, CardStatus status) {

        int summonedCardId = status.getCard().getSkillValue()[Skill.SUMMON.ordinal()];
        if (summonedCardId > 0) {
            final Card summonedCard;
            try {
                summonedCard = Cards.getCardById(summonedCardId);
            } catch (Exception e) {
                return null;
            }

            debug(1, "%s summons %s\n", status.description(), summonedCard.getName());

            switch (summonedCard.getType()) {
                case ASSAULT:
                case STRUCTURE:
                    return new PlayCard(summonedCard, field, status.getPlayer(), status).op();
                default:
                    debug(0, "Unknown card type: #%d %s: %s\n", String.valueOf(summonedCard.getId()),
                            summonedCard.getName(), summonedCard.getType().toString());
                    throw new AssertionError();
            }
        }

        return null;
    }

    private static void evaluateSkills(Field field, CardStatus card, List<SkillSpec> skills) {
        evaluateSkills(field, card, skills, null);
    }

    /**
     * @return if card has attacked
     */
    private static Boolean evaluateSkills(Field field, CardStatus status, List<SkillSpec> skills, Boolean attacked) {
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
            attDmg = performAttack(field, CardType.ASSAULT, attStatus, defStatus);
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
            field.addBloodlust(field.hasBGEffect(field.getTapi(), PassiveBGE.BLOODLUST) ? 1 : 0);
            field.setAssaultBloodlusted(true);
        }

        return true;
    }

    private static int performAttack(Field field, CardType cardType, CardStatus attStatus, CardStatus defStatus) {
        int preModifierDmg = attStatus.getAttackPower();

        // Evaluation order:
        // modify damage
        // deal damage
        // assaults only: (poison)
        // counter, berserk
        // assaults only: (leech if still alive)

        int attDmg = modifyAttackDamage(field, cardType, attStatus, defStatus, preModifierDmg);

        if (attDmg == 0) {
            return 0;
        }

        attackDamage(field, cardType, attStatus, defStatus, attDmg);
        if (field.isEnd()) {
            return attDmg;
        }
        damageDependantPreOA(field, attStatus, defStatus);

        // TODO: implement skills

        return attDmg;
    }

    private static int modifyAttackDamage(Field field, CardType cardType, CardStatus attStatus, CardStatus defStatus, int preModifierDmg) {

        int attDmg = preModifierDmg;

        if (attDmg == 0) return 0;

        List<CardStatus> attAssaults = field.tap.getAssaults(); // (active) attacker assaults
        List<CardStatus> defAssaults = field.tip.getAssaults(); // (inactive) defender assaults
        int legionValue = 0; // Starting legion value

        // Enhance damage (if additional damage isn't prevented)
        if (!attStatus.isSundered()) {

            // Skill: Legion
            int legionBase = attStatus.skill(Skill.LEGION);
            if (legionBase > 0) {

                boolean bgeMegamorphosis = field.hasBGEffect(field.tapi, PassiveBGE.MEGAMORPHOSIS);

                // Check if adjacent cards add legion value
                if (attStatus.getIndex() > 0 && attAssaults.get(attStatus.getIndex() - 1).isAlive() && (bgeMegamorphosis
                        || attAssaults.get(attStatus.getIndex() - 1).getCard().getFaction() == attStatus.getCard().getFaction()))
                    legionValue++;

                if (attStatus.getIndex() + 1 < attAssaults.size() && attAssaults.get(attStatus.getIndex() + 1).isAlive() && (bgeMegamorphosis
                        || attAssaults.get(attStatus.getIndex() + 1).getCard().getFaction() == attStatus.getCard().getFaction()))
                    legionValue++;

                if (legionValue > 0) {
                    legionValue *= legionBase;
                    attDmg += legionValue;
                }
            }

            // Skill: Coalition
            int coalitionBase = attStatus.skill(Skill.COALITION);
            if (coalitionBase > 0) {
                // TODO: Implement this
            }

            // Skill: Rupture
            int ruptureValue = attStatus.skill(Skill.RUPTURE);
            if (ruptureValue > 0)
                attDmg += ruptureValue;

            // Skill: Venom
            int venomValue = attStatus.skill(Skill.VENOM);
            if (venomValue > 0 && defStatus.getPoisoned() > 0)
                attDmg += venomValue;

            // PassiveBGE: Bloodlust
            if (field.bloodlustValue > 0)
                attDmg += field.bloodlustValue;

            // State: Enfeebled
            if (defStatus.getEnfeebled() > 0)
                attDmg += defStatus.getEnfeebled();
        }

        // Prevent damage
        int reducedDmg = 0;

        // Skill: Armor
        int armorValue = 0;
        if (defStatus.getCard().getType() == CardType.ASSAULT) {
            // PassiveBGE: Fortification (adj step -> 1 (1 left, host, 1 right)
            // TODO: C++ code says tapi but probably should be tipi.. Test this
            int adjSize = field.hasBGEffect(field.tapi, PassiveBGE.FORTIFICATION) ? 1 : 0;
            int hostIdx = defStatus.getIndex();
            int fromIdx = safeMinus(hostIdx, adjSize);
            int tillIdx = Math.min(hostIdx + adjSize, safeMinus(defAssaults.size(), 1));

            while (fromIdx <= tillIdx) {
                CardStatus adjStatus = defAssaults.get(fromIdx);
                if (adjStatus.isAlive())
                    armorValue = Math.max(armorValue, adjStatus.skill(Skill.ARMORED));
                fromIdx++;
            }
        }

        if (armorValue > 0)
            reducedDmg += armorValue;

        // Skill: Protect
        if (defStatus.protectedValue() > 0)
            reducedDmg += defStatus.protectedValue();

        // Skill: Pierce
        int pierceValue = attStatus.skill(Skill.PIERCE) + attStatus.skill(Skill.RUPTURE);
        if (reducedDmg > 0 && pierceValue > 0)
            reducedDmg = safeMinus(reducedDmg, pierceValue);

        // Final Attack Damage
        attDmg = safeMinus(attDmg, reducedDmg);

        debug(1, "%s attacks %s for %d damage\n",
                attStatus.description(), defStatus.description(), String.valueOf(preModifierDmg));

        // PassiveBGE: Brigade
        if (field.hasBGEffect(field.tapi, PassiveBGE.BRIGADE) && legionValue > 0 && attStatus.canBeHealed()) {
            debug(1, "Brigade: %s heals itself for %d\n", attStatus.description(), String.valueOf(legionValue));
            attStatus.addHP(legionValue);
        }

        return attDmg;
    }

    private static void attackDamage(Field field, CardType cardType, CardStatus attStatus, CardStatus defStatus, int attDmg) {
        removeHP(field, defStatus, attDmg);
        prependOnDeath(field);
        resolveSkill(field);
    }

    private static void damageDependantPreOA(Field field, CardStatus attStatus, CardStatus defStatus) {
        // Skill: Poison / Venom
        int poisonValue = Math.max(attStatus.skill(Skill.POISON), attStatus.skill(Skill.VENOM));
        if (poisonValue > defStatus.getPoisoned() && skillCheck(field, Skill.POISON, attStatus, defStatus)) {
            // Perform skill poison
            debug(1, "%s poisons %s by %d\n", attStatus.description(), defStatus.description(), String.valueOf(poisonValue));
            defStatus.setPoisoned(poisonValue);
        }

        // Damage-Dependant Skill: Inhibit
        int inhibitValue = attStatus.skill(Skill.INHIBIT);
        if (inhibitValue > defStatus.getInhibited() && skillCheck(field, Skill.INHIBIT, attStatus, defStatus)) {
            debug(1, "%s inhibits %s by %d\n", attStatus.description(), defStatus.description(), String.valueOf(inhibitValue));
            defStatus.setInhibited(inhibitValue);
        }

        // Damage-Dependant Skill: Sabotage
        int sabotagedValue = attStatus.skill(Skill.SABOTAGE);
        if (sabotagedValue > defStatus.getSabotaged() && skillCheck(field, Skill.SABOTAGE, attStatus, defStatus)) {
            debug(1, "%s sabotages %s by %d\n", attStatus.description(), defStatus.description(), String.valueOf(sabotagedValue));
            defStatus.setSabotaged(sabotagedValue);
        }
    }

    private static void removeHP(Field field, CardStatus status, int dmg) {
        if (dmg == 0) return;
        status.setHP(safeMinus(status.getHP(), dmg));
        if (field.getCurrentPhase().ordinal() < FieldPhase.END_PHASE.ordinal() && status.hasSkill(Skill.BARRIER)) {
            field.incDamagedUnitsToTimes(status);
        }
        if (status.getHP() == 0) {
            field.killedUnits.add(status);
            field.players[status.getPlayer()].incTotalCardsDestroyed();
            if (status.getPlayer() == 0) {
                boolean isVip = false;
                for (Integer card : field.getPlayer(0).getDeck().getVipCards()) {
                    if (card == status.getCard().getId()) {
                        isVip = true;
                        break;
                    }
                }

                if(isVip) {
                    field.getPlayers()[0].getCommander().setHP(0);
                    field.setEnd(true);
                }
            }
        }
    }

    private static boolean skillCheck(Field field, Skill skill, CardStatus c, CardStatus ref) {
        return SkillUtils.isDefensiveSkill(skill) || c.isAlive();
    }

    private static void prependOnDeath(Field field) {
        if (field.getKilledUnits().isEmpty()) return;

        List<CardStatus> assaults = field.getPlayer(field.getKilledUnits().get(0).getPlayer()).getAssaults();
        int stackedPoisonValue = 0;
        int lastIndex = 99999;
        CardStatus leftVirulenceVictim = null;
        for (CardStatus status : field.getKilledUnits()) {
            if (status.getCard().getType() == CardType.ASSAULT) {

                // Skill: Avenge
                final int hostIdx = status.getIndex();
                int fromIdx, tillIdx;
                if (field.hasBGEffect(field.tapi, PassiveBGE.BLOOD_VENGEANCE)) {
                    // Passive BGE Blood Vengeance: scan all assaults for Avenge
                    fromIdx = 0;
                    tillIdx = assaults.size() - 1;
                } else {
                    fromIdx = safeMinus(hostIdx, 1);
                    tillIdx = Math.min(hostIdx + 1, safeMinus(assaults.size(), 1));
                }

                for (; fromIdx <= tillIdx; fromIdx++) {
                    if (fromIdx == hostIdx) continue;
                    CardStatus adjStatus = assaults.get(fromIdx);
                    if (!adjStatus.isAlive()) continue;
                    int avengeValue = adjStatus.skill(Skill.AVENGE);
                    if (avengeValue == 0) continue;

                    // Passive BGE Blood Vengeance: use half value rounded up
                    // (for distance > 1, i. e. non-standard Avenge triggering)
                    if (Math.abs(fromIdx - hostIdx) > 1)
                        avengeValue = (avengeValue + 1) / 2;

                    debug(1, "%s%s activates Avenge %d\n", Math.abs(fromIdx - hostIdx) > 1
                            ? "BGE BloodVengeance: " : "", adjStatus.description(), String.valueOf(avengeValue));

                    if (!adjStatus.isSundered())
                        adjStatus.setPermAttackBuff(adjStatus.getPermAttackBuff() + avengeValue);
                    adjStatus.extHP(avengeValue);
                }

                // Passive BGE: Virulence
                if (field.hasBGEffect(field.tapi, PassiveBGE.VIRULENCE)) {
                    if (status.getIndex() != lastIndex + 1) {
                        stackedPoisonValue = 0;
                        leftVirulenceVictim = null;
                        if (status.getIndex() > 0) {
                            CardStatus leftStatus = assaults.get(status.getIndex() - 1);
                            if (leftStatus.isAlive())
                                leftVirulenceVictim = leftStatus;
                        }
                    }

                    if (status.getPoisoned() > 0) {
                        if (leftVirulenceVictim != null) {
                            debug(1, "Virulence: %s spreads left poison +%d to %s\n", status.description(),
                                    String.valueOf(status.getPoisoned()), leftVirulenceVictim.description());
                            leftVirulenceVictim.setPoisoned(leftVirulenceVictim.getPoisoned() + status.getPoisoned());
                        }
                        stackedPoisonValue += status.getPoisoned();
                        debug(1, "Virulence: %s spreads right poison +%d = %d\n", status.description(),
                                String.valueOf(status.getPoisoned()), String.valueOf(stackedPoisonValue));
                    }

                    if (status.getIndex() < assaults.size()) {
                        CardStatus rightStatus = assaults.get(status.getIndex() + 1);
                        if (rightStatus.isAlive()) {
                            debug(1, "Virulence: spreads stacked poison +%d to %s\n",
                                    String.valueOf(stackedPoisonValue), rightStatus.description());
                            rightStatus.setPoisoned(rightStatus.getPoisoned() + stackedPoisonValue);
                        }
                    }

                    lastIndex = status.getIndex();
                }
            }

            // Passive BGE: Revenge
            if (field.hasBGEffect(field.tapi, PassiveBGE.REVENGE)) {

                if (field.getBGEffectValue(field.tapi, PassiveBGE.REVENGE) < 0)
                    throw new RuntimeException("BGE Revenge: Value must be defined & positive");

                SkillSpec ssHeal = new SkillSpec(Skill.HEAL, field.getBGEffectValue(field.tapi, PassiveBGE.REVENGE),
                        Faction.ALL_FACTIONS, 0, 0, Skill.NO_SKILL, Skill.NO_SKILL, true, 0, SkillTrigger.ACTIVATE);
                SkillSpec ssRally = new SkillSpec(Skill.RALLY, field.getBGEffectValue(field.tapi, PassiveBGE.REVENGE),
                        Faction.ALL_FACTIONS, 0, 0, Skill.NO_SKILL, Skill.NO_SKILL, true, 0, SkillTrigger.ACTIVATE);
                CardStatus commander = field.getPlayer(status.getPlayer()).getCommander();
                debug(2, "Revenge: Preparing (head) skills  %s and %s\n", ssHeal.description(), ssRally.description());
                field.skillQueue.addFirst(new Pair<>(commander, ssRally));
                field.skillQueue.addFirst(new Pair<>(commander, ssHeal)); // +1: keep ss_heal at first place
            }

            // resolve On-Death skills
            for (SkillSpec ss : status.getCard().getSkillsOnDeath()) {
                field.addSkillToQueue(status, ss);
            }
        }
        field.getKilledUnits().clear();
    }

    private static int attackCommander(Field field, CardStatus attStatus) {
        CardStatus defStatus = selectFirstEnemyWall(field);
        if (defStatus != null) {
            return performAttack(field, CardType.STRUCTURE, attStatus, defStatus);
        } else {
            return performAttack(field, CardType.COMMANDER, attStatus, field.getTip().getCommander());
        }
    }

    private static CardStatus selectFirstEnemyWall(Field field) {
        for (CardStatus c : field.getTip().getStructures()) {
            if (c.hasSkill(Skill.WALL) && c.isAlive()) {
                return c;
            }
        }
        return null;
    }

    private static int opponent(int player) {
        return ((player + 1) % 2);
    }

//    void perform_targetted_hostile_fast(Skill skill_id, Field fd, CardStatus src, SkillSpec s) {
//        select_targets(skill_id, fd, src, s);
//        List<CardStatus> paybackers;
//        boolean has_turningtides = (fd.getBGEffects(fd.getTapi())[PassiveBGE.TURNINGTIDES] != null && (skill_id == Skill.WEAKEN || skill_id == Skill.SUNDER));
//        int turningtides_value= 0, old_attack = 0;
//
//        // apply skill to each target(dst)
//        int selection_array_len = fd.selectionArray.size();
//        //ArrayList<CardStatus> selection_array = new ArrayList<CardStatus> new CardStatus[selection_array_len];
//        //std.memcpy(selection_array, fd.getselection_array[0], selection_array_len * sizeof(CardStatus *));
//        ArrayList<CardStatus> selection_array = (ArrayList<CardStatus>)fd.selectionArray.clone();
//        for (CardStatus dst: selection_array) {
//            // TurningTides
//            if (__builtin_expect(has_turningtides, false))
//            {
//                old_attack = dst.getAttackPower();
//            }
//
//            // check & apply skill to target(dst)
//            if (check_and_perform_skill<skill_id>(fd, src, dst, s, ! src.getm_overloaded)) {
//                // TurningTides: get max attack decreasing
//                if (has_turningtides) {
//                    turningtides_value = Math.max(turningtides_value, safe_minus(old_attack, dst.getAttackPower()));
//                }
//
//                // Payback/Revenge: collect paybackers/revengers
//                int payback_value = dst.getSkillCd(Skill.PAYBACK) + dst.getSkillCd(Skill.REVENGE); // dst.getskill(Skill.PAYBACK) + dst.getskill(Skill.REVENGE);
//                if ((s.getId() != Skill.MIMIC) && (dst.getPaybacked() < payback_value) && skill_check<Skill.payback>(fd, dst, src)) {
//                    paybackers.reserve(selection_array_len);
//                    paybackers.push_back(dst);
//                }
//            }
//        }
//
//        // apply TurningTides
//        if (has_turningtides && turningtides_value > 0) {
//            SkillSpec ss_rally{Skill.rally, turningtides_value, allfactions, 0, 0, Skill.no_skill, Skill.no_skill, s.all, 0,};
//            debug(1, "TurningTides %u!\n", turningtides_value);
//            perform_targetted_allied_fast<Skill.rally>(fd, &fd.getplayers[src.getm_player].getcommander, ss_rally);
//        }
//
//        prepend_on_death(fd);  // skills
//
//        // Payback/Revenge
//        for (CardStatus pb_status: paybackers)
//        {
//            turningtides_value = 0;
//
//            // apply Revenge
//            if (pb_status.getSkillCd(Skill.REVENGE))
//            {
//                int revenged_count = 0;
//                for (int case_index = 0; case_index < 3; ++ case_index)
//                {
//                    CardStatus * target_status;
//                    const char * target_desc;
//                    switch (case_index)
//                    {
//                    // revenge to left
//                    case 0:
//                        if (!(target_status = fd.getleft_assault(src))) { continue; }
//                        target_desc = "left";
//                        break;
//
//                    // revenge to core
//                    case 1:
//                        target_status = src;
//                        target_desc = "core";
//                        break;
//
//                    // revenge to right
//                    case 2:
//                        if (!(target_status = fd.getright_assault(src))) { continue; }
//                        target_desc = "right";
//                        break;
//
//                    // wtf?
//                    default:
//                        __builtin_unreachable();
//                    }
//
//                    // skip illegal target
//                    if (!skill_predicate<skill_id>(fd, target_status, target_status, s))
//                    {
//                        continue;
//                    }
//
//                    // skip dead target
//                    if (!is_alive(target_status))
//                    {
//                        debug(1, "(CANCELLED: target unit dead) %s Revenge (to %s) %s on %s\n",
//                            status_description(pb_status).c_str(), target_desc,
//                            skill_short_description(fd.getcards, s).c_str(), status_description(target_status).c_str());
//                        continue;
//                    }
//
//                    // TurningTides
//                    if (__builtin_expect(has_turningtides, false))
//                    {
//                        old_attack = target_status.getattack_power();
//                    }
//
//                    // apply revenged skill
//                    debug(1, "%s Revenge (to %s) %s on %s\n",
//                        status_description(pb_status).c_str(), target_desc,
//                        skill_short_description(fd.getcards, s).c_str(), status_description(target_status).c_str());
//                    perform_skill<skill_id>(fd, pb_status, target_status, s);
//                    ++ revenged_count;
//
//                    // revenged TurningTides: get max attack decreasing
//                    if (__builtin_expect(has_turningtides, false))
//                    {
//                        turningtides_value = std.max(turningtides_value, safe_minus(old_attack, target_status.getattack_power()));
//                    }
//                }
//                if (revenged_count)
//                {
//                    // consume remaining payback/revenge
//                    ++ pb_status.getm_paybacked;
//
//                    // apply TurningTides
//                    if (__builtin_expect(has_turningtides, false) && (turningtides_value > 0))
//                    {
//                        SkillSpec ss_rally{Skill.rally, turningtides_value, allfactions, 0, 0, Skill.no_skill, Skill.no_skill, false, 0,};
//                        debug(1, "Paybacked TurningTides %u!\n", turningtides_value);
//                        perform_targetted_allied_fast<Skill.rally>(fd, &fd.getplayers[pb_status.getm_player].getcommander, ss_rally);
//                    }
//                }
//            }
//            // apply Payback
//            else
//            {
//                // skip illegal target(src)
//                if (!skill_predicate<skill_id>(fd, src, src, s))
//                {
//                    continue;
//                }
//
//                // skip dead target(src)
//                if (!is_alive(src))
//                {
//                    debug(1, "(CANCELLED: src unit dead) %s Payback %s on %s\n",
//                        status_description(pb_status).c_str(), skill_short_description(fd.getcards, s).c_str(),
//                        status_description(src).c_str());
//                    continue;
//                }
//
//                // TurningTides
//                if (__builtin_expect(has_turningtides, false))
//                {
//                    old_attack = src.getattack_power();
//                }
//
//                // apply paybacked skill
//                debug(1, "%s Payback %s on %s\n",
//                    status_description(pb_status).c_str(), skill_short_description(fd.getcards, s).c_str(), status_description(src).c_str());
//                perform_skill<skill_id>(fd, pb_status, src, s);
//                ++ pb_status.getm_paybacked;
//
//                // handle paybacked TurningTides
//                if (has_turningtides) {
//                    turningtides_value = Math.max(turningtides_value, safe_minus(old_attack, src.getattack_power()));
//                    if (turningtides_value > 0) {
//                        SkillSpec ss_rally{Skill.rally, turningtides_value, allfactions, 0, 0, Skill.no_skill, Skill.no_skill, false, 0,};
//                        debug(1, "Paybacked TurningTides %u!\n", turningtides_value);
//                        perform_targetted_allied_fast<Skill.rally>(fd, &fd.getplayers[pb_status.getm_player].getcommander, ss_rally);
//                    }
//                }
//            }
//        }
//
//        prepend_on_death(fd);  // paybacked skills
//    }
//    interface Skill {
//        public double perform_targetted_hostile_fast(Skill skill);
//    }

//    private int select_fast(Skill skill, Field fd, CardStatus src, List<CardStatus> cards, SkillSpec s) {
//        switch (skill) {
//        case MEND:
//            fd.selectionArray.clear();
//            boolean critical_reach = fd.getBGEffects(fd.getTapi())[PassiveBGE.CRITICALREACH.ordinal()] != null;
//            List<CardStatus> assaults = fd.getPlayers()[src.getPlayer()].getAssaults();
//            int adj_size = 1 + (critical_reach ? 1 : 0);
//            int host_idx = src.getm_index;
//            int from_idx = safe_minus(host_idx, adj_size);
//            int till_idx = std::min(host_idx + adj_size, safe_minus(assaults.size(), 1));
//            for (; from_idx <= till_idx; ++ from_idx)
//            {
//                if (from_idx == host_idx) { continue; }
//                CardStatus* adj_status = &assaults[from_idx];
//                if (!is_alive(adj_status)) { continue; }
//                if (skill_predicate<Skill::mend>(fd, src, adj_status, s))
//                {
//                    fd.getselection_array.push_back(adj_status);
//                }
//            }
//            return fd.getselection_array.size();
//        default:
//            if ((s.y == allfactions)
//                    || fd.getbg_effects[fd.gettapi][PassiveBGE::metamorphosis]
//                    || fd.getbg_effects[fd.gettapi][PassiveBGE::megamorphosis])
//                {
//                    auto pred = [fd, src, s](CardStatus* c) {
//                        return(skill_predicate<skill_id>(fd, src, c, s));
//                    };
//                    return fd.getmake_selection_array(cards.begin(), cards.end(), pred);
//                }
//                else
//                {
//                    auto pred = [fd, src, s](CardStatus* c) {
//                        return ((c.getm_card.getm_faction == s.y || c.getm_card.getm_faction == progenitor) && skill_predicate<skill_id>(fd, src, c, s));
//                    };
//                    return fd.getmake_selection_array(cards.begin(), cards.end(), pred);
//                }
//            break;
//        }
//    }
//
//    private long select_targets(Skill skill_id, Field fd, CardStatus src, SkillSpec s) {
//        long n_candidates;
//        switch (skill_id) {
//        case BESIEGE:
//            n_candidates = select_fast(Skill.SIEGE, fd, src, skill_targets(Skill.SIEGE, fd, src), s);
//            if (n_candidates == 0) {
//                n_candidates = select_fast(Skill.STRIKE, fd, src, skill_targets(Skill.STRIKE, fd, src), s);
//            }
//            break;
//        default:
//            n_candidates = select_fast(skill_id, fd, src, skill_targets(skill_id, fd, src), s);
//            break;
//        }
//
//        // (false-loop)
//        long n_selected = n_candidates;
//        do
//        {
//            // no candidates
//            if (n_candidates == 0)
//            { break; }
//
//            // show candidates (debug)
//            debugSelection("%s", skill_id.getDescription().c_str());
//
//            // analyze targets count / skill
//            int n_targets = s.n > 0 ? s.n : 1;
//            if (s.all || n_targets >= n_candidates || skill_id == Skill.mend)  // target all or mend
//            { break; }
//
//            // shuffle & trim
//            for (int i = 0; i < n_targets; ++i)
//            {
//                std.swap(fd.getselection_array[i], fd.getselection_array[fd.getrand(i, n_candidates - 1)]);
//            }
//            fd.getselection_array.resize(n_targets);
//            if (n_targets > 1) {
//                Collections.sort(fd.selectionArray, new Comparator<CardStatus>() {
//                    @Override
//                    public int compare(CardStatus o1, CardStatus o2) {
//                        return o1.getIndex() - o2.getIndex();
//                        //return 0;
//                    }});
////                std.sort(fd.getselection_array.begin(), fd.getselection_array.end(),
////                    [](const CardStatus * a, const CardStatus * b) { return a.getm_index < b.getm_index; });
//            }
//            n_selected = n_targets;
//        } while (false); // (end)
//
//        return n_selected;
//    }
    
    void debugSelection(String format, Field fd, String... args) {                                     
        if(Main.debug_print >= 2) {
            debug(2, MessageFormat.format("Possible targets of " + format + ":\n", args));
                for(CardStatus c: fd.selectionArray) {
                    debug(2, "+ %s\n", c.description());
                }
        }                                                              
    }

    private static void debug(int v, String format, String... args) {
        if (Main.debug_print >= v) {
            if (Main.debug_line) {
                System.out.println(MessageFormat.format("%i - " + format, args));
            } else if (Main.debug_cached > 0) {
                Main.debug_str.append(MessageFormat.format(format, args));
            } else {
                System.out.println(MessageFormat.format(format, args));
            }
            System.out.println();
        }
    }

    List<CardStatus> skill_targets(Skill skill, Field fd, CardStatus src) {
        switch (skill) {
        
        case ENFEEBLE:
        case JAM:
        case SIEGE:
        case STRIKE:
        case SUNDER:
        case WEAKEN:
        case MIMIC:
            return fd.getPlayers()[opponent(src.getPlayer())].getAssaults(); // .getassaults.m_indirect)
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
            return fd.getPlayers()[src.getPlayer()].getAssaults(); // .getIndirect()
        default:
            System.err.println("skill_targets: Error: no specialization for " + skill.getDescription() + "\n");
            throw new RuntimeException();
        }
    }
    
//    public static void fillSkillTable() {
//        Skill skillLambda = (skill) .get { perform_targetted_hostile_fast(skill); };  
//        skill_table[Skill.BESIEGE.ordinal()] = perform_targetted_hostile_fast(Skill.BESIEGE);
//        skill_table[Skill.ENFEEBLE.ordinal()] = perform_targetted_hostile_fast<Skill.enfeeble>;
//        skill_table[Skill.ENHANCE.ordinal()] = perform_targetted_allied_fast<Skill.enhance>;
//        skill_table[Skill.EVOLVE.ordinal()] = perform_targetted_allied_fast<Skill.evolve>;
//        skill_table[Skill.HEAL.ordinal()] = perform_targetted_allied_fast<Skill.heal>;
//        skill_table[Skill.JAM.ordinal()] = perform_targetted_hostile_fast<Skill.jam>;
//        skill_table[Skill.MEND.ordinal()] = perform_targetted_allied_fast<Skill.mend>;
//        skill_table[Skill.OVERLOAD.ordinal()] = perform_targetted_allied_fast<Skill.overload>;
//        skill_table[Skill.PROTECT.ordinal()] = perform_targetted_allied_fast<Skill.protect>;
//        skill_table[Skill.RALLY.ordinal()] = perform_targetted_allied_fast<Skill.rally>;
//        skill_table[Skill.ENRAGE.ordinal()] = perform_targetted_allied_fast<Skill.enrage>;
//        skill_table[Skill.ENTRAP.ordinal()] = perform_targetted_allied_fast<Skill.entrap>;
//        skill_table[Skill.RUSH.ordinal()] = perform_targetted_allied_fast_rush;
//        skill_table[Skill.SIEGE.ordinal()] = perform_targetted_hostile_fast<Skill.siege>;
//        skill_table[Skill.STRIKE.ordinal()] = perform_targetted_hostile_fast<Skill.strike>;
//        skill_table[Skill.SUNDER.ordinal()] = perform_targetted_hostile_fast<Skill.sunder>;
//        skill_table[Skill.WEAKEN.ordinal()] = perform_targetted_hostile_fast<Skill.weaken>;
//        skill_table[Skill.MIMIC.ordinal()] = perform_targetted_hostile_fast<Skill.mimic>;
//
//    }

}
