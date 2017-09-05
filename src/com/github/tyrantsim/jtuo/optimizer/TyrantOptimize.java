package com.github.tyrantsim.jtuo.optimizer;

import java.awt.PageAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.math3.stat.interval.ClopperPearsonInterval;
import org.apache.commons.math3.stat.interval.ConfidenceInterval;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.control.EvaluatedResults;
import com.github.tyrantsim.jtuo.control.SimProcess;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.DeckStrategy;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.sim.OptimizationMode;
import com.github.tyrantsim.jtuo.sim.Results;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;
import com.github.tyrantsim.jtuo.util.Utils;

public class TyrantOptimize {

    public static boolean DEBUG = false;
    
    OptimizationMode optimization_mode = OptimizationMode.NOT_SET;
    
    public static boolean modeOpenTheDeck = false;
    public static OptimizationMode optimizationMode = OptimizationMode.NOT_SET;
    final Card owned_alpha_dominion = null;
    boolean use_owned_cards = true;
    
    public int min_deck_len = 1;
    public int max_deck_len = 10;
    public int freezed_cards = 0;

    
    public static int fund = 0;
    public double target_score = 100;
    public double min_increment_of_score = 0;
    
    public static boolean use_top_level_card = true;
    public static boolean use_top_level_commander = true;

    private static class Requirement {
        public Map<Card, Integer> num_cards = new HashMap<>();
    };
    
    Requirement requirement;
    
    public static double confidenceLevel = 0.99;
    
    boolean show_ci = false;
    
    public static boolean useHarmonicMean = false;
    int min_possible_score[] = {0, 0, 0, 10, 5, 5, 5, 0};
    int  max_possible_score[] = {100, 100, 100, 100, 65, 65, 100, 100};
    
    public HashMap<Integer, Integer> owned_cards = new HashMap<>();
    
    private String card_id_name(Card card) {
        StringBuilder ios = new StringBuilder(30);
        if (card != null) {
            ios.append("[" + card.getId() + "] " + card.getName());
        } else {
            ios.append("-void-");
        }
        return ios.toString();
    }
    
    String card_slot_id_names(List<Pair<Integer, Card>> card_list) {
        if (card_list.isEmpty()) {
            return "-void-";
        }
        StringBuilder ios = new StringBuilder(200);
        String separator = "";
        for (Pair<Integer, Card> card_it : card_list) {
            ios.append(separator);
            separator = ", ";
            if (card_it.getFirst() >= 0) ios.append(card_it.getFirst() + " ");
            ios.append("[" + card_it.getSecond().getId() + "] " + card_it.getSecond().getName());
        }
        return ios.toString();
    }
    
    public Deck findDeck(Decks decks, Cards allCards, String deckName)
    {
        Deck deck = Decks.findDeckByName(deckName);
        if (deck != null) {
            deck.resolve();
            return deck;
        }
        // TODO ???
        //deck  decks.decks.emplace_back(Deck{all_cards});
        deck = Decks.decks.get(0);
        //deck.gde.getset(deck_name);
        deck.resolve();
        return(deck);
    }
    
