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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.github.tyrantsim.jtuo.util.Utils.safeMinus;

public class FieldSimulator {

    public static int turnLimit = Constants.DEFAULT_TURN_LIMIT;

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

            debug(1, "TURN %d begins for %s", field.getTurn(), field.tap.getCommander().description());

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

            debug(1, "TURN %d ends for %s", field.getTurn(), field.tap.getCommander().description());

            // Swap hand and player index
            int tmpTapi = field.getTapi();
            field.setTapi(field.getTipi());
            field.setTipi(tmpTapi);

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
                debug(1, "%s reduces its timer\n", status.description());
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

        debug(2, "Skills in queue: %d", field.getSkillQueue().size());

        while (!field.getSkillQueue().isEmpty()) {

            Pair<CardStatus, SkillSpec> skillInstance = field.skillQueue.pop();
            CardStatus status = skillInstance.getFirst();
            SkillSpec ss = skillInstance.getSecond();

            debug(2, "%s resolving skill: %s", status.description(), ss.description());

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
            if (SkillUtils.isActivationSkillWithX(modifiedSkill.getId()) && modifiedSkill.getX() == 0) {
                debug(2, "%s failed to %s because its X value is zeroed (sabotaged).",
                        status.description(), ss.description());
            } else {
                autoPerformSkill(modifiedSkill.getId(), field, status, modifiedSkill);
            }
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
        debug(2, "Play card: %s", playedStatus.description());
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
        if (field.hasBGEffect(field.tapi, PassiveBGE.HEROISM)) {
            for (CardStatus dst: field.tap.getAssaults()) {
                int bgeValue = (dst.skill(Skill.VALOR) + 1) / 2;
                if (bgeValue <= 0) continue;
                SkillSpec ssProtect = new SkillSpec(Skill.PROTECT, bgeValue, Faction.ALL_FACTIONS, 0, 0,
                        Skill.NO_SKILL, Skill.NO_SKILL, false, 0, SkillTrigger.ACTIVATE);

                if (dst.getInhibited() > 0) {
                    debug(1, "Heroism: %s on %s but it is inhibited", ssProtect.description(), dst.description());
                    dst.setInhibited(dst.getInhibited() - 1);

                    // Passive BGE: Divert
                    if (field.hasBGEffect(field.tapi, PassiveBGE.DIVERT)) {

                        SkillSpec divertedSS = ssProtect.clone();
                        divertedSS.setY(Faction.ALL_FACTIONS);
                        divertedSS.setN(1);
                        divertedSS.setAll(false);

                        selectTargets(Skill.PROTECT, field, field.tip.getCommander(), divertedSS);
                        for (CardStatus dstProtect : field.selectionArray) {
                            if (dstProtect.getInhibited() > 0) {
                                debug(1, "Heroism: %s (Diverted) on %s but it is inhibited",
                                        divertedSS.description(), dstProtect.description());
                                dstProtect.setInhibited(dstProtect.getInhibited() - 1);
                                continue;
                            }
                            debug(1, "Heroism: %s (Diverted) on %s",
                                    divertedSS.description(), dstProtect.description());
                            performSkill(Skill.PROTECT, field, field.tap.getCommander(), dst, divertedSS); // XXX: the caster
                        }
                    }
                }
                checkAndPerformSkill(Skill.PROTECT, field, field.tap.getCommander(), dst, ssProtect, false);
            }
        }
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
                debug(2, "%s cannot take action.", currentStatus.description());
                // Passive BGE: Halted orders
                int inhibitValue;
                if (field.hasBGEffect(field.tapi, PassiveBGE.HALTEDORDERS)
                        && currentStatus.getDelay() > 0 // still frozen
                        && field.getCurrentCI() < field.tip.getAssaults().size() // across slot isn't empty
                        && field.tip.getAssaults().get(field.getCurrentCI()).isAlive() // across assault is alive
                        && (inhibitValue = currentStatus.skill(Skill.INHIBIT))
                        > field.tip.getAssaults().get(field.getCurrentCI()).getInhibited()) // inhibit/re-inhibit(if higher)
                {
                    CardStatus acrossStatus = field.tip.getAssaults().get(field.getCurrentCI());
                    debug(1, "Halted Orders: %s inhibits %s by %u",
                            currentStatus.description(), acrossStatus.description(), inhibitValue);
                    acrossStatus.setInhibited(inhibitValue);
                }
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
        return 55
                + field.getPlayer(opponent(player)).getTotalCardsDestroyed()
                + field.getPlayer(player).getDeck().getShuffledCards().size()
                - ((field.turn + 7) / 8);
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
                debug(2, "%s reduces timer (%d) of skill %s",
                        status.description(), status.getSkillCd(skill.getId()), skill.getId().toString());
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
                debug(1, "%s loses Valor (no blocker)", src.description());
                return false;
            } else if (dst.getAttackPower() <= src.getAttackPower()) {
                debug(1, "%s loses Valor (weak blocker %s)", src.description(), dst.description());
                return false;
            }

            debug(1, "%s activates Valor %d", src.description(), valorValue);
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

            debug(1, "%s summons %s", status.description(), summonedCard.getName());

            switch (summonedCard.getType()) {
                case ASSAULT:
                case STRUCTURE:
                    return new PlayCard(summonedCard, field, status.getPlayer(), status).op();
                default:
                    debug(0, "Unknown card type: #%d %s: %s", summonedCard.getId(),
                            summonedCard.getName(), summonedCard.getType().toString());
                    throw new AssertionError();
            }
        }

        return null;
    }

    /**
     * @return true if skill is actually performed
     */
    private static boolean checkAndPerformSkill(Skill skillId, Field field, CardStatus src, CardStatus dst,
                                             SkillSpec s, boolean isEvadable) {

        if (skillCheck(field, skillId, dst, src)) {
            if (isEvadable && dst.getEvaded() < dst.skill(Skill.EVADE)) {
                dst.setEvaded(dst.getEvaded() + 1);
                debug(1, "%s %s on %s but it evades",
                        src.description(), s.description(), dst.description());
                return false;
            }
            debug(1, "%s %s on %s", src.description(), s.description(), dst.description());
            performSkill(skillId, field, src, dst, s);
            if (s.getC() > 0)
                src.setSkillCd(skillId, s.getC());

            // Skill: Tribute
            if (skillCheck(field, Skill.TRIBUTE, dst, src)
                    // only activation helpful skills can be tributed (* except Evolve, Enhance, and Rush)
                    && SkillUtils.isActivationHelpfulSkill(s.getId()) && s.getId() != Skill.EVOLVE
                    && s.getId() != Skill.ENHANCE && s.getId() != Skill.RUSH
                    && dst.getTributed() < dst.skill(Skill.TRIBUTE)
                    && skillCheck(field, skillId, src, src)) {

                dst.setTributed(dst.getTributed() + 1);
                debug(1, "%s tributes %s back to %s", dst.description(), s.description(), src.description());
                performSkill(skillId, field, src, src, s);
            }
            return true;
        }
        debug(1, "(CANCELLED) %s %s on %s", src.description(), s.description(), dst.description());
        return false;
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

            for (SkillSpec ss: skills) {
                debug(2, "Skill: %s", ss.description());
                if (!SkillUtils.isActivationSkill(ss.getId())) continue;
                if (status.getSkillCd(ss.getId()) > 0) continue;
                field.addSkillToQueue(status, ss);
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

        // Resolve On-Attacked skills
        for (SkillSpec ss: defStatus.getCard().getSkillsOnAttacked()) {
            debug(1, "On Attacked %s: Preparing (tail) skill %s", defStatus.description(), ss.description());
            field.skillQueue.addLast(new Pair<>(defStatus, ss));
            resolveSkill(field);
        }

        // Enemy Skill: Counter
        if (defStatus.hasSkill(Skill.COUNTER)) {
            // perform skill counter
            int counterDmg = getCounterDamage(field, attStatus, defStatus);
            debug(1, "%s takes %d counter damage from %s", attStatus.description(),
                    counterDmg, defStatus.description());
            removeHP(field, attStatus, counterDmg);
            prependOnDeath(field);
            resolveSkill(field);

            // Passive BGE: Counterflux
            if (field.hasBGEffect(field.tapi, PassiveBGE.COUNTERFLUX)
                    && cardType == CardType.ASSAULT && defStatus.isAlive()) {
                int fluxDenominator = field.getBGEffectValue(field.tapi, PassiveBGE.COUNTERFLUX) > 0
                        ? field.getBGEffectValue(field.tapi, PassiveBGE.COUNTERFLUX) : 4;
                int fluxValue = (defStatus.skill(Skill.COUNTER) - 1) / fluxDenominator + 1;
                debug(1, "Counterflux: %s heals itself and berserks for %d",
                        defStatus.description(), fluxValue);
                defStatus.addHP(fluxValue);
                if (!defStatus.isSundered())
                    defStatus.addPermAttackBuff(fluxValue);
            }

            // Is attacker dead?
            if (!attStatus.isAlive())
                return attDmg;
        }

        // Skill: Corrosive
        int corrosiveValue = defStatus.skill(Skill.CORROSIVE);
        if (corrosiveValue > attStatus.getCorrodedRate()) {
            // perform skill corrosive
            debug(1, "%s corrodes %s by %d", defStatus.description(),
                    attStatus.description(), corrosiveValue);
            attStatus.setCorrodedRate(corrosiveValue);
        }

        // Skill: Berserk
        int berserkValue = attStatus.skill(Skill.BERSERK);
        if (!attStatus.isSundered() && berserkValue > 0) {
            // perform skill berserk
            attStatus.addPermAttackBuff(berserkValue);

            // Passive BGE: Enduring Rage
            if (field.hasBGEffect(field.tapi, PassiveBGE.ENDURINGRAGE)) {
                int bgeDenominator = field.getBGEffectValue(field.tapi, PassiveBGE.ENDURINGRAGE) > 0
                        ? field.getBGEffectValue(field.tapi, PassiveBGE.ENDURINGRAGE) : 2;
                int bgeValue = (berserkValue - 1) / bgeDenominator + 1;
                debug(1, "EnduringRage: %s heals and protects itself for %d",
                        attStatus.description(), bgeValue);
                attStatus.addHP(bgeValue);
                attStatus.setProtectedBy(attStatus.getProtectedBy() + bgeValue);
            }
        }

        // Skill: Leech
        int leechValue = Math.min(attDmg, attStatus.skill(Skill.LEECH));
        if (leechValue > 0 && skillCheck(field, Skill.LEECH, attStatus, null)) {
            debug(1, "%s leeches %d health", attStatus.description(), leechValue);
            attStatus.addHP(leechValue);
        }

        // Passive BGE: Heroism
        int valorValue;
        if (field.hasBGEffect(field.tapi, PassiveBGE.HEROISM)
                && (valorValue = attStatus.skill(Skill.VALOR)) > 0
                && !attStatus.isSundered()
                && cardType == CardType.ASSAULT
                && defStatus.getHP() <= 0) {
            debug(1, "Heroism: %s gain %d attack", attStatus.description(), valorValue);
            attStatus.addPermAttackBuff(valorValue);
        }

        // Passive BGE: Devour
        int leechDevourValue;
        if (field.hasBGEffect(field.tapi, PassiveBGE.DEVOUR)
                && (leechDevourValue = attStatus.skill(Skill.LEECH) + attStatus.skill(Skill.REFRESH)) > 0
                && cardType == CardType.ASSAULT) {

            int bgeDenominator = field.getBGEffectValue(field.tapi, PassiveBGE.DEVOUR) > 0
                    ? field.getBGEffectValue(field.tapi, PassiveBGE.DEVOUR) : 4;
            int bgeValue = (leechDevourValue - 1) / bgeDenominator + 1;
            if (!attStatus.isSundered()) {
                debug(1, "Devour: %s gains %d attack", attStatus.description(), bgeValue);
                attStatus.addPermAttackBuff(bgeValue);
            }
            debug(1, "Devour: %s extends max hp / heals itself for %d",
                    attStatus.description(), bgeValue);
            attStatus.extHP(bgeValue);
        }

        // Skill: Subdue
        int subdueValue = defStatus.skill(Skill.SUBDUE);
        if (subdueValue > 0) {
            debug(1, "%s subdues %s by %d", defStatus.description(),
                    attStatus.description(), subdueValue);
            attStatus.setSubdued(attStatus.getSubdued() + subdueValue);
            if (attStatus.getHP() > attStatus.getMaxHP()) {
                debug(1, "%s loses %d HP due to subdue (max hp: %d)",
                        attStatus.description(), attStatus.getHP() - attStatus.getMaxHP(), attStatus.getMaxHP());
                attStatus.setHP(attStatus.getMaxHP());
            }
        }

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
                byte factionsBitmap = 0;
                for (CardStatus status: attAssaults) {
                    if (!status.isAlive()) continue;
                    factionsBitmap |= (1 << status.getCard().getFaction().ordinal());
                }
                int uniqFactions = Integer.bitCount(factionsBitmap);
                int coalitionValue = coalitionBase * uniqFactions;
                attDmg += coalitionValue;
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

        debug(1, "%s attacks %s for %d damage",
                attStatus.description(), defStatus.description(), preModifierDmg);

        // PassiveBGE: Brigade
        if (field.hasBGEffect(field.tapi, PassiveBGE.BRIGADE) && legionValue > 0 && attStatus.canBeHealed()) {
            debug(1, "Brigade: %s heals itself for %d", attStatus.description(), legionValue);
            attStatus.addHP(legionValue);
        }

        return attDmg;
    }

    private static void attackDamage(Field field, CardType cardType, CardStatus attStatus, CardStatus defStatus, int attDmg) {
        switch (cardType) {
            case COMMANDER:
                removeCommanderHP(field, defStatus, attDmg);
                break;
            default:
                removeHP(field, defStatus, attDmg);
                prependOnDeath(field);
                resolveSkill(field);
        }
    }

    private static void removeCommanderHP(Field field, CardStatus status, int attDmg) {
        debug(2, "%s takes %d damage", status.description(), attDmg);
        status.setHP(safeMinus(status.getHP(), attDmg));
        if (status.getHP() == 0) {
            debug(1, "%s dies -> GAME OVER", status.description());
            field.setEnd(true);
        }
    }

    private static void damageDependantPreOA(Field field, CardStatus attStatus, CardStatus defStatus) {
        // Skill: Poison / Venom
        int poisonValue = Math.max(attStatus.skill(Skill.POISON), attStatus.skill(Skill.VENOM));
        if (poisonValue > defStatus.getPoisoned() && skillCheck(field, Skill.POISON, attStatus, defStatus)) {
            // Perform skill poison
            debug(1, "%s poisons %s by %d", attStatus.description(), defStatus.description(), poisonValue);
            defStatus.setPoisoned(poisonValue);
        }

        // Damage-Dependant Skill: Inhibit
        int inhibitValue = attStatus.skill(Skill.INHIBIT);
        if (inhibitValue > defStatus.getInhibited() && skillCheck(field, Skill.INHIBIT, attStatus, defStatus)) {
            debug(1, "%s inhibits %s by %d", attStatus.description(), defStatus.description(), inhibitValue);
            defStatus.setInhibited(inhibitValue);
        }

        // Damage-Dependant Skill: Sabotage
        int sabotagedValue = attStatus.skill(Skill.SABOTAGE);
        if (sabotagedValue > defStatus.getSabotaged() && skillCheck(field, Skill.SABOTAGE, attStatus, defStatus)) {
            debug(1, "%s sabotages %s by %d", attStatus.description(), defStatus.description(), sabotagedValue);
            defStatus.setSabotaged(sabotagedValue);
        }
    }

    private static void removeHP(Field field, CardStatus status, int dmg) {
        if (dmg == 0) return;
        debug(2, "%s takes %d damage", status.description(), dmg);
        status.setHP(safeMinus(status.getHP(), dmg));
        if (field.getCurrentPhase().ordinal() < FieldPhase.END_PHASE.ordinal() && status.hasSkill(Skill.BARRIER)) {
            field.incDamagedUnitsToTimes(status);
            debug(2, "%s damaged %d times\n", status.description(), field.getDamagedUnitsToTimes(status));
        }
        if (status.getHP() == 0) {
            debug(1, "%s dies", status.description());
            field.killedUnits.add(status);
            field.getPlayer(status.getPlayer()).incTotalCardsDestroyed();
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
        switch (skill) {
            case HEAL: return c.canBeHealed();
            case MEND: return c.canBeHealed();
            case RALLY: return !c.isSundered();
            case OVERLOAD: return c.isActive() && !c.isOverloaded() && !c.hasAttacked();
            case JAM:
                if (field.getTapi() == ref.getPlayer())
                    return c.isActiveNextTurn(); // active player performs Jam
                return c.isActive() && c.getStep() == CardStep.NONE; // inactive player performs Jam
            case LEECH: return c.canBeHealed();
            case COALITION: return c.isActive();
            case PAYBACK: return ref.getCard().getType() == CardType.ASSAULT;
            case REVENGE: return skillCheck(field, Skill.PAYBACK, c, ref);
            case TRIBUTE: return ref.getCard().getType() == CardType.ASSAULT && !c.equals(ref);
            case REFRESH: return c.canBeHealed();
            case DRAIN: return c.canBeHealed();
            default: return SkillUtils.isDefensiveSkill(skill) || c.isAlive();
        }
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

                    debug(1, "%s%s activates Avenge %d", Math.abs(fromIdx - hostIdx) > 1
                            ? "BGE BloodVengeance: " : "", adjStatus.description(), avengeValue);

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
                            debug(1, "Virulence: %s spreads left poison +%d to %s", status.description(),
                                    status.getPoisoned(), leftVirulenceVictim.description());
                            leftVirulenceVictim.setPoisoned(leftVirulenceVictim.getPoisoned() + status.getPoisoned());
                        }
                        stackedPoisonValue += status.getPoisoned();
                        debug(1, "Virulence: %s spreads right poison +%d = %d", status.description(),
                                status.getPoisoned(), stackedPoisonValue);
                    }

                    if (status.getIndex() < assaults.size()) {
                        CardStatus rightStatus = assaults.get(status.getIndex() + 1);
                        if (rightStatus.isAlive()) {
                            debug(1, "Virulence: spreads stacked poison +%d to %s",
                                    stackedPoisonValue, rightStatus.description());
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
                debug(2, "Revenge: Preparing (head) skills  %s and %s", ssHeal.description(), ssRally.description());
                field.skillQueue.addFirst(new Pair<>(commander.clone(), ssRally));
                field.skillQueue.addFirst(new Pair<>(commander.clone(), ssHeal)); // +1: keep ss_heal at first place
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

    private static int getCounterDamage(Field field, CardStatus att, CardStatus def) {
        return safeMinus(def.skill(Skill.COUNTER) + att.getEnfeebled(), att.getProtectedBy());
    }

    private static int opponent(int player) {
        return ((player + 1) % 2);
    }

    /**
     * @return number of selected targets
     */
    private static int selectTargets(Skill skillId, Field field, CardStatus src, SkillSpec s) {
        int numCandidates;
        switch (skillId) {
            case BESIEGE:
                numCandidates = selectFast(Skill.SIEGE, field, src, skillTargets(Skill.SIEGE, field, src), s);
                if (numCandidates == 0)
                    numCandidates = selectFast(Skill.STRIKE, field, src, skillTargets(Skill.STRIKE, field, src), s);
                break;
            default:
                numCandidates = selectFast(skillId, field, src, skillTargets(skillId, field, src), s);
        }

        // (false-loop)
        int numSelected = numCandidates;
        do {
            // no candidates
            if (numCandidates == 0) break;

            // show candidates (debug)
            debugSelection("%s", field, skillId.getDescription());

            // analyze targets count / skill
            int numTargets = s.getN() > 0 ? s.getN() : 1;
            if (s.isAll() || numTargets >= numCandidates || skillId == Skill.MEND) // target all or mend
                break;

            // shuffle
            field.shuffleSelection();

            // trim
            if (field.selectionArray.size() > numTargets)
                field.selectionArray.subList(numTargets, field.selectionArray.size()).clear();

            // sort
            if (numTargets > 1)
                field.selectionArray.sort(Comparator.comparingInt(CardStatus::getIndex));

            numSelected = numTargets;

        } while (false); // (end)

        return numSelected;
    }

    private static int selectFast(Skill skillId, Field field, CardStatus src, List<CardStatus> cards, SkillSpec s) {
        switch (skillId) {
            case MEND:

                field.selectionArray.clear();
                boolean criticalReach = field.hasBGEffect(field.tapi, PassiveBGE.CRITICALREACH);
                List<CardStatus> assaults = field.getPlayer(src.getPlayer()).getAssaults();
                int adjSize = 1 + (criticalReach ? 1 : 0);
                int hostIdx = src.getIndex();
                int fromIdx = safeMinus(hostIdx, adjSize);
                int tillIdx = Math.min(hostIdx + adjSize, safeMinus(assaults.size(), 1));
                for (; fromIdx <= tillIdx; fromIdx++) {
                    if (fromIdx == hostIdx) continue;
                    CardStatus adjStatus = assaults.get(fromIdx);
                    if (!adjStatus.isAlive()) continue;
                    if (skillPredicate(Skill.MEND, field, src, adjStatus, s))
                        field.selectionArray.add(adjStatus);
                }
                return field.selectionArray.size();

            default:

                if (s.getY() == Faction.ALL_FACTIONS
                        || field.hasBGEffect(field.tapi, PassiveBGE.METAMORPHOSIS)
                        || field.hasBGEffect(field.tapi, PassiveBGE.MEGAMORPHOSIS)) {
                    return field.makeSelectionArray(cards, (c) -> skillPredicate(skillId, field, src, c, s));
                } else {
                    return field.makeSelectionArray(cards, (c) ->
                            ((c.getCard().getFaction() == s.getY() || c.getCard().getFaction() == Faction.PROGENITOR)
                                    && skillPredicate(skillId, field, src, c, s)));
                }
        }
    }

    private static boolean skillPredicate(Skill skillId, Field field, CardStatus src, CardStatus dst, SkillSpec s) {
        switch (skillId) {

            case ENHANCE:

                if (!dst.isAlive()) return false;
                if (!dst.hasSkill(s.getS())) return false;
                if (dst.isActive()) return true;
                if (SkillUtils.isDefensiveSkill(s.getS())) return true;

                /* Strange Transmission [Gilians]: strange gillian's behavior implementation:
                 * The Gillian commander and assaults can enhance any skills on any assaults
                 * regardless of jammed/delayed states. But what kind of behavior is in the case
                 * when gilians are played among standard assaults, I don't know. :)
                 */
                return src.isAliveGilian();

            case EVOLVE:

                if (!dst.isAlive()) return false;
                if (!dst.hasSkill(s.getS())) return false;
                if (dst.hasSkill(s.getS2())) return false;
                if (dst.isActive()) return true;
                if (SkillUtils.isDefensiveSkill(s.getS2())) return true;

                /* Strange Transmission [Gilians]: strange gillian's behavior implementation:
                 * The Gillian commander and assaults can enhance any skills on any assaults
                 * regardless of jammed/delayed states. But what kind of behavior is in the case
                 * when gilians are played among standard assaults, I don't know. :)
                 */
                return src.isAliveGilian();

            case MIMIC:

                // skip dead units
                if (!dst.isAlive()) return false;

                // scan all enemy skills until first activation
                for (SkillSpec ss: dst.getCard().getSkills()) {
                    // get skill
                    Skill mimickedSkillId = ss.getId();

                    // skip non-activation skills and Mimic (Mimic can't be mimicked)
                    if (!SkillUtils.isActivationSkill(mimickedSkillId) || mimickedSkillId == Skill.MIMIC)
                        continue;

                    // skip mend for non-assault mimickers
                    if (mimickedSkillId == Skill.MEND && src.getCard().getType() != CardType.ASSAULT)
                        continue;

                    // enemy has at least one activation skill that can be mimicked, so enemy is eligible target for Mimic
                    return true;
                }

                // found nothing (enemy has no skills to be mimicked, so enemy isn't eligible target for Mimic)
                return false;

            case OVERLOAD:

                // basic skill check
                if (!skillCheck(field, Skill.OVERLOAD, dst, src))
                    return false;

                // check skills
                boolean inhibitedSearched = false;
                for (SkillSpec ss: dst.getCard().getSkills()) {

                    // skip cooldown skills
                    if (dst.getSkillCd(ss.getId()) > 0)
                        continue;

                    // get evolved skill
                    Skill evolvedSkillId = Skill.values()[ss.getId().ordinal() + dst.getEvolvedSkillOffset(ss.getId())];

                    // unit with an activation hostile skill is always valid target for OL
                    if (SkillUtils.isActivationHostileSkill(evolvedSkillId))
                        return true;

                    // unit with an activation helpful skill is valid target only when there are inhibited units
                    if (evolvedSkillId != Skill.MEND
                            && SkillUtils.isActivationHelpfulSkill(evolvedSkillId)
                            && !inhibitedSearched) {
                        for (CardStatus c: field.getPlayer(dst.getPlayer()).getAssaults()) {
                            if (c.isAlive() && c.getInhibited() > 0)
                                return true;
                        }
                        inhibitedSearched = true;
                    }
                }
                return false;

            case RALLY:

                return (skillCheck(field, Skill.RALLY, dst, src) // basic skill check
                        && field.tapi == dst.getPlayer()) // is target on the active side
                        ? dst.isActive() && !dst.hasAttacked() // normal case
                        : dst.isActiveNextTurn(); // diverted case / on-death activation

            case ENRAGE:

                return (field.tapi == dst.getPlayer() // is target on the active side?
                        ? dst.isActive() && dst.getStep() == CardStep.NONE // normal case
                        : dst.isActiveNextTurn()) // on-death activation
                        && dst.getAttackPower() > 0; // card can perform direct attack

            case RUSH:

                return !src.isRushAttempted()
                        && dst.getDelay()
                        >= ((src.getCard().getType() == CardType.ASSAULT && dst.getIndex() < src.getIndex()) ? 2 : 1);

            case WEAKEN:
            case SUNDER:

                if (dst.getAttackPower() == 0) return false;

                // active player performs Weaken (normal case)
                if (field.tapi == src.getPlayer())
                    return dst.isActiveNextTurn();

                // inactive player performs Weaken (inverted case (on-death activation))
                return dst.isActive() && !dst.hasAttacked();

            default: return skillCheck(field, skillId, dst, src);
        }
    }

    private static void performSkill(Skill skillId, Field field, CardStatus src, CardStatus dst, SkillSpec s) {
        switch (skillId) {

            case ENFEEBLE:

                dst.setEnfeebled(dst.getEnfeebled() + (int) s.getX());
                break;

            case ENHANCE:

                dst.addEnhancedValue(s.getS().ordinal() + dst.getPrimarySkillOffset()[s.getS().ordinal()], (int) s.getX());
                break;

            case EVOLVE:

                int primaryS1 = dst.getPrimarySkillOffset()[s.getS().ordinal()] + s.getS().ordinal();
                int primaryS2 = dst.getPrimarySkillOffset()[s.getS2().ordinal()] + s.getS2().ordinal();
                dst.setPrimarySkillOffset(s.getS().ordinal(), primaryS2 - s.getS().ordinal());
                dst.setPrimarySkillOffset(s.getS2().ordinal(), primaryS1 - s.getS2().ordinal());
                dst.setEvolvedSkillOffset(primaryS1, s.getS2().ordinal() - primaryS1);
                dst.setEvolvedSkillOffset(primaryS2, s.getS().ordinal() - primaryS2);
                break;

            case HEAL:

                dst.addHP((int) s.getX());

                // Passive BGE: Zealot's Preservation
                if (field.hasBGEffect(field.tapi, PassiveBGE.ZEALOTSPRESERVATION)
                        && src.getCard().getType() == CardType.ASSAULT) {
                    int bgeValue = (int) ((s.getX() + 1) / 2);
                    debug(1, "Zealot's Preservation: %s Protect %d on %s",
                            src.description(), bgeValue, dst.description());
                    dst.setProtectedBy(dst.getProtectedBy() + bgeValue);
                }
                break;

            case JAM:

                dst.setJammed(true);
                break;

            case MEND:

                dst.addHP((int) s.getX());
                break;

            case BESIEGE:

                if (dst.getCard().getType() == CardType.STRUCTURE) {
                    removeHP(field, dst, (int) s.getX());
                } else {
                    int strikeDmg = safeMinus((int) ((s.getX() + 1) / 2 + dst.getEnfeebled()),
                            src.isOverloaded() ? 0 : dst.protectedValue());
                    removeHP(field, dst, strikeDmg);
                }
                break;

            case OVERLOAD:

                dst.setOverloaded(true);
                break;

            case PROTECT:

                dst.setProtectedBy(dst.getProtectedBy() + (int) s.getX());
                break;

            case RALLY:

                dst.addTempAttackBuff((int) s.getX());
                break;

            case ENRAGE:

                dst.setEnraged(dst.getEnraged() + (int) s.getX());

                // Passive BGE: Furiosity
                if (field.hasBGEffect(field.tapi, PassiveBGE.FURIOSITY) && dst.canBeHealed()) {
                    int bgeValue = (int) s.getX();
                    debug(1, "Furiosity: %s Heals %s for %d",
                            src.description(), dst.description(), bgeValue);
                    dst.addHP(bgeValue);
                }
                break;

            case ENTRAP:

                dst.setEntrapped(dst.getEntrapped() + (int) s.getX());
                break;

            case RUSH:

                dst.setDelay(dst.getDelay() - Math.min(Math.max((int) s.getX(), 1), dst.getDelay()));
                if (dst.getDelay() == 0) {
                    checkAndPerformValor(field, dst);
                    checkAndPerformSummon(field, dst);
                }
                break;

            case SIEGE:

                removeHP(field, dst, (int) s.getX());
                break;

            case STRIKE:

                int strikeDmg = safeMinus((int) ((s.getX() + 1) / 2 + dst.getEnfeebled()),
                        src.isOverloaded() ? 0 : dst.protectedValue());
                removeHP(field, dst, strikeDmg);
                break;

            case WEAKEN:

                dst.addTempAttackBuff(-Math.min((int) s.getX(), dst.getAttackPower()));
                break;

            case SUNDER:

                dst.setSundered(true);
                performSkill(Skill.WEAKEN, field, src, dst, s);
                break;

            case MIMIC:

                List<SkillSpec> mimickableSkills = new ArrayList<>(dst.getCard().getSkills().size());
                debug(2, " * Mimickable skills of %s", dst.description());
                for (SkillSpec ss: dst.getCard().getSkills()) {

                    // get skill
                    Skill mimickableSkillId = ss.getId();

                    // skip non-activation skills and Mimic (Mimic can't be mimicked)
                    if (!SkillUtils.isActivationSkill(mimickableSkillId) || mimickableSkillId == Skill.MIMIC)
                        continue;

                    // skip mend for non-assault mimickers
                    if (mimickableSkillId == Skill.MEND && src.getCard().getType() != CardType.ASSAULT)
                        continue;

                    mimickableSkills.add(ss.clone());
                    debug(2, "  + %s", ss.description());

                }

                // select skill
                int mimIdx = 0;
                switch (mimickableSkills.size()) {
                    case 0: assert(false); break;
                    case 1: break;
                    default: mimIdx = (field.getRandom().nextInt(mimickableSkills.size()));
                }

                final SkillSpec mimSS = mimickableSkills.get(mimIdx);
                Skill mimSkillId = mimSS.getId();
                int skillValue = (int) s.getX() + src.getEnhanced(mimSkillId);
                SkillSpec mimickedSS = new SkillSpec(mimSkillId, skillValue, Faction.ALL_FACTIONS, mimSS.getN(), 0,
                        mimSS.getS(), mimSS.getS2(), mimSS.isAll(), mimSS.getCardId(), SkillTrigger.ACTIVATE);
                debug(1, " * Mimicked skill: %s", mimickedSS.description());
                performTargettedHostileFast(Skill.MIMIC, field, src, mimickedSS);
                break;

            default: assert(false);
        }
    }

    private static void performTargettedAlliedFast(Skill skillId, Field field, CardStatus src, SkillSpec s) {

        selectTargets(skillId, field, src, s);
        int numInhibited = 0;
        for (CardStatus dst: field.selectionArray) {
            if (dst.getInhibited() > 0 && !src.isOverloaded()) {
                debug(1, "%s %s on %s but it is inhibited", src.description(),
                        s.description(), dst.description());
                dst.setInhibited(dst.getInhibited() - 1);
                numInhibited++;
                continue;
            }
            checkAndPerformSkill(skillId, field, src, dst, s, false);
        }

        // Passive BGE: Divert
        if (field.hasBGEffect(field.tapi, PassiveBGE.DIVERT) && numInhibited > 0) {

            SkillSpec divertedSS = s.clone();
            divertedSS.setY(Faction.ALL_FACTIONS);
            divertedSS.setN(1);
            divertedSS.setAll(false);

            for (int i = 0; i < numInhibited; i++) {
                selectTargets(skillId, field, field.getTip().getCommander(), divertedSS);
                for (CardStatus dst: field.selectionArray) {
                    if (dst.getInhibited() > 0) {
                        debug(1, "%s %s (Diverted) on %s but it is inhibited", src.description(),
                                divertedSS.description(), dst.description());
                        dst.setInhibited(dst.getInhibited() - 1);
                        continue;
                    }
                    debug(1, "%s %s (Diverted) on %s", src.description(),
                            divertedSS.description(), dst.description());
                    performSkill(skillId, field, src, dst, divertedSS);
                }
            }
        }

    }

    private static void performTargettedAlliedFastRush(Field field, CardStatus src, SkillSpec s) {
        if (src.getCard().getType() == CardType.COMMANDER) {
            // Passive BGE skills are casted as by commander
            performTargettedAlliedFast(Skill.RUSH, field, src, s);
            return;
        }
        if (src.isRushAttempted()) {
            debug(2, "%s does not check Rush again.", src.description());
            return;
        }
        debug(1, "%s attempts to activate Rush.", src.description());
        performTargettedAlliedFast(Skill.RUSH, field, src, s);
        src.setRushAttempted(true);
    }

    private static void performTargettedHostileFast(Skill skillId, Field field, CardStatus src, SkillSpec s) {

        selectTargets(skillId, field, src, s);
        List<CardStatus> paybackers = new ArrayList<>();
        boolean hasTurningTides = field.hasBGEffect(field.tapi, PassiveBGE.TURNINGTIDES)
                && (skillId == Skill.WEAKEN || skillId == Skill.SUNDER);
        int turningTidesValue = 0, oldAttack = 0;

        // apply skill to each target(dst)
        for (CardStatus dst: field.selectionArray) {

            // TurningTides
            if (hasTurningTides) oldAttack = dst.getAttackPower();

            // check & apply skill to target(dst)
            if (checkAndPerformSkill(skillId, field, src, dst, s, !src.isOverloaded())) {
                // TurningTides: get max attack decreasing
                turningTidesValue = Math.max(turningTidesValue, safeMinus(oldAttack, dst.getAttackPower()));

                // Payback/Revenge: collect paybackers/revengers
                int paybackValue = dst.skill(Skill.PAYBACK) + dst.skill(Skill.REVENGE);
                if (s.getId() != Skill.MIMIC && dst.getPaybacked() < paybackValue && skillCheck(field, Skill.PAYBACK, dst, src))
                    paybackers.add(dst);
            }
        }

        // apply TurningTides
        if (hasTurningTides && turningTidesValue > 0) {
            SkillSpec ssRally = new SkillSpec(Skill.RALLY, turningTidesValue, Faction.ALL_FACTIONS, 0, 0,
                    Skill.NO_SKILL, Skill.NO_SKILL, s.isAll(), 0, SkillTrigger.ACTIVATE);
            debug(1, "TurningTides %d!", turningTidesValue);
            performTargettedAlliedFast(Skill.RALLY, field, field.getPlayer(src.getPlayer()).getCommander(), ssRally);
        }

        prependOnDeath(field); // skills

        // Payback/Revenge
        for (CardStatus pbStatus: paybackers) {
            turningTidesValue = 0;

            // Apply Revenge
            if (pbStatus.skill(Skill.REVENGE) > 0) {
                int revengedCount = 0;
                for (int caseIdx = 0; caseIdx < 3; caseIdx++) {
                    // Get revenge target
                    CardStatus targetStatus = null;
                    switch (caseIdx) {
                        case 0:
                            // Revenge to left
                            if ((targetStatus = field.getLeftAssault(src)) == null)
                                continue;
                            break;
                        case 1:
                            // Revenge to core
                            targetStatus = src;
                            break;
                        case 2:
                            // Revenge to right
                            if ((targetStatus = field.getRightAssault(src)) == null)
                                continue;
                            break;
                    }

                    // Skip illegal target
                    if (!skillPredicate(skillId, field, targetStatus, targetStatus, s))
                        continue;

                    // Skip dead target
                    if (!targetStatus.isAlive()) {
                        debug(1, "(CANCELLED: target unit dead) %s Revenge (to %s) %s on %s",
                                pbStatus.description(), caseIdx == 0 ? "left" : caseIdx == 1 ? "core" : "right",
                                s.description(), targetStatus.description());
                        continue;
                    }

                    // TurningTides
                    if (hasTurningTides) oldAttack = targetStatus.getAttackPower();

                    // Apply revenged skill
                    debug(1, "%s Revenge (to %s) %s on %s",
                            pbStatus.description(), caseIdx == 0 ? "left" : caseIdx == 1 ? "core" : "right",
                            s.description(), targetStatus.description());
                    performSkill(skillId, field, pbStatus, targetStatus, s);
                    revengedCount++;

                    // Revenged TurningTides: get max attack decreasing
                    if (hasTurningTides)
                        turningTidesValue = Math.max(turningTidesValue,
                                safeMinus(oldAttack, targetStatus.getAttackPower()));
                }

                if (revengedCount > 0) {

                    // Consume remaining Payback/Revenge
                    pbStatus.setPaybacked(pbStatus.getPaybacked() + 1);

                    // Apply TurningTides
                    if (hasTurningTides && turningTidesValue > 0) {
                        SkillSpec ssRally = new SkillSpec(Skill.RALLY, turningTidesValue, Faction.ALL_FACTIONS,
                                0, 0, Skill.NO_SKILL, Skill.NO_SKILL, false, 0, SkillTrigger.ACTIVATE);
                        debug(1, "Paybacked TurningTides %d!", turningTidesValue);
                        performTargettedAlliedFast(Skill.RALLY, field,
                                field.getPlayer(pbStatus.getPlayer()).getCommander(), ssRally);
                    }
                }
            } else { // Apply Payback

                // Skip illegal target
                if (!skillPredicate(skillId, field, src, src, s))
                    continue;

                // Skip dead target
                if (src.isAlive()) {
                    debug(1, "(CANCELLED: src unit dead) %s Payback %s on %s",
                            pbStatus.description(), s.description(), src.description());
                    continue;
                }

                // TurningTides
                if (hasTurningTides) oldAttack = src.getAttackPower();

                // Apply paybacked skill
                debug(1, "%s Payback %s on %s", pbStatus.description(), s.description(), src.description());
                performSkill(skillId, field, pbStatus, src, s);
                pbStatus.setPaybacked(pbStatus.getPaybacked() + 1);

                // handle paybacked TurningTides
                if (hasTurningTides) {
                    turningTidesValue = Math.max(turningTidesValue, safeMinus(oldAttack, src.getAttackPower()));
                    if (turningTidesValue > 0) {
                        SkillSpec ssRally = new SkillSpec(Skill.RALLY, turningTidesValue, Faction.ALL_FACTIONS,
                                0, 0, Skill.NO_SKILL, Skill.NO_SKILL, false, 0, SkillTrigger.ACTIVATE);
                        debug(1, "Paybacked TurningTides %d!", turningTidesValue);
                        performTargettedAlliedFast(Skill.RALLY, field,
                                field.getPlayer(pbStatus.getPlayer()).getCommander(), ssRally);
                    }
                }
            }
        }
        prependOnDeath(field);
    }

    private static void debugSelection(String format, Field fd, Object... args) {
        if(Main.debug_print >= 2) {
            debug(2, MessageFormat.format("Possible targets of " + format + ":", args));
                for(CardStatus c: fd.selectionArray) {
                    debug(2, "+ %s", c.description());
                }
        }                                                              
    }

    private static void debug(int v, String format, Object... args) {
        if (Main.debug_print >= v) {
            if (Main.debug_line) {
                System.out.printf("debug: " + format, args);
                //System.out.println(MessageFormat.format("%i - " + format, args));
            } else if (Main.debug_cached > 0) {
                Main.debug_str.append(MessageFormat.format(format, args));
            } else {
                System.out.println(MessageFormat.format(format, args));
            }
            System.out.println();
        }
    }

    private static List<CardStatus> skillTargets(Skill skill, Field fd, CardStatus src) {
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
                System.err.println("skill_targets: Error: no specialization for " + skill.getDescription() + "");
                throw new RuntimeException();
        }
    }

    private static void autoPerformSkill(Skill skill, Field field, CardStatus status, SkillSpec ss) {
        switch (skill) {
            case BESIEGE: performTargettedHostileFast(skill, field, status, ss); break;
            case ENFEEBLE: performTargettedHostileFast(skill, field, status, ss); break;
            case ENHANCE: performTargettedAlliedFast(skill, field, status, ss); break;
            case EVOLVE: performTargettedAlliedFast(skill, field, status, ss); break;
            case HEAL: performTargettedAlliedFast(skill, field, status, ss); break;
            case JAM: performTargettedHostileFast(skill, field, status, ss); break;
            case MEND: performTargettedAlliedFast(skill, field, status, ss); break;
            case OVERLOAD: performTargettedAlliedFast(skill, field, status, ss); break;
            case PROTECT: performTargettedAlliedFast(skill, field, status, ss); break;
            case RALLY: performTargettedAlliedFast(skill, field, status, ss); break;
            case ENRAGE: performTargettedAlliedFast(skill, field, status, ss); break;
            case ENTRAP: performTargettedAlliedFast(skill, field, status, ss); break;
            case RUSH: performTargettedAlliedFastRush(field, status, ss); break;
            case SIEGE: performTargettedHostileFast(skill, field, status, ss); break;
            case STRIKE: performTargettedHostileFast(skill, field, status, ss); break;
            case SUNDER: performTargettedHostileFast(skill, field, status, ss); break;
            case WEAKEN: performTargettedHostileFast(skill, field, status, ss); break;
            case MIMIC: performTargettedHostileFast(skill, field, status, ss); break;
            default:
                System.err.println("autoPerformSkill: Error: no specialization for " + skill.getDescription() + "");
                throw new RuntimeException();
        }
    }

}