  //---------------------- $80 deck optimization ---------------------------------
    int getRequiredCardsBeforeUpgrade(HashMap<Integer, Integer> owned_cards, List<Card> card_list, Map<Card, Integer> num_cards) {
        // TODO: to implement
        int deck_cost = 0;
        Stack<Card> unresolvedCards = new Stack<>();
        for (Card card : card_list) {
            num_cards.put(card, num_cards.get(card).intValue() + 1);
            unresolvedCards.add(card);
        }
        // un-upgrade according to type/category
        // * use fund for normal cards
        // * use only top-level cards for initial (basic) dominion (Alpha Dominion) and dominion material (Dominion Shard)
        while (!unresolvedCards.isEmpty()) {
            // pop next unresolved card
            Card card = unresolvedCards.pop();

            // assume unlimited common/rare level-1 cards (standard set)
            if ((card.getSet() == 1000) && (card.getRarity() <= 2) && (card.isLowLevelCard())) continue;

            // keep un-defused (top-level) basic dominion & its material
            if ((card.getId() == 50002) || (card.getCategory() == CardCategory.DOMINION_MATERIAL)) continue;

            // defuse if inventory lacks required cards and recipe is not empty
            if ((fund > 0 || (card.getCategory() != CardCategory.NORMAL)) && (owned_cards.get(card.getId()) < num_cards.get(card) && !card.getRecipeCards().isEmpty())) {
                int num_under = num_cards.get(card) - owned_cards.get(card.getId());
                num_cards.put(card, owned_cards.get(card.getId()));

                // do count cost (in SP) only for normal cards
                if (card.getCategory() == CardCategory.NORMAL) {
                    deck_cost += num_under * card.getRecipeCost();
                }

                // enqueue recipe cards as unresolved
                for (Entry<Card, Integer> recipe_card : card.getRecipeCards().entrySet()) {
                    int new_cost = num_cards.get(recipe_card.getKey()) + num_under * recipe_card.getValue();
                    num_cards.put(recipe_card.getKey(), new_cost);
                    unresolvedCards.push(recipe_card.getKey());
                }
            }
        }
        return deck_cost;
    }

    Integer getRequiredCardsBeforeUpgrade(List<Card> card_list, Map<Card, Integer> num_cards) {
        return getRequiredCardsBeforeUpgrade(owned_cards, card_list, num_cards);
    }
    
    public int get_deck_cost(Deck deck){
        if (!use_owned_cards) { return 0; }
        Map<Card, Integer> num_in_deck = new HashMap<>();
        int deck_cost = 0;
        if (deck.getCommander() != null) { 
            deck_cost += getRequiredCardsBeforeUpgrade(Arrays.asList(new Card[] {deck.getCommander()}), num_in_deck); // {deck.getCommander()}, num_in_deck); 
        }
        deck_cost += getRequiredCardsBeforeUpgrade(deck.getCards(), num_in_deck);
        for (Entry<Card, Integer> it: num_in_deck.entrySet())
        {
            Integer card_id = it.getKey().getId();
            if (it.getValue() > owned_cards.get(card_id));
            { return Integer.MAX_VALUE; }
        }
        return deck_cost;
    }
    
 // insert card at to_slot into deck limited by fund; store deck_cost
 // return true if affordable
    boolean adjustDeck(Deck deck, int from_slot, int to_slot, final Card card, int fund, Object rndGenerator, int deck_cost, List<Pair<Integer, Card>> cards_out, List<Pair<Integer, Card>> cards_in) {
     boolean is_random = deck.getStrategy() == DeckStrategy.RANDOM;
     cards_out.clear();
     cards_in.clear();
     if (from_slot < 0)
     {
         if (card.getCategory() == CardCategory.DOMINION_ALPHA){ 
             // change alpha dominion
             cards_out.add(new Pair<>(-1, deck.getAlphaDominion()));
             deck.setAlphaDominion(card);
             cards_in.add(new Pair<>(-1, deck.getAlphaDominion()));
             deck_cost = get_deck_cost(deck);
             return true;
         }

         // change commander
         cards_out.add(new Pair<>(-1, deck.getCommander()));
         deck.setCommander(card);
         cards_in.add(new Pair<>(-1, deck.getCommander()));
         deck_cost = get_deck_cost(deck);
         return (deck_cost <= fund);
     }
     if (from_slot < deck.getCards().size()) {
         // remove card from the deck
         cards_out.add(new Pair<>(is_random ? -1 : from_slot, deck.getCards().get(from_slot))); // cards_out.emplace_back(is_random ? -1 : from_slot, deck.getcards[from_slot]); // 
         deck.getCards().remove(from_slot);
     }
     if (card == null) { // remove card (no new replacement for removed card)
         deck_cost = get_deck_cost(deck);
         return (deck_cost <= fund);
     }

     // backup deck cards
     Card old_commander = deck.getCommander();
     List<Card> cards = deck.getCards();

     // try to add new card into the deck, downgrade it if necessary
     {
         Card candidate_card = card;
         deck.setCommander(null);
         deck.getCards().clear();
         //deck.getcards.emplace_back(card);
         deck_cost = get_deck_cost(deck);
         if (!use_top_level_card && (deck_cost > fund)) {
             while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                 candidate_card = candidate_card.downgraded();
                 deck.getCards().set(0, candidate_card);
                 deck_cost = get_deck_cost(deck);
             }
         }
         if (deck_cost > fund) return false;
         cards_in.add(new Pair<>(is_random ? -1 : to_slot, deck.getCards().get(0)));
     }

     // try to add commander into the deck, downgrade it if necessary
     {
         Card candidate_card = old_commander;
         deck.setCommander(candidate_card);
         deck_cost = get_deck_cost(deck);
         if (!use_top_level_commander && (deck_cost > fund))
         {
             while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                 candidate_card = candidate_card.downgraded();
                 deck.setCommander(candidate_card);
                 deck_cost = get_deck_cost(deck);
             }
         }
         if (deck_cost > fund) return false;
         if (deck.getCommander() != old_commander){
             // TODO: replace C++ code
             //append_unless_remove(cards_out, cards_in, {-1, old_commander});
             //append_unless_remove(cards_in, cards_out, {-1, deck.getCommander()});
         }
     }

     // added backuped deck cards back (place cards strictly before/after card inserted above according to slot index)
     for (int i = 0; i < cards.size(); ++i) {
         // try to add cards[i] into the deck, downgrade it if necessary
         Card candidate_card = cards.get(i);
         //Card in_it = deck.getCards().get(deck.getCards().size() - (i < to_slot ? 1 : 0)); //  ??? .getcards.end() - (i < to_slot); // (before/after according to slot index)
         if (i < to_slot) {
             deck.getCards().add(deck.getCards().size() - 1, candidate_card);
         } else {
             deck.getCards().add(candidate_card);             
         }

         deck_cost = get_deck_cost(deck);
         if (!use_top_level_card && (deck_cost > fund)) {
             while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                 candidate_card = candidate_card.downgraded();
                 //in_it = candidate_card;
                 deck_cost = get_deck_cost(deck);
             }
         }
         if (deck_cost > fund) return false;
         if (candidate_card != cards.get(i)) {
             // TODO: replace C++ code
             //append_unless_remove(cards_out, cards_in, {is_random ? -1 : i + (i >= from_slot), cards[i]});
             //append_unless_remove(cards_in, cards_out, {is_random ? -1 : i + (i >= to_slot), *in_it});
         }
     }
     return !cards_in.isEmpty() || !cards_out.isEmpty();
 }

 int check_requirement(Deck deck, Requirement requirement) {
     int gap = Utils.safeMinus(min_deck_len, deck.getCards().size());
     if (!requirement.num_cards.isEmpty()) {
         Map<Card, Integer> num_cards = new HashMap<>();
         num_cards.put(deck.getCommander(), 1);
         for (Card card: deck.getCards())
         {
             num_cards.put(card, num_cards.get(card) + 1);
         }
         for (Entry<Card, Integer> entry: requirement.num_cards.entrySet()) {
             gap += Utils.safeMinus(entry.getValue(), num_cards.get(entry.getKey()));
         }
     }
     return gap;
 }
    
    public void claimCards(ArrayList<Card> card_list) {
            TreeMap<Card, Integer> num_cards = new TreeMap<>();
            getRequiredCardsBeforeUpgrade(card_list, num_cards);
            for (Entry<Card, Integer> pair : num_cards.entrySet()) {
                Card card = pair.getKey();
                Integer num_to_claim = Utils.safeMinus(pair.getValue(), owned_cards.get(card.getId()));
                if (num_to_claim > 0) {
                    owned_cards.put(card.getId(), owned_cards.get(card.getId()) + num_to_claim);
                    if (DEBUG) {
                        System.out.println("WARNING: Need extra " + num_to_claim  + " " + card.getName() + " to build your initial deck: adding to owned card list.\n");
                    }
                }
            }
        }
    
        
    public Results computeScore(EvaluatedResults results, List<Double> factors) {
        Results last = new Results(0l, 0l, 0l, 0l); //, 0, 0, 0, 0, 0, results.second};
        double max_possible = max_possible_score[optimizationMode.ordinal()];
        for (int index = 0; index < results.getResults().size(); ++index) {
            last.wins += results.getResults().get(index).wins * factors.get(index);
            last.draws += results.getResults().get(index).draws * factors.get(index);
            last.losses += results.getResults().get(index).losses * factors.get(index);
            //results.second, results.first.get(index).points / max_possible, 1 - confidence_level
            ConfidenceInterval confidenceInterval = new ClopperPearsonInterval().createInterval(results.getTotalBattles(), (int)Math.round(results.getResults().get(index).points / max_possible), confidenceLevel); //new BinomialDistribution(results.getTotalBattles(), 1 - confidenceLevel);
            // results.getResults().get(index).points / max_possible
            double lower_bound = confidenceInterval.getLowerBound() * max_possible; // find_lower_bound_on_p() * max_possible;
            double upper_bound = confidenceInterval.getUpperBound() * max_possible; //new BinomialDistribution(results.getTotalBattles(), results.getResults().get(index).points / max_possible, 1 - confidenceLevel).getSupportUpperBound() * max_possible;
            if (useHarmonicMean)
            {
                last.points += factors.get(index) / results.getResults().get(index).points;
                last.points_lower_bound += factors.get(index) / lower_bound;
                last.points_upper_bound += factors.get(index) / upper_bound;
            }
            else
            {
                last.points += results.getResults().get(index).points * factors.get(index);
                last.points_lower_bound += lower_bound * factors.get(index);
                last.points_upper_bound += upper_bound * factors.get(index);
            }
        }
        double factorSum = 0;
        for (int i = 0; i < factors.size(); i++) {
            factorSum += factors.get(i); 
        }

        last.wins /= factorSum * ( double)results.getTotalBattles();
        last.draws /= factorSum * ( double)results.getTotalBattles();
        last.losses /= factorSum * ( double)results.getTotalBattles();
        if (useHarmonicMean)
        {
            last.points = (long)factorSum / (results.getTotalBattles() * last.points);
            last.points_lower_bound = factorSum / last.points_lower_bound;
            last.points_upper_bound = factorSum / last.points_upper_bound;
        }
        else
        {
            last.points /= factorSum * results.getTotalBattles();
            last.points_lower_bound /= factorSum;
            last.points_upper_bound /= factorSum;
        }
        return last;
    }
    
    
  //------------------------------------------------------------------------------
    boolean tryImproveDeck(Deck d1, int from_slot, int to_slot, Card card_candidate,
            Card best_commander, Card best_alpha_dominion, List<Card> best_cards,
            Results best_score, int best_gap, String best_deck,
            Map<String, EvaluatedResults> evaluated_decks, EvaluatedResults zero_results,
            long skipped_simulations, SimProcess proc) {
        int deck_cost = 0;
        List<Pair<Integer, Card>> cards_out = new ArrayList<>(), cards_in = new ArrayList<>();
        //std.mt19937& re = proc.threads_data[0].getre;

        // setup best deck
        d1.setCommander(best_commander);
        d1.setAlphaDominion(best_alpha_dominion);
        d1.setCards(best_cards);

        Object re = null;
        // try to adjust the deck
        if (!adjustDeck(d1, from_slot, to_slot, card_candidate, fund, re, deck_cost, cards_out, cards_in))
        { return false; }

        // check gap
        int new_gap = check_requirement(d1, requirement);
        if ((new_gap > 0) && (new_gap >= best_gap))
        { return false; }

        // check previous simulations
        String cur_deck = d1.hash();
        EvaluatedResults prev_results = null;
        if (evaluated_decks.get(cur_deck) == null) {
            evaluated_decks.put(cur_deck, zero_results);
            prev_results = zero_results;
        } else {
            prev_results = evaluated_decks.get(cur_deck);
            skipped_simulations += prev_results.getTotalBattles();
        }

        // Evaluate new deck
        EvaluatedResults compare_results = proc.compare(best_score.n_sims, prev_results, best_score);
        Results current_score = computeScore(compare_results, proc.getFactors());

        // Is it better ?
        if (new_gap < best_gap || current_score.points > best_score.points + min_increment_of_score)
        {
            // Then update best score/slot, print stuff
            System.out.println("Deck improved: " + d1.hash() + ": " + card_slot_id_names(cards_out) + " .get " + card_slot_id_names(cards_in) + ": ");
            best_gap = new_gap;
            best_score = current_score;
            best_deck = cur_deck;
            best_commander = d1.getCommander();
            best_alpha_dominion = d1.getAlphaDominion();
            best_cards = d1.getCards();
            print_score_info(compare_results, proc.getFactors());
            print_deck_inline(deck_cost, best_score, d1);
            return true;
        }

        return false;
    }
//    //------------------------------------------------------------------------------
//    void hill_climbing(int num_min_iterations, int num_iterations, Deck* d1, Process& proc, Requirement & requirement
//    #ifndef NQUEST
//        , Quest & quest
//    #endif
//    )
//    {
//        EvaluatedResults zero_results = { EvaluatedResults.first_type(proc.enemy_decks.size()), 0 };
//        std.string best_deck = d1.gethash();
//        std.unordered_map<std.string, EvaluatedResults> evaluated_decks{{best_deck, zero_results}};
//        EvaluatedResults& results = proc.evaluate(num_min_iterations, evaluated_decks.begin().getsecond);
//        print_score_info(results, proc.factors);
//        FinalResults<long double> best_score = compute_score(results, proc.factors);
//        const Card* best_commander = d1.getcommander;
//        const Card* best_alpha_dominion = d1.getalpha_dominion;
//        std.vector<const Card*> best_cards = d1.getcards;
//        int deck_cost = get_deck_cost(d1);
//        fund = std.max(fund, deck_cost);
//        print_deck_inline(deck_cost, best_score, d1);
//        std.mt19937& re = proc.threads_data[0].getre;
//        int best_gap = check_requirement(d1, requirement
//    #ifndef NQUEST
//            , quest
//    #endif
//        );
//        boolean is_random = d1.getstrategy == DeckStrategy.random;
//        boolean deck_has_been_improved = true;
//        int long skipped_simulations = 0;
//        std.vector<const Card*> commander_candidates;
//        std.vector<const Card*> alpha_dominion_candidates;
//        std.vector<const Card*> card_candidates;
//
//        // resolve available to player cards
//        auto player_assaults_and_structures = proc.cards.player_commanders;
//        player_assaults_and_structures.insert(player_assaults_and_structures.end(), proc.cards.player_structures.begin(), proc.cards.player_structures.end());
//        player_assaults_and_structures.insert(player_assaults_and_structures.end(), proc.cards.player_assaults.begin(), proc.cards.player_assaults.end());
//        for (const Card* card: player_assaults_and_structures)
//        {
//            // skip illegal
//            if ((card.getm_category != CardCategory.dominion_alpha)
//                && (card.getm_category != CardCategory.normal))
//            { continue; }
//
//            // skip dominions when their climbing is disabled
//            if ((card.getm_category == CardCategory.dominion_alpha) && (!use_dominion_climbing))
//            { continue; }
//
//            // try to skip a card unless it's allowed
//            if (!allowed_candidates.count(card.getId()))
//            {
//                // skip disallowed always
//                if (disallowed_candidates.count(card.getId()))
//                { continue; }
//
//                // handle dominions
//                if (card.getm_category == CardCategory.dominion_alpha)
//                {
//                    // skip non-top-level dominions anyway
//                    // (will check it later and downgrade if necessary according to amount of material (shards))
//                    if (!card.getis_top_level_card())
//                    { continue; }
//
//                    // skip basic dominions
//                    if ((card.getId() == 50001) || (card.getId() == 50002))
//                    { continue; }
//                }
//
//                // handle normal cards
//                else
//                {
//                    // skip non-top-level cards (adjust_deck() will try to downgrade them if necessary)
//                    boolean use_top_level = (card.getm_type == CardType.commander) ? use_top_level_commander : use_top_level_card;
//                    if (!card.getis_top_level_card() and (fund || use_top_level || !owned_cards[card.getId()]))
//                    { continue; }
//
//                    // skip lowest fusion levels
//                    int use_fused_level = (card.getm_type == CardType.commander) ? use_fused_commander_level : use_fused_card_level;
//                    if (card.getm_fusion_level < use_fused_level)
//                    { continue; }
//                }
//            }
//
//            // skip sub-dominion cards anyway
//            if ((card.getm_category == CardCategory.dominion_alpha) && is_in_recipe(owned_alpha_dominion, card))
//            { continue; }
//
//            // skip unavailable cards anyway when ownedcards is used
//            if (use_owned_cards && !is_owned_or_can_be_fused(card))
//            {
//                boolean success = false;
//                if (card.getm_category == CardCategory.dominion_alpha)
//                {
//                    while (!card.getis_low_level_card() && !success)
//                    {
//                        card = card.getdowngraded();
//                        if (is_in_recipe(owned_alpha_dominion, card)) { break; }
//                        success = is_owned_or_can_be_fused(card);
//                    }
//                }
//                if (!success)
//                { continue; }
//            }
//
//            // enqueue candidate according to category & type
//            if (card.getm_type == CardType.commander)
//            {
//                commander_candidates.emplace_back(card);
//            }
//            else if (card.getm_category == CardCategory.dominion_alpha)
//            {
//                alpha_dominion_candidates.emplace_back(card);
//            }
//            else if (card.getm_category == CardCategory.normal)
//            {
//                card_candidates.emplace_back(card);
//            }
//        }
//        // append NULL as void card as well
//        card_candidates.emplace_back(nullptr);
//
//        // add current alpha dominion to candidates if necessary
//        // or setup first candidate into the deck if no alpha dominion defined
//        if (use_dominion_climbing)
//        {
//            if (best_alpha_dominion)
//            {
//                if (!std.count(alpha_dominion_candidates.begin(), alpha_dominion_candidates.end(), best_alpha_dominion))
//                {
//                    alpha_dominion_candidates.emplace_back(best_alpha_dominion);
//                }
//            }
//            else if (!alpha_dominion_candidates.empty())
//            {
//                best_alpha_dominion = d1.getalpha_dominion = alpha_dominion_candidates[0];
//            }
//            if (debug_print > 0)
//            {
//                for (const Card* dom_card : alpha_dominion_candidates)
//                {
//                    System.out.print(" ** next Alpha Dominion candidate: " + dom_card.getm_name
//                        + " ($: " + alpha_dominion_cost(dom_card) + ")" + std.endl;
//                }
//            }
//        }
//        if (!best_alpha_dominion && owned_alpha_dominion)
//        {
//            best_alpha_dominion = owned_alpha_dominion;
//            System.out.print("Setting up owned Alpha Dominion into a deck: " + best_alpha_dominion.getm_name + std.endl;
//        }
//
//        // + main climbing loop >>
//        for (int from_slot(freezed_cards), dead_slot(freezed_cards); ;
//                from_slot = std.max(freezed_cards, (from_slot + 1) % std.min<int>(max_deck_len, best_cards.size() + 1)))
//        {
//            if (deck_has_been_improved)
//            {
//                dead_slot = from_slot;
//                deck_has_been_improved = false;
//            }
//            else if (from_slot == dead_slot || best_score.points - target_score > -1e-9)
//            {
//                if (best_score.n_sims >= num_iterations || best_gap > 0)
//                { break; }
//                auto & prev_results = evaluated_decks[best_deck];
//                skipped_simulations += prev_results.second;
//                // Re-evaluate the best deck
//                d1.getcommander = best_commander;
//                d1.getalpha_dominion = best_alpha_dominion;
//                d1.getcards = best_cards;
//                auto evaluate_result = proc.evaluate(std.min(prev_results.second * iterations_multiplier, num_iterations), prev_results);
//                best_score = compute_score(evaluate_result, proc.factors);
//                System.out.print("Results refined: ";
//                print_score_info(evaluate_result, proc.factors);
//                dead_slot = from_slot;
//            }
//            if (best_score.points - target_score > -1e-9)
//            { continue; }
//
//            // commander
//            if (requirement.num_cards.count(best_commander) == 0)
//            {
//                // + commander candidate loop >>
//                for (const Card* commander_candidate: commander_candidates)
//                {
//                    if (best_score.points - target_score > -1e-9)
//                    { break; }
//                    if (commander_candidate == best_commander)
//                    { continue; }
//                    deck_has_been_improved |= try_improve_deck(d1, -1, -1, commander_candidate,
//                        best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck,
//                        evaluated_decks, zero_results, skipped_simulations, proc);
//                }
//                // Now that all commanders are evaluated, take the best one
//                d1.getcommander = best_commander;
//                d1.getalpha_dominion = best_alpha_dominion;
//                d1.getcards = best_cards;
//            }
//
//            // alpha dominion
//            if (use_dominion_climbing && !alpha_dominion_candidates.empty())
//            {
//                // + alpha dominion candidate loop >>
//                for (const Card* alpha_dominion_candidate: alpha_dominion_candidates)
//                {
//                    if (best_score.points - target_score > -1e-9)
//                    { break; }
//                    if (alpha_dominion_candidate == best_alpha_dominion)
//                    { continue; }
//                    deck_has_been_improved |= try_improve_deck(d1, -1, -1, alpha_dominion_candidate,
//                        best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck,
//                        evaluated_decks, zero_results, skipped_simulations, proc);
//                }
//                // Now that all alpha dominions are evaluated, take the best one
//                d1.getcommander = best_commander;
//                d1.getalpha_dominion = best_alpha_dominion;
//                d1.getcards = best_cards;
//            }
//
//            // shuffle candidates
//            std.shuffle(card_candidates.begin(), card_candidates.end(), re);
//
//            // + card candidate loop >>
//            for (const Card* card_candidate: card_candidates)
//            {
//                for (int to_slot(is_random ? from_slot : card_candidate ? freezed_cards : (best_cards.size() - 1));
//                        to_slot < (is_random ? (from_slot + 1) : (best_cards.size() + (from_slot < best_cards.size() ? 0 : 1)));
//                        ++ to_slot)
//                {
//                    if (card_candidate ?
//                            (from_slot < best_cards.size() && (from_slot == to_slot && card_candidate == best_cards[to_slot])) // 2 Omega .get 2 Omega
//                            :
//                            (from_slot == best_cards.size())) // void .get void
//                    { continue; }
//                    deck_has_been_improved |= try_improve_deck(d1, from_slot, to_slot, card_candidate,
//                        best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck,
//                        evaluated_decks, zero_results, skipped_simulations, proc);
//                }
//                if (best_score.points - target_score > -1e-9)
//                { break; }
//            }
//        }
//        d1.getcommander = best_commander;
//        d1.getalpha_dominion = best_alpha_dominion;
//        d1.getcards = best_cards;
//        int simulations = 0;
//        for (auto evaluation: evaluated_decks)
//        { simulations += evaluation.second.second; }
//        System.out.print("Evaluated " + evaluated_decks.size() + " decks (" + simulations + " + " + skipped_simulations + " simulations)." + std.endl;
//        System.out.print("Optimized Deck: ";
//        print_deck_inline(get_deck_cost(d1), best_score, d1);
//    }

  //------------------------------------------------------------------------------
    public void print_score_info(EvaluatedResults results, List<Double> factors) {
        Results finalResults = computeScore(results, factors);
        System.out.print(finalResults.getPoints() + " (");
        if (show_ci) {
            System.out.print(finalResults.points_lower_bound + "-" + finalResults.points_upper_bound +  ", ");
        }
        for (Results val: results.getResults()) {
            switch(optimization_mode)
            {
                case RAID:
                case CAMPAIGN:
                case BRAWL:
                case BRAWL_DEFENSE:
                case WAR:
                    System.out.print(val.points + " ");
                    break;
                default:
                    System.out.print(val.points / 100 + " ");
                    break;
            }
        }
        System.out.println("/ " + results.getTotalBattles() + ")");
    }
    
    void print_deck_inline(int deck_cost, Results score, Deck deck) {
        // print units count
        System.out.print(deck.getCards().size() + " units: ");
        
        // print deck cost (if fund is enabled)
        if (fund > 0){
            System.out.print("$" + deck_cost + " ");
        }

        // print optimization result details
        switch(optimization_mode)
        {
            case RAID:
            case CAMPAIGN:
            case BRAWL:
            case BRAWL_DEFENSE:
            case WAR:
                System.out.println("(" + score.wins * 100 + "% win");
                if (show_ci)
                {
                    System.out.print(", " + score.points_lower_bound + "-" + score.points_upper_bound);
                }
                System.out.print(") ");
                break;
            case DEFENSE:
                System.out.print("(" + score.draws * 100.0 + "% stall) ");
                break;
            default:
                break;
        }
        System.out.print(score.points);
        int min_score = min_possible_score[optimization_mode.ordinal()];
        int max_score = max_possible_score[optimization_mode.ordinal()];
        if (optimization_mode == OptimizationMode.BRAWL)
        {
            double win_points = score.getWins() > 0 ? ((score.getPoints() - min_score * (1.0 - score.getWins())) / score.getWins()) : score.getPoints();
            System.out.print(" [" + win_points + " per win]");
        }
        else if (optimization_mode == OptimizationMode.BRAWL_DEFENSE)
        {
            double opp_win_points = score.getLosses() > 0 ? max_score - ((score.points - (max_score - min_score) * (1.0 - score.losses)) / score.losses) : score.points;
            System.out.print(" [" + opp_win_points + " per opp win]");
        }

        // print commander
        System.out.print(": " + deck.getCommander().getName());

        // print dominions
        if (deck.getAlphaDominion() != null) { System.out.print(", " + deck.getAlphaDominion().getName()); }

        // print deck cards
        if (deck.getStrategy() == DeckStrategy.RANDOM) {
            Collections.sort(deck.getCards(), new Comparator<Card>() {
                @Override
                public int compare(Card a, Card b) {
                    return a.getId() - b.getId();
                }
            });
        }
        String last_name = "";
        int num_repeat = 0;
        for (Card card : deck.getCards()) {
            if (card.getName().equals(last_name)) {
                ++num_repeat;
            } else {
                if (num_repeat > 1) {
                    System.out.print(" #" + num_repeat);
                }
                System.out.print(", " + card.getName());
                last_name = card.getName();
                num_repeat = 1;
            }
        }
        if (num_repeat > 1) {
            System.out.print(" #" + num_repeat);
        }
        System.out.println();
    }
}
