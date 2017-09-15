package com.github.tyrantsim.jtuo.optimizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.math3.stat.interval.ClopperPearsonInterval;
import org.apache.commons.math3.stat.interval.ConfidenceInterval;

import com.github.tyrantsim.jtuo.Constants;
import com.github.tyrantsim.jtuo.Main;
import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.control.ConsoleLauncher;
import com.github.tyrantsim.jtuo.control.EvaluatedResults;
import com.github.tyrantsim.jtuo.control.Operation;
import com.github.tyrantsim.jtuo.control.SimProcess;
import com.github.tyrantsim.jtuo.control.Todo;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.DeckStrategy;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.parsers.DeckParser;
import com.github.tyrantsim.jtuo.parsers.FileParser;
import com.github.tyrantsim.jtuo.parsers.LevelsParser;
import com.github.tyrantsim.jtuo.parsers.SkillsSetParser;
import com.github.tyrantsim.jtuo.parsers.XmlBasedParser;
import com.github.tyrantsim.jtuo.sim.FieldSimulator;
import com.github.tyrantsim.jtuo.sim.GameMode;
import com.github.tyrantsim.jtuo.sim.OptimizationMode;
import com.github.tyrantsim.jtuo.sim.Results;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.util.Pair;
import com.github.tyrantsim.jtuo.util.Utils;

public class TyrantOptimize {

    public static boolean DEBUG = false;

    GameMode gamemode = GameMode.FIGHT;
    OptimizationMode optimization_mode = OptimizationMode.NOT_SET;

    public boolean modeOpenTheDeck = false;
    public OptimizationMode optimizationMode = OptimizationMode.NOT_SET;
    public HashMap<Integer, Integer> owned_cards = new HashMap<>();

    final Card owned_alpha_dominion = null;
    boolean use_owned_cards = true;

    public int min_deck_len = 1;
    public int max_deck_len = 10;
    public int freezed_cards = 0;

    public int fund = 0;
    public double target_score = 100;
    public double min_increment_of_score = 0;

    public boolean use_top_level_card = true;
    public boolean use_top_level_commander = true;

    public final int[] upgrade_cost = { 0, 5, 15, 30, 75, 150 };
    Map<Card, Integer> dominion_cost = new TreeMap<>();// [3][7];
    Map<Card, Integer> dominion_refund = new TreeMap<>(); // [3][7];

    public double confidenceLevel = 0.99;

    private boolean mode_open_the_deck = false;
    private boolean use_dominion_climbing = false;
    private boolean use_dominion_defusing = false;
    private int fused_card_level = 0;
    private int use_fused_card_level = 0;
    public int use_fused_commander_level = 0;
    boolean show_ci = false;

    public boolean useHarmonicMean = false;

    public volatile int thread_num_iterations = 0; // written by threads
    public volatile EvaluatedResults thread_results = null; // written by threads
    public volatile Results thread_best_results = null;
    public volatile boolean thread_compare = false;
    public volatile boolean thread_compare_stop = false; // written by threads
    public volatile boolean destroy_threads;

    int iterations_multiplier = 10;
    public int sim_seed = 0;
    
    Requirement requirement;

    Set<Integer> allowed_candidates = new HashSet<>();
    Set<Integer> disallowed_candidates = new HashSet<>();

    int min_possible_score[] = { 0, 0, 0, 10, 5, 5, 5, 0 };
    int max_possible_score[] = { 100, 100, 100, 100, 65, 65, 100, 100 };

    private int turn_limit = Constants.DEFAULT_TURN_LIMIT;

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
            if (card_it.getFirst() >= 0)
                ios.append(card_it.getFirst() + " ");
            ios.append("[" + card_it.getSecond().getId() + "] " + card_it.getSecond().getName());
        }
        return ios.toString();
    }

    public Deck findDeck(Decks decks, Cards allCards, String deckName) {
        Deck deck = Decks.findDeckByName(deckName);
        if (deck != null) {
            deck.resolve();
            return deck;
        }
        deck = new Deck(deckName, allCards);
        Decks.decks.add(deck);
        deck.resolve();
        return deck;
    }

    // ---------------------- $80 deck optimization
    // ---------------------------------
    int getRequiredCardsBeforeUpgrade(Map<Integer, Integer> owned_cards, List<Card> card_list, Map<Card, Integer> num_cards) {
        // TODO: to implement
        int deck_cost = 0;
        Stack<Card> unresolvedCards = new Stack<>();
        for (Card card : card_list) {
            num_cards.put(card, num_cards.get(card).intValue() + 1);
            unresolvedCards.add(card);
        }
        // un-upgrade according to type/category
        // * use fund for normal cards
        // * use only top-level cards for initial (basic) dominion (Alpha
        // Dominion) and dominion material (Dominion Shard)
        while (!unresolvedCards.isEmpty()) {
            // pop next unresolved card
            Card card = unresolvedCards.pop();

            // assume unlimited common/rare level-1 cards (standard set)
            if ((card.getSet() == 1000) && (card.getRarity() <= 2) && (card.isLowLevelCard()))
                continue;

            // keep un-defused (top-level) basic dominion & its material
            if ((card.getId() == 50002) || (card.getCategory() == CardCategory.DOMINION_MATERIAL))
                continue;

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

    public Integer getRequiredCardsBeforeUpgrade(List<Card> card_list, Map<Card, Integer> num_cards) {
        return getRequiredCardsBeforeUpgrade(owned_cards, card_list, num_cards);
    }

    public int get_deck_cost(Deck deck) {
        if (!use_owned_cards) {
            return 0;
        }
        Map<Card, Integer> num_in_deck = new HashMap<>();
        int deck_cost = 0;
        if (deck.getCommander() != null) {
            deck_cost += getRequiredCardsBeforeUpgrade(Arrays.asList(new Card[] { deck.getCommander() }), num_in_deck); // {deck.getCommander()},
                                                                                                                        // num_in_deck);
        }
        deck_cost += getRequiredCardsBeforeUpgrade(deck.getCards(), num_in_deck);
        for (Entry<Card, Integer> it : num_in_deck.entrySet()) {
            Integer card_id = it.getKey().getId();
            if (it.getValue() > owned_cards.get(card_id))
                ;
            {
                return Integer.MAX_VALUE;
            }
        }
        return deck_cost;
    }

    boolean is_in_recipe(Card card, Card material) {
        // is it already material?
        if (card == material) {
            return true;
        }
        // no recipes
        if (card.getRecipeCards().isEmpty()) {
            return false;
        }
        // avoid illegal
        if (card.getCategory() == CardCategory.DOMINION_MATERIAL) {
            return false;
        }
        // check recursively
        for (Entry<Card, Integer> recipe_it : card.getRecipeCards().entrySet()) {
            // is material found?
            if (recipe_it.getKey() == material) {
                return true;
            }
            // go deeper ...
            if (is_in_recipe(recipe_it.getKey(), material)) {
                return true;
            }
        }
        // found nothing
        return false;
    }

    boolean is_owned_or_can_be_fused(Card card) {
        if (owned_cards.get(card.getId()) != null) {
            return true;
        }
        if (fund == 0 && card.getCategory() == CardCategory.NORMAL) {
            return false;
        }
        Map<Card, Integer> num_in_deck = new TreeMap<>();
        List<Card> cards = new ArrayList<>();
        cards.add(card);
        Integer deck_cost = getRequiredCardsBeforeUpgrade(cards, num_in_deck);

        Map<Integer, Integer> num_under = new TreeMap<>();

        if ((card.getCategory() == CardCategory.NORMAL) && (deck_cost > fund)) {
            while (!card.isLowLevelCard() && (deck_cost > fund)) {
                cards.clear();
                cards.add(card.downgraded());
                num_in_deck.clear();
                deck_cost = getRequiredCardsBeforeUpgrade(cards, num_in_deck);
            }
            if (deck_cost > fund) {
                return false;
            }
        }
        for (Entry<Card, Integer> it : num_in_deck.entrySet()) {
            if (it.getValue() > owned_cards.get(it.getKey().getId())) {
                if ((card.getCategory() == CardCategory.DOMINION_ALPHA) && use_dominion_defusing && !is_in_recipe(card, owned_alpha_dominion)) {
                    if (it.getKey().getId() != 50002) {
                        Integer value = num_under.get(it.getKey().getId());
                        value += it.getValue() - owned_cards.get(it.getKey().getId());
                        num_under.put(it.getKey().getId(), value);
                    }
                    continue;
                }
                return false;
            }
        }
        // TODO: rewrite Dominion refund here.
        // if (!num_under.isEmpty()) {
        // Map<Card, Integer> refund =
        // dominion_refund.get(owned_alpha_dominion.getFusionLevel())[owned_alpha_dominion.getLevel()];
        // for (Entry<Card, Integer> refund_it : refund.entrySet()) {
        // int refund_id = refund_it.getKey().getId();
        // if (!num_under.count(refund_id)) { continue; }
        // num_under.put(refund_id, Utils.safeMinus(num_under.get(refund_id),
        // refund_it.getValue());
        // if (!num_under.get(refund_id)) { num_under.remove(refund_id); }
        // }
        // }
        return num_under.isEmpty();
    }

    private String alpha_dominion_cost(Card dom_card) {
        // assert(dom_card.getCategory() == CardCategory.DOMINION_ALPHA);
        // if (owned_alpha_dominion == null) { return "(no owned alpha
        // dominion)"; }
        // Map<Integer, Integer> _owned_cards;
        // Map<Integer, Integer> refund_owned_cards;
        // Map<Card, Integer> num_cards;
        // Map<Card, Integer> refund =
        // dominion_refund[owned_alpha_dominion.getFusionLevel()][owned_alpha_dominion.getLevel()];
        // Integer own_dom_id = 50002;
        // if (is_in_recipe(dom_card, owned_alpha_dominion)) {
        // own_dom_id = owned_alpha_dominion.getId();
        // }
        // else if (owned_alpha_dominion.getId() != 50002) {
        // for (Entry<Card, Integer> it : refund.entrySet()) {
        // if (it.getKey().getCategory() != CardCategory.DOMINION_MATERIAL) {
        // continue; }
        // refund_owned_cards.put( it.getKey().getId(),
        // refund_owned_cards.get(it.getKey().getId()) + it.getValue);
        // }
        // }
        // _owned_cards.put(own_dom_id) = 1;
        // // List cards = new ArrayList
        // getRequiredCardsBeforeUpgrade(_owned_cards, Arrays.asList(new Card[]
        // {dom_card}), num_cards);
        String value = "";
        // for (Entry<Card, Integer> it : num_cards.entrySet()) {
        // if (it.getKey().getCategory() != CardCategory.DOMINION_MATERIAL) {
        // continue; }
        // value += it.getKey().getName() + " x " + it.getValue() + ", ";
        // }
        // if (!is_in_recipe(dom_card, owned_alpha_dominion))
        // {
        // num_cards.clear();
        // getRequiredCardsBeforeUpgrade(_owned_cards, Arrays.asList(new Card[]
        // {owned_alpha_dominion}), num_cards);
        // value += "using refund: ";
        // for (Entry<Card, Integer> it : refund.entrySet()) {
        // // TODO: what do it (next line) here?
        // //signed num_under(it.getValue() -
        // (signed)num_cards.get(it.getKey()));
        // value += it.getKey().getName() + " x " + it.getValue() + "/" +
        // num_under + ", ";
        // }
        // }
        // // remove trailing ', ' for non-empty string / replace empty by
        // '(none)'
        // if (!value.isEmpty()) { value.substring(0, value.length() - 3); }
        // else { value += "(none)"; }
        return value;
    }

    // insert card at to_slot into deck limited by fund; store deck_cost
    // return true if affordable
    boolean adjustDeck(Deck deck, int from_slot, int to_slot, final Card card, int fund, Object rndGenerator, int deck_cost, List<Pair<Integer, Card>> cards_out, List<Pair<Integer, Card>> cards_in) {
        boolean is_random = deck.getStrategy() == DeckStrategy.RANDOM;
        cards_out.clear();
        cards_in.clear();
        if (from_slot < 0) {
            if (card.getCategory() == CardCategory.DOMINION_ALPHA) {
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
            cards_out.add(new Pair<>(is_random ? -1 : from_slot, deck.getCards().get(from_slot))); // cards_out.emplace_back(is_random
                                                                                                   // ?
                                                                                                   // -1
                                                                                                   // :
                                                                                                   // from_slot,
                                                                                                   // deck.getcards[from_slot]);
                                                                                                   // //
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
            // deck.getcards.emplace_back(card);
            deck_cost = get_deck_cost(deck);
            if (!use_top_level_card && (deck_cost > fund)) {
                while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                    candidate_card = candidate_card.downgraded();
                    deck.getCards().set(0, candidate_card);
                    deck_cost = get_deck_cost(deck);
                }
            }
            if (deck_cost > fund)
                return false;
            cards_in.add(new Pair<>(is_random ? -1 : to_slot, deck.getCards().get(0)));
        }

        // try to add commander into the deck, downgrade it if necessary
        {
            Card candidate_card = old_commander;
            deck.setCommander(candidate_card);
            deck_cost = get_deck_cost(deck);
            if (!use_top_level_commander && (deck_cost > fund)) {
                while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                    candidate_card = candidate_card.downgraded();
                    deck.setCommander(candidate_card);
                    deck_cost = get_deck_cost(deck);
                }
            }
            if (deck_cost > fund)
                return false;
            if (deck.getCommander() != old_commander) {
                // TODO: replace C++ code
                // append_unless_remove(cards_out, cards_in, {-1,
                // old_commander});
                // append_unless_remove(cards_in, cards_out, {-1,
                // deck.getCommander()});
            }
        }

        // added backuped deck cards back (place cards strictly before/after
        // card inserted above according to slot index)
        for (int i = 0; i < cards.size(); ++i) {
            // try to add cards[i] into the deck, downgrade it if necessary
            Card candidate_card = cards.get(i);
            // Card in_it = deck.getCards().get(deck.getCards().size() - (i <
            // to_slot ? 1 : 0)); // ??? .getcards.end() - (i < to_slot); //
            // (before/after according to slot index)
            if (i < to_slot) {
                deck.getCards().add(deck.getCards().size() - 1, candidate_card);
            } else {
                deck.getCards().add(candidate_card);
            }

            deck_cost = get_deck_cost(deck);
            if (!use_top_level_card && (deck_cost > fund)) {
                while ((deck_cost > fund) && !candidate_card.isLowLevelCard()) {
                    candidate_card = candidate_card.downgraded();
                    // in_it = candidate_card;
                    deck_cost = get_deck_cost(deck);
                }
            }
            if (deck_cost > fund)
                return false;
            if (candidate_card != cards.get(i)) {
                // TODO: replace C++ code
                // append_unless_remove(cards_out, cards_in, {is_random ? -1 : i
                // + (i >= from_slot), cards[i]});
                // append_unless_remove(cards_in, cards_out, {is_random ? -1 : i
                // + (i >= to_slot), *in_it});
            }
        }
        return !cards_in.isEmpty() || !cards_out.isEmpty();
    }

    int check_requirement(Deck deck, Requirement requirement) {
        int gap = Utils.safeMinus(min_deck_len, deck.getCards().size());
        if (!requirement.num_cards.isEmpty()) {
            Map<Card, Integer> num_cards = new HashMap<>();
            num_cards.put(deck.getCommander(), 1);
            for (Card card : deck.getCards()) {
                num_cards.put(card, num_cards.get(card) + 1);
            }
            for (Entry<Card, Integer> entry : requirement.num_cards.entrySet()) {
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
                    System.out.println("WARNING: Need extra " + num_to_claim + " " + card.getName() + " to build your initial deck: adding to owned card list.\n");
                }
            }
        }
    }

    public Results computeScore(EvaluatedResults results, List<Double> factors) {
        Results last = new Results(0l, 0l, 0l, 0l); // , 0, 0, 0, 0, 0,
                                                    // results.second};
        double max_possible = max_possible_score[optimizationMode.ordinal()];
        for (int index = 0; index < results.getResults().size(); ++index) {
            last.wins += results.getResults().get(index).wins * factors.get(index);
            last.draws += results.getResults().get(index).draws * factors.get(index);
            last.losses += results.getResults().get(index).losses * factors.get(index);
            // results.second, results.first.get(index).points / max_possible, 1
            // - confidence_level
            ConfidenceInterval confidenceInterval = new ClopperPearsonInterval().createInterval(results.getTotalBattles(), (int) Math.round(results.getResults().get(index).points / max_possible), confidenceLevel); // new
                                                                                                                                                                                                                      // BinomialDistribution(results.getTotalBattles(),
                                                                                                                                                                                                                      // 1
                                                                                                                                                                                                                      // -
                                                                                                                                                                                                                      // confidenceLevel);
            // results.getResults().get(index).points / max_possible
            double lower_bound = confidenceInterval.getLowerBound() * max_possible; // find_lower_bound_on_p()
                                                                                    // *
                                                                                    // max_possible;
            double upper_bound = confidenceInterval.getUpperBound() * max_possible; // new
                                                                                    // BinomialDistribution(results.getTotalBattles(),
                                                                                    // results.getResults().get(index).points
                                                                                    // /
                                                                                    // max_possible,
                                                                                    // 1
                                                                                    // -
                                                                                    // confidenceLevel).getSupportUpperBound()
                                                                                    // *
                                                                                    // max_possible;
            if (useHarmonicMean) {
                last.points += factors.get(index) / results.getResults().get(index).points;
                last.points_lower_bound += factors.get(index) / lower_bound;
                last.points_upper_bound += factors.get(index) / upper_bound;
            } else {
                last.points += results.getResults().get(index).points * factors.get(index);
                last.points_lower_bound += lower_bound * factors.get(index);
                last.points_upper_bound += upper_bound * factors.get(index);
            }
        }
        double factorSum = 0;
        for (int i = 0; i < factors.size(); i++) {
            factorSum += factors.get(i);
        }

        last.wins /= factorSum * (double) results.getTotalBattles();
        last.draws /= factorSum * (double) results.getTotalBattles();
        last.losses /= factorSum * (double) results.getTotalBattles();
        if (useHarmonicMean) {
            last.points = (long) factorSum / (results.getTotalBattles() * last.points);
            last.points_lower_bound = factorSum / last.points_lower_bound;
            last.points_upper_bound = factorSum / last.points_upper_bound;
        } else {
            last.points /= factorSum * results.getTotalBattles();
            last.points_lower_bound /= factorSum;
            last.points_upper_bound /= factorSum;
        }
        return last;
    }

    // ------------------------------------------------------------------------------
    private boolean tryImproveDeck(Deck d1, int from_slot, int to_slot, Card card_candidate, Card best_commander, Card best_alpha_dominion, List<Card> best_cards, Results best_score, int best_gap, String best_deck,
            Map<String, EvaluatedResults> evaluated_decks, EvaluatedResults zero_results, long skipped_simulations, SimProcess proc) {
        int deck_cost = 0;
        List<Pair<Integer, Card>> cards_out = new ArrayList<>(), cards_in = new ArrayList<>();
        Random re = proc.getRandom();

        // setup best deck
        d1.setCommander(best_commander);
        d1.setAlphaDominion(best_alpha_dominion);
        d1.setCards(best_cards);

        // try to adjust the deck
        if (!adjustDeck(d1, from_slot, to_slot, card_candidate, fund, re, deck_cost, cards_out, cards_in)) {
            return false;
        }

        // check gap
        int new_gap = check_requirement(d1, requirement);
        if ((new_gap > 0) && (new_gap >= best_gap)) {
            return false;
        }

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
        if (new_gap < best_gap || current_score.points > best_score.points + min_increment_of_score) {
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

    // ------------------------------------------------------------------------------
    public void hillClimbing(int num_min_iterations, int num_iterations, Deck d1, SimProcess proc, Requirement requirement) {
        EvaluatedResults zero_results = new EvaluatedResults();
        // zero_results. =
        // EvaluatedResults.first_type(proc.getEnemyDecks().size()), 0 };
        String best_deck = d1.hash();
        Map<String, EvaluatedResults> evaluated_decks = new TreeMap<>();
        evaluated_decks.put(best_deck, zero_results);

        EvaluatedResults results = proc.evaluate(num_min_iterations, evaluated_decks.get(best_deck));
        print_score_info(results, proc.getFactors());
        Results best_score = computeScore(results, proc.getFactors());
        Card best_commander = d1.getCommander();
        Card best_alpha_dominion = d1.getAlphaDominion();
        List<Card> best_cards = d1.getCards();
        int deck_cost = get_deck_cost(d1);
        fund = Math.max(fund, deck_cost);
        print_deck_inline(deck_cost, best_score, d1);

        int best_gap = check_requirement(d1, requirement);
        boolean is_random = d1.getStrategy() == DeckStrategy.RANDOM;
        boolean deck_has_been_improved = true;
        long skipped_simulations = 0;
        List<Card> commander_candidates = new ArrayList<>();
        ;
        List<Card> alpha_dominion_candidates = new ArrayList<>();
        List<Card> card_candidates = new ArrayList<>();

        // resolve available to player cards
        List<Card> player_assaults_and_structures = new ArrayList<>(Cards.playerCommanders);
        player_assaults_and_structures.addAll(Cards.playerStructures);
        player_assaults_and_structures.addAll(Cards.playerAssaults);
        for (Card card : player_assaults_and_structures) {
            // skip illegal
            if ((card.getCategory() != CardCategory.DOMINION_ALPHA) && (card.getCategory() != CardCategory.NORMAL)) {
                continue;
            }

            // skip dominions when their climbing is disabled
            if ((card.getCategory() == CardCategory.DOMINION_ALPHA) && (!use_dominion_climbing)) {
                continue;
            }

            // try to skip a card unless it's allowed
            if (allowed_candidates.contains(card.getId())) {
                // skip disallowed always
                if (disallowed_candidates.contains(card.getId())) {
                    continue;
                }

                // handle dominions
                if (card.getCategory() == CardCategory.DOMINION_ALPHA) {
                    // skip non-top-level dominions anyway
                    // (will check it later and downgrade if necessary according
                    // to amount of material (shards))
                    if (!card.isTopLevelCard()) {
                        continue;
                    }

                    // skip basic dominions
                    if ((card.getId() == 50001) || (card.getId() == 50002)) {
                        continue;
                    }
                }

                // handle normal cards
                else {
                    // skip non-top-level cards (adjust_deck() will try to
                    // downgrade them if necessary)
                    boolean use_top_level = (card.getType() == CardType.COMMANDER) ? use_top_level_commander : use_top_level_card;
                    if (!card.isTopLevelCard() && (fund > 0 || use_top_level || owned_cards.get(card.getId()) == 0)) {
                        continue;
                    }

                    // skip lowest fusion levels
                    int use_fused_level = (card.getType() == CardType.COMMANDER) ? use_fused_commander_level : use_fused_card_level;
                    if (card.getFusionLevel() < use_fused_level) {
                        continue;
                    }
                }
            }

            // skip sub-dominion cards anyway
            if ((card.getCategory() == CardCategory.DOMINION_ALPHA) && is_in_recipe(owned_alpha_dominion, card)) {
                continue;
            }

            // skip unavailable cards anyway when ownedcards is used
            if (use_owned_cards && !is_owned_or_can_be_fused(card)) {
                boolean success = false;
                if (card.getCategory() == CardCategory.DOMINION_ALPHA) {
                    while (!card.isLowLevelCard() && !success) {
                        card = card.downgraded();
                        if (is_in_recipe(owned_alpha_dominion, card)) {
                            break;
                        }
                        success = is_owned_or_can_be_fused(card);
                    }
                }
                if (!success) {
                    continue;
                }
            }

            // enqueue candidate according to category & type
            if (card.getType() == CardType.COMMANDER) {
                commander_candidates.add(card);
            } else if (card.getCategory() == CardCategory.DOMINION_ALPHA) {
                alpha_dominion_candidates.add(card);
            } else if (card.getCategory() == CardCategory.NORMAL) {
                card_candidates.add(card);
            }
        }
        // append NULL as void card as well
        card_candidates.add(null);

        // add current alpha dominion to candidates if necessary
        // or setup first candidate into the deck if no alpha dominion defined
        if (use_dominion_climbing) {
            if (best_alpha_dominion != null) {
                if (!alpha_dominion_candidates.contains(best_alpha_dominion)) {
                    alpha_dominion_candidates.add(best_alpha_dominion);
                }
            } else if (!alpha_dominion_candidates.isEmpty()) {
                best_alpha_dominion = alpha_dominion_candidates.get(0);
                d1.setAlphaDominion(best_alpha_dominion);
            }
            if (Main.debug_print > 0) {
                for (Card dom_card : alpha_dominion_candidates) {
                    System.out.println(" ** next Alpha Dominion candidate: " + dom_card.getName() + " ($: " + alpha_dominion_cost(dom_card) + ")");
                }
            }
        }
        if (best_alpha_dominion == null && owned_alpha_dominion != null) {
            best_alpha_dominion = owned_alpha_dominion;
            System.out.println("Setting up owned Alpha Dominion into a deck: " + best_alpha_dominion.getName());
        }

        // + main climbing loop >>
        int from_slot = freezed_cards, dead_slot = freezed_cards;
        while (true) {
            from_slot = Math.max(freezed_cards, (from_slot + 1) % Math.min(max_deck_len, best_cards.size() + 1));
            if (deck_has_been_improved) {
                dead_slot = from_slot;
                deck_has_been_improved = false;
            } else if (from_slot == dead_slot || best_score.points - target_score > -1e-9) {
                if (best_score.n_sims >= num_iterations || best_gap > 0) {
                    break;
                }
                EvaluatedResults prev_results = evaluated_decks.get(best_deck);
                skipped_simulations += prev_results.getTotalBattles();
                // Re-evaluate the best deck
                d1.setCommander(best_commander);
                d1.setAlphaDominion(best_alpha_dominion);
                d1.setCards(best_cards);
                EvaluatedResults evaluate_result = proc.evaluate(Math.min(prev_results.getTotalBattles() * iterations_multiplier, num_iterations), prev_results);
                best_score = computeScore(evaluate_result, proc.getFactors());
                System.out.print("Results refined: ");
                print_score_info(evaluate_result, proc.getFactors());
                dead_slot = from_slot;
            }
            if (best_score.points - target_score > -1e-9) {
                continue;
            }

            // commander
            if (!requirement.num_cards.containsKey(best_commander)) {
                // + commander candidate loop >>
                for (Card commander_candidate : commander_candidates) {
                    if (best_score.points - target_score > -1e-9) {
                        break;
                    }
                    if (commander_candidate == best_commander) {
                        continue;
                    }
                    deck_has_been_improved |= tryImproveDeck(d1, -1, -1, commander_candidate, best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck, evaluated_decks, zero_results, skipped_simulations, proc);
                }
                // Now that all commanders are evaluated, take the best one
                d1.setCommander(best_commander);
                d1.setAlphaDominion(best_alpha_dominion);
                d1.setCards(best_cards);
            }

            // alpha dominion
            if (use_dominion_climbing && !alpha_dominion_candidates.isEmpty()) {
                // + alpha dominion candidate loop >>
                for (Card alpha_dominion_candidate : alpha_dominion_candidates) {
                    if (best_score.points - target_score > -1e-9) {
                        break;
                    }
                    if (alpha_dominion_candidate == best_alpha_dominion) {
                        continue;
                    }
                    deck_has_been_improved |= tryImproveDeck(d1, -1, -1, alpha_dominion_candidate, best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck, evaluated_decks, zero_results, skipped_simulations, proc);
                }
                // Now that all alpha dominions are evaluated, take the best one
                d1.setCommander(best_commander);
                d1.setAlphaDominion(best_alpha_dominion);
                d1.setCards(best_cards);
            }

            // shuffle candidates
            Collections.shuffle(card_candidates, proc.getRandom());

            // + card candidate loop >>
            for (Card card_candidate : card_candidates) {
                for (int to_slot = is_random ? from_slot : card_candidate != null ? freezed_cards : (best_cards.size() - 1); to_slot < (is_random ? (from_slot + 1) : (best_cards.size() + (from_slot < best_cards.size() ? 0 : 1))); to_slot++) {
                    if (card_candidate != null ? (from_slot < best_cards.size() && (from_slot == to_slot && card_candidate == best_cards.get(to_slot))) // 2
                                                                                                                                                        // Omega
                                                                                                                                                        // .get
                                                                                                                                                        // 2
                                                                                                                                                        // Omega
                            : (from_slot == best_cards.size())) // void .get
                                                                // void
                    {
                        continue;
                    }
                    deck_has_been_improved |= tryImproveDeck(d1, from_slot, to_slot, card_candidate, best_commander, best_alpha_dominion, best_cards, best_score, best_gap, best_deck, evaluated_decks, zero_results, skipped_simulations, proc);
                }
                if (best_score.points - target_score > -1e-9) {
                    break;
                }
            }
        }
        d1.setCommander(best_commander);
        d1.setAlphaDominion(best_alpha_dominion);
        d1.setCards(best_cards);
        int simulations = 0;
        for (Entry<String, EvaluatedResults> evaluation : evaluated_decks.entrySet()) {
            simulations += evaluation.getValue().getTotalBattles();
        }
        System.out.println("Evaluated " + evaluated_decks.size() + " decks (" + simulations + " + " + skipped_simulations + " simulations).");
        System.out.print("Optimized Deck: ");
        print_deck_inline(get_deck_cost(d1), best_score, d1);
    }

    // ------------------------------------------------------------------------------
    public void print_score_info(EvaluatedResults results, List<Double> factors) {
        Results finalResults = computeScore(results, factors);
        System.out.print(finalResults.getPoints() + " (");
        if (show_ci) {
            System.out.print(finalResults.points_lower_bound + "-" + finalResults.points_upper_bound + ", ");
        }
        for (Results val : results.getResults()) {
            switch (optimization_mode) {
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
        if (fund > 0) {
            System.out.print("$" + deck_cost + " ");
        }

        // print optimization result details
        switch (optimization_mode) {
        case RAID:
        case CAMPAIGN:
        case BRAWL:
        case BRAWL_DEFENSE:
        case WAR:
            System.out.println("(" + score.wins * 100 + "% win");
            if (show_ci) {
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
        if (optimization_mode == OptimizationMode.BRAWL) {
            double win_points = score.getWins() > 0 ? ((score.getPoints() - min_score * (1.0 - score.getWins())) / score.getWins()) : score.getPoints();
            System.out.print(" [" + win_points + " per win]");
        } else if (optimization_mode == OptimizationMode.BRAWL_DEFENSE) {
            double opp_win_points = score.getLosses() > 0 ? max_score - ((score.points - (max_score - min_score) * (1.0 - score.losses)) / score.losses) : score.points;
            System.out.print(" [" + opp_win_points + " per opp win]");
        }

        // print commander
        System.out.print(": " + deck.getCommander().getName());

        // print dominions
        if (deck.getAlphaDominion() != null) {
            System.out.print(", " + deck.getAlphaDominion().getName());
        }

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
    
    public int main(String[] args) {
        if (args.length == 2 && args[1].equals("-version")) {
            System.out.println("Tyrant Unleashed Optimizer " + Constants.VERSION);
            return 0;
        }
        if (args.length <= 2) {
            ConsoleLauncher.printUsage();
            return 255;
        }

        int opt_num_threads = 4;
        DeckStrategy opt_your_strategy = DeckStrategy.RANDOM;
        DeckStrategy opt_enemy_strategy = DeckStrategy.RANDOM;
        String opt_forts, opt_enemy_forts;
        String opt_doms, opt_enemy_doms;
        String opt_hand, opt_enemy_hand;
        String opt_vip;
        String opt_allow_candidates;
        String opt_disallow_candidates;
        String opt_disallow_recipes;
        String opt_target_score;
        List<String> fn_suffix_list = Arrays.asList(new String[] {""});
        List<String> opt_owned_cards_str_list;
        boolean opt_do_optimization= false;
        boolean opt_keep_commander = false;
        List<Todo> opt_todo = new ArrayList<>();

        String[] opt_effects = new String[3];  // 0-you; 1-enemy; 2-global
        int[] opt_bg_effects = new int[2]; // std::array<signed short, PassiveBGE::num_passive_bges> opt_bg_effects[2];
        SkillSpec[] opt_bg_skills = new SkillSpec[2];
        Set<Integer> disallowed_recipes;

        for (int argIndex = 3; argIndex < args.length; argIndex++) {
            // Codec
            if (args[argIndex].equals("ext_b64")) {
                //hash_to_ids = hash_to_ids_ext_b64;
                //encode_deck = encode_deck_ext_b64;
//            } else if (args[argIndex].equals("wmt_b64") == 0) {
//                hash_to_ids = hash_to_ids_wmt_b64;
//                encode_deck = encode_deck_wmt_b64;
//            }
//            else if (args[argIndex].equals("ddd_b64") == 0)
//            {
//                hash_to_ids = hash_to_ids_ddd_b64;
//                encode_deck = encode_deck_ddd_b64;
//            }
//            // Base Game Mode
//            else if (args[argIndex].equals("GameMode.FIGHT") == 0)
//            {
//                gamemode = GameMode.FIGHT;
//            }
//            else if (args[argIndex].equals("-s") == 0 || args[argIndex].equals("GameMode.SURGE") == 0)
//            {
//                gamemode = GameMode.SURGE;
            } else if (args[argIndex].equals("win")) {
                // Base Scoring Mode
                optimization_mode = OptimizationMode.WINRATE;
            } else if (args[argIndex].equals("defense")) {
                optimization_mode = OptimizationMode.DEFENSE;
            }
            else if (args[argIndex].equals("raid"))
            {
                optimization_mode = OptimizationMode.RAID;
            }
            // Mode Package
            else if (args[argIndex].equals("campaign"))
            {
                gamemode = GameMode.SURGE;
                optimization_mode = OptimizationMode.CAMPAIGN;
            }
            else if (args[argIndex].equals("pvp"))
            {
                gamemode = GameMode.FIGHT;
                optimization_mode = OptimizationMode.WINRATE;
            }
            else if (args[argIndex].equals("pvp-defense"))
            {
                gamemode = GameMode.SURGE;
                optimization_mode = OptimizationMode.DEFENSE;
            }
            else if (args[argIndex].equals("brawl"))
            {
                gamemode = GameMode.SURGE;
                optimization_mode = OptimizationMode.BRAWL;
            }
            else if (args[argIndex].equals("brawl-defense"))
            {
                gamemode = GameMode.FIGHT;
                optimization_mode = OptimizationMode.BRAWL_DEFENSE;
            }
            else if (args[argIndex].equals("gw"))
            {
                gamemode = GameMode.SURGE;
                optimization_mode = OptimizationMode.WINRATE;
            }
            else if (args[argIndex].equals("gw-defense"))
            {
                gamemode = GameMode.FIGHT;
                optimization_mode = OptimizationMode.DEFENSE;
            }
            // Others
            else if (args[argIndex].equals("keep-commander") || args[argIndex].equals("-c"))
            {
                opt_keep_commander = true;
            }
            else if (args[argIndex].equals("effect") || args[argIndex].equals("-e"))
            {
                opt_effects[2] = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("ye") || args[argIndex].equals("yeffect"))
            {
                opt_effects[0] = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("ee") || args[argIndex].equals("eeffect"))
            {
                opt_effects[1] = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("freeze") || args[argIndex].equals("-F"))
            {
                freezed_cards = Integer.valueOf(args[argIndex + 1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("-L"))
            {
                min_deck_len = Integer.valueOf(args[argIndex + 1]);
                max_deck_len = Integer.valueOf(args[argIndex + 2]);
                argIndex += 2;
            }
            else if (args[argIndex].equals("-o-"))
            {
                use_owned_cards = false;
            }
            else if (args[argIndex].equals("-o"))
            {
                opt_owned_cards_str_list.add("data/ownedcards.txt");
                use_owned_cards = true;
            }
            else if (args[argIndex].startsWith("-o="))
            {
                opt_owned_cards_str_list.add(args[argIndex] + 3);
                use_owned_cards = true;
            }
            else if (args[argIndex].startsWith("_"))
            {
                fn_suffix_list.add(args[argIndex]);
            }
            else if (args[argIndex].equals("fund"))
            {
                fund = Integer.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("dom+") || args[argIndex].equals("dominion+"))
            {
                use_dominion_climbing = true;
            }
            else if (args[argIndex].equals("dom-") || args[argIndex].equals("dominion-"))
            {
                use_dominion_climbing = true;
                use_dominion_defusing = true;
            }
            else if (args[argIndex].equals("random"))
            {
                opt_your_strategy = DeckStrategy.RANDOM;
            }
            else if (args[argIndex].equals("-r") || args[argIndex].equals("ordered"))
            {
                opt_your_strategy = DeckStrategy.ORDERED;
            }
            else if (args[argIndex].equals("exact-ordered"))
            {
                opt_your_strategy = DeckStrategy.EXACT_ORDERED;
            }
            else if (args[argIndex].equals("enemy:ordered"))
            {
                opt_enemy_strategy = DeckStrategy.ORDERED;
            }
            else if (args[argIndex].equals("enemy:exact-ordered"))
            {
                opt_enemy_strategy = DeckStrategy.EXACT_ORDERED;
            }
            else if (args[argIndex].equals("endgame"))
            {
                use_fused_card_level = Integer.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("threads") || args[argIndex].equals("-t"))
            {
                opt_num_threads = Integer.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("target"))
            {
                opt_target_score = args[argIndex+1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("turnlimit"))
            {
                turn_limit = Integer.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("mis"))
            {
                min_increment_of_score = Double.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("cl"))
            {
                confidenceLevel = Double.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("+ci"))
            {
                show_ci = true;
            }
            else if (args[argIndex].equals("+hm"))
            {
                useHarmonicMean = true;
            }
            else if (args[argIndex].equals("seed"))
            {
                sim_seed = Integer.valueOf(args[argIndex+1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("-v"))
            {
                Main.debug_print = 0;
            }
            else if (args[argIndex].equals("+v"))
            {
                Main.debug_print = 2;
            }
            else if (args[argIndex].equals("vip"))
            {
                opt_vip = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("allow-candidates"))
            {
                opt_allow_candidates = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("disallow-candidates"))
            {
                opt_disallow_candidates = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("disallow-recipes"))
            {
                opt_disallow_recipes = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("hand"))  // set initial hand for test
            {
                opt_hand = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("enemy:hand"))  // set enemies' initial hand for test
            {
                opt_enemy_hand = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("yf") || args[argIndex].equals("yfort"))  // set forts
            {
                opt_forts = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("ef") || args[argIndex].equals("efort"))  // set enemies' forts
            {
                opt_enemy_forts = args[argIndex + 1];
                argIndex += 1;
            }
            else if (args[argIndex].equals("yd") || args[argIndex].equals("ydom"))  // set dominions
            {
                opt_doms = (args[argIndex + 1]);
                argIndex += 1;
            }
            else if (args[argIndex].equals("ed") || args[argIndex].equals("edom"))  // set enemies' dominions
            {
                opt_enemy_doms = args[argIndex + 1];
                argIndex += 1;
            } else if (args[argIndex].equals("sim")) {
                Todo todo = new Todo(Operation.SIMULATE, Integer.parseInt(args[argIndex + 1]), 0);
                opt_todo.add(todo);
                if (todo.iterations < 10) { opt_num_threads = 1; }
                argIndex += 1;
            }
            // climbing tasks
            else if (args[argIndex].equals("climbex")) {
                Todo todo = new Todo(Operation.CLIMB, Integer.parseInt(args[argIndex + 1]), Integer.parseInt(args[argIndex + 2]));
                opt_todo.add(todo);
                if (todo.iterationsFine < 10) { opt_num_threads = 1; }
                opt_do_optimization = true;
                argIndex += 2;
            } else if (args[argIndex].equals("climb")) {
                Todo todo = new Todo(Operation.CLIMB, Integer.parseInt(args[argIndex + 1]), Integer.parseInt(args[argIndex + 1]));
                opt_todo.add(todo);
                if (todo.iterationsFine < 10) { opt_num_threads = 1; }
                opt_do_optimization = true;
                argIndex += 1;
            } else if (args[argIndex].equals("reorder")) {
                Todo todo = new Todo(Operation.REORDER, Integer.parseInt(args[argIndex + 1]), Integer.parseInt(args[argIndex + 1]));
                opt_todo.add(todo);
                if (todo.iterationsFine < 10) { opt_num_threads = 1; }
                argIndex += 1;
            } else if (args[argIndex].equals("climb-opts:")) {
                // climbing options
                String climb_opts_str = args[argIndex] + 11;
                //boost.tokenizer<boost.char_delimiters_separator<char>> climb_opts{climb_opts_str, boost.char_delimiters_separator<char>{false, ",", ""}};
                String[] climb_opts = climb_opts_str.split(",");
                for (String opt : climb_opts) {
                    int delim_pos = opt.indexOf("=");
                    boolean has_value = (delim_pos != -1);
                    String opt_name = has_value ? opt.substring(0, delim_pos) : opt;
                    String opt_value = has_value ? opt.substring(delim_pos + 1) : opt;
                    //String[] ensure_opt_value =  boolean has_value, String opt_name;
                    ensure_opt_value(has_value, opt_name);
                    if (opt_name.equals("iter-mul") || opt_name.equals("iterations-multiplier")) {
                        ensure_opt_value(has_value, opt_name);
                        iterations_multiplier = Integer.parseInt(opt_value);
                    } else if (opt_name.equals("egc") && opt_name.equals("endgame-commander") && opt_name.equals("min-commander-fusion-level")) {
                        ensure_opt_value(has_value, opt_name);
                        use_fused_commander_level = Integer.parseInt(opt_value);
                    }
                    else if (opt_name.equals("use-all-commander-levels")) {
                        use_top_level_commander = false;
                    }
                    else if (opt_name.equals("use-all-card-levels"))
                    {
                        use_top_level_card = false;
                    }
                    else if (opt_name.equals("otd") || opt_name.equals("open-the-deck")) {
                        mode_open_the_deck = true;
                    }
                    else
                    {
                        System.err.println("Error: Unknown climb option " + opt_name);
                        if (has_value)
                        { System.err.print(" (value is: " + opt_value + ")"); }
                        System.err.println();
                        return 1;
                    }
                }
            } else if (args[argIndex].equals("debug")) {
                //opt_todo.add(e) = std.make_tuple(0, 0, debug));
                Todo todo = new Todo(Operation.DEBUG, 0, 0);
                opt_todo.add(todo);
                opt_num_threads = 1;
            } else if (args[argIndex].equals("debuguntil")) {
                // output the debug info for the first battle that min_score <= score <= max_score.
                // E.g., 0 0: lose; 100 100: win (non-raid); 20 100: at least 20 damage (raid).
                // opt_todo = std.make_tuple((unsigned)atoi(args[argIndex + 1]), (unsigned)atoi(args[argIndex + 2]), debuguntil));
                Todo todo = new Todo(Operation.DEBUG_UNTIL,  Integer.parseInt(args[argIndex + 1]), Integer.parseInt(args[argIndex + 2]));
                opt_todo.add(todo);
                opt_num_threads = 1;
                argIndex += 2;
            } else {
                System.err.println("Error: Unknown option " + args[argIndex]);
                return 1;
            }
        }

        Cards all_cards;
        Decks decks;
        Map<String, String> bge_aliases = new HashMap<>();
        SkillsSetParser.load_skills_set_xml(all_cards, "data/skills_set.xml", true);
        LevelsParser.load_levels_xml(all_cards, "data/levels.xml", true);
        all_cards.fixDominionRecipes();
        for (String suffix: fn_suffix_list) {
            XmlBasedParser.load_decks_xml(decks, all_cards, "data/missions" + suffix + ".xml", "data/raids" + suffix + ".xml", suffix.isEmpty());
            XmlBasedParser.load_recipes_xml(all_cards, "data/fusion_recipes_cj2" + suffix + ".xml", suffix.isEmpty());
            FileParser.readCardAbbrs("data/cardabbrs" + suffix + ".txt");
            // TODO: Abbreviations Parser
            //read_card_abbrs(all_cards, "data/cardabbrs" + suffix + ".txt");
        }
        for (String suffix: fn_suffix_list) {
            FileParser.loadCustomDecks("data/customdecks" + suffix + ".txt");
//            map_keys_to_set(read_custom_cards(all_cards, "data/allowed_candidates" + suffix + ".txt", false), allowed_candidates);
//            map_keys_to_set(read_custom_cards(all_cards, "data/disallowed_candidates" + suffix + ".txt", false), disallowed_candidates);
//            map_keys_to_set(read_custom_cards(all_cards, "data/disallowed_recipes" + suffix + ".txt", false), disallowed_recipes);
        }

        FileParser.readBgeAliases(bge_aliases, "data/bges.txt");

        //FieldSimulator.fillSkillTable();

        if (opt_do_optimization && use_owned_cards) {
            if (opt_owned_cards_str_list.isEmpty()) {
                // load default files only if specify no -o=
                for (String suffix: fn_suffix_list) {
                    String filename = "data/ownedcards" + suffix + ".txt";
                    if (new File(filename).exists()) {
                        opt_owned_cards_str_list.add(filename);
                    }
                }
            }
            Map<Integer, Integer> _owned_cards = new HashMap<>();
            for (String oc_str: opt_owned_cards_str_list) {
                FileParser.readOwnedCards(_owned_cards, oc_str);
            }

            // keep only one copy of alpha dominion
            for (Entry<Integer, Integer> owned_it : _owned_cards.entrySet()) {
                Card owned_card = all_cards.getCardById(owned_it.getKey());
                boolean need_remove = owned_it.getValue() != null;
                if (!need_remove && (owned_card.getCategory() == CardCategory.DOMINION_ALPHA)) {
                    if (owned_alpha_dominion != null) {
                        owned_alpha_dominion = owned_card;
                    } else {
                        System.err.println("Warning: ownedcards already contains alpha dominion (" 
                    + owned_alpha_dominion.getName() + "): removing additional " + owned_card.getName());
                        need_remove = true;
                    }
                }
                if (need_remove) { 
                    _owned_cards.remove(owned_it); 
                } else { 
                    continue; //owned_it.setValue(value); 
                }
            }
            if (owned_alpha_dominion != null && use_dominion_climbing) {
                owned_alpha_dominion = all_cards.getCardById(50002);
                System.err.println();
                System.err.println("Warning: dominion climbing enabled and no alpha dominion found in owned cards, adding default "
                    + owned_alpha_dominion.getName());
            }
            if (owned_alpha_dominion != null) { 
                _owned_cards.set [owned_alpha_dominion.getId()] = 1; 
            }

            // remap owned cards to unordered map (should be quicker for searching)
            owned_cards.reserve(_owned_cards.size());
            for (auto owned_it = _owned_cards.begin(); owned_it != _owned_cards.end(); ++owned_it)
            {
                owned_cards[owned_it->first] = owned_it->second;
            }
        }

        // parse BGEs
        opt_bg_effects[0].fill(0);
        opt_bg_effects[1].fill(0);
        for (int player = 2; player >= 0; -- player) {
            for (String opt_effect: opt_effects[player]) {
                std.unordered_set<String> used_bge_aliases;
                if (!parse_bge(opt_effect, player, bge_aliases, opt_bg_effects[0], opt_bg_effects[1], opt_bg_skills[0], opt_bg_skills[1], used_bge_aliases))
                {
                    return 1;
                }
            }
        }

        String your_deck_name= args[1];
        String enemy_deck_list = args[2];
        List<Deck> deck_list_parsed = DeckParser.parseDeckList(enemy_deck_list, decks);

        Deck your_deck = null;
        List<Deck> enemy_decks;
        List<Double> enemy_decks_factors;

        try {
            your_deck = findDeck(decks, all_cards, your_deck_name).clone();
        } catch(RuntimeException e) {
            System.err.println("Error: Deck " + your_deck_name + ": " + e.getLocalizedMessage());
            return 1;
        }
        if (your_deck == null)
        {
            System.err.println("Error: Invalid attack deck name/hash " + your_deck_name + ".\n");
        }
        else if (!your_deck->variable_cards.empty())
        {
            System.err.println("Error: Invalid attack deck " + your_deck_name + ": has optional cards.\n");
            your_deck = nullptr;
        }
        else if (!your_deck->variable_forts.empty())
        {
            System.err.println("Error: Invalid attack deck " + your_deck_name + ": has optional cards.\n");
            your_deck = nullptr;
        }
        if (your_deck == nullptr)
        {
            usage(args, args);
            return 255;
        }

        your_deck.setDeckStrategy(opt_your_strategy);
        if (!opt_forts.isEmpty()) {
            try {
                your_deck.addForts(opt_forts + ",");
            } catch(RuntimeException e) {
                System.err.println("Error: yfort " + opt_forts + ": " + e.what());
                return 1;
            }
        }
        if (!opt_doms.isEmpty())
        {
            try
            {
                your_deck.addDominions(opt_doms + ",", true);
            }
            catch(RuntimeException e)
            {
                System.err.println("Error: ydom " + opt_doms + ": " + e.what());
                return 1;
            }
        }

        try
        {
            your_deck.setVipCards(opt_vip);
        }
        catch(RuntimeException e)
        {
            System.err.println("Error: vip " + opt_vip + ": " + e.what());
            return 1;
        }

        // parse allowed candidates from options
        try {
            Pair<List<Integer>, Map<Integer, Boolean>> id_marks = DeckParser.stringToIds(all_cards, opt_allow_candidates, "allowed-candidates");
            for (String cid : id_marks.first) {
                allowed_candidates.insert(cid);
            }
        } catch(RuntimeException e) {
            System.err.println("Error: allow-candidates " + opt_allow_candidates + ": " + e.what());
            return 1;
        }

        // parse disallowed candidates from options
        try
        {
            Pair<List<Integer>, Map<Integer, Boolean>> id_marks = DeckParser.stringToIds(all_cards, opt_disallow_candidates, "disallowed-candidates");
            for (String cid : id_marks.first)
            {
                disallowed_candidates.insert(cid);
            }
        }
        catch(RuntimeException e)
        {
            System.err.println("Error: disallow-candidates " + opt_disallow_candidates + ": " + e.what());
            return 1;
        }

        // parse & drop disallowed recipes
        try
        {
            Pair<List<Integer>, Map<Integer, Boolean>>  id_dis_recipes = DeckParser.stringToIds(all_cards, opt_disallow_recipes, "disallowed-recipes");
            for (Integer cid : id_dis_recipes.getFirst()) { all_cards.erase_fusion_recipe(cid); }
        }
        catch(RuntimeException e)
        {
            System.err.println("Error: disallow-recipes " + opt_disallow_recipes + ": " + e.what());
            return 1;
        }
        for (Integer cid : disallowed_recipes)
        { all_cards.eraseFusionRecipe(cid);

        try
        {
            your_deck.setGivenHand(opt_hand);
        }
        catch(RuntimeException e)
        {
            System.err.println("Error: hand " + opt_hand + ": " + e.what());
            return 1;
        }

        if (opt_keep_commander)
        {
            requirement.num_cards[your_deck->commander] = 1;
        }
        for (Entry<Integer, Boolean> card_mark: your_deck.cardMarks.entrySet()) {
            Card card = card_mark.first < 0 ? your_deck.getCommander() : your_deck.getCards()[card_mark.first];
            Boolean mark = card_mark.second;
            if ((mark == '!') && ((card_mark.first >= 0) || !opt_keep_commander))
            {
                requirement.num_cards[card] += 1;
            }
        }

        target_score = opt_target_score.isEmpty() ? max_possible_score[(size_t)optimization_mode] : boost.lexical_cast<Double>(opt_target_score);

        for (Deck deck_parsed: deck_list_parsed) {
            Deck enemy_deck = null;
            try
            {
                enemy_deck = find_deck(decks, all_cards, deck_parsed.first);
            }
            catch(RuntimeException e)
            {
                System.err.println("Error: Deck " + deck_parsed.first + ": " + e.what());
                return 1;
            }
            if (enemy_deck == nullptr)
            {
                System.err.println("Error: Invalid defense deck name/hash " + deck_parsed.first + ".\n");
                usage(args, args);
                return 1;
            }
            if (optimization_mode == OptimizationMode.notset)
            {
                if (enemy_deck->decktype == DeckType.raid)
                {
                    optimization_mode = OptimizationMode.raid;
                }
                else if (enemy_deck->decktype == DeckType.campaign)
                {
                    gamemode = GameMode.SURGE;
                    optimization_mode = OptimizationMode.campaign;
                }
                else
                {
                    optimization_mode = OptimizationMode.winrate;
                }
            }
            enemy_deck.setDeckStrategy(opt_enemy_strategy);
            if (!opt_enemy_doms.isEmpty())
            {
                try
                {
                    enemy_deck.addDominions(opt_enemy_doms + ",", true);
                }
                catch(RuntimeException e)
                {
                    System.err.println("Error: edom " + opt_enemy_doms + ": " + e.getLocalizedMessage());
                    return 1;
                }
            }
            if (!opt_enemy_forts.isEmpty())
            {
                try
                {
                    enemy_deck.addForts(opt_enemy_forts + ",");
                }
                catch(RuntimeException e)
                {
                    System.err.println("Error: efort " + opt_enemy_forts + ": " + e.getLocalizedMessage());
                    return 1;
                }
            }
            try
            {
                enemy_deck.setGivenHand(opt_enemy_hand);
            }
            catch(RuntimeException e)
            {
                System.err.println("Error: enemy:hand " + opt_enemy_hand + ": " + e.getLocalizedMessage());
                return 1;
            }
            enemy_decks.add(enemy_deck);
            enemy_decks_factors = deck_parsed.getV;
        }

        // Force to claim cards in your initial deck.
        if (opt_do_optimization && use_owned_cards) {
            claim_cards(Arrays.asList(your_deck.getCommander()));
            claim_cards(your_deck.getCards());
            if (your_deck.getAlphaDominion() != null) claim_cards(Arrays.asList(your_deck.getAlphaDominion()));
        }

        // shrink any oversized deck to maximum of 10 cards + commander
        // NOTE: do this AFTER the call to claim_cards so that passing an initial deck of >10 cards
        //       can be used as a "shortcut" for adding them to owned cards. Also this allows climb
        //       to figure out which are the best 10, rather than restricting climb to the first 10.
        if (your_deck.cards.size() > max_deck_len)
        {
            your_deck.shrink(max_deck_len);
            if (debug_print >= 0)
            {
                System.err.println("WARNING: Too many cards in your deck. Trimmed.\n");
            }
        }
        freezed_cards = Math.min(freezed_cards, your_deck.cards.size());

        if (debug_print >= 0)
        {
            System.out.print("Your Deck: " + (debug_print > 0 ? your_deck.long_description() : your_deck.medium_description()));
            for (unsigned bg_effect = PassiveBGE.no_bge; bg_effect < PassiveBGE.num_passive_bges; ++bg_effect)
            {
                auto bge_value = opt_bg_effects[0][bg_effect];
                if (!bge_value)
                    continue;
                System.out.print("Your BG Effect: " + passive_bge_names[bg_effect]);
                if (bge_value != -1) System.out.print(" " + bge_value);
                System.out.println();
            }
            for (String bg_skill: opt_bg_skills[0])
            {
                System.out.println("Your BG Skill: " + skill_description(all_cards, bg_skill));
            }

            for (int i =0; i < enemy_decks.size(); ++i) {
                auto enemy_deck = enemy_decks[i];
                System.out.println("Enemy's Deck:" + enemy_decks_factors[i] + ": "
                    + (debug_print > 0 ? enemy_deck.long_description() : enemy_deck.medium_description()));
            }
            for (unsigned bg_effect = PassiveBGE.no_bge; bg_effect < PassiveBGE.num_passive_bges; ++bg_effect)
            {
                auto bge_value = opt_bg_effects[1][bg_effect];
                if (!bge_value)
                    continue;
                System.out.print("Enemy's BG Effect: " + passive_bge_names[bg_effect]);
                if (bge_value != -1) System.out.print(" " + bge_value + "\n");
            }
            for (String bg_skill: opt_bg_skills[1])
            {
                System.out.println("Enemy's BG Skill: " + skill_description(all_cards, bg_skill));
            }
        }
        if (enemy_decks.size() == 1) {
            Deck enemy_deck = enemy_decks.get(0);
            for (auto x_mult_ss : enemy_deck.getEffects()) {
                if (debug_print >= 0) {
                    System.out.print("Enemy's X-Mult BG Skill (effective X = round_up[X * " + enemy_deck.level + "]): "
                        + skill_description(all_cards, x_mult_ss));
                    if (x_mult_ss.x) { System.out.print(" (eff. X = " + ceil(x_mult_ss.x * enemy_deck.level) + ")"); }
                    System.out.print();
                }
                opt_bg_skills[1] = new SkillSpec[] {x_mult_ss.id, ceil(x_mult_ss.x * enemy_deck.getLevel),
                    x_mult_ss.y, x_mult_ss.n, x_mult_ss.c, x_mult_ss.s, x_mult_ss.s2, x_mult_ss.all};
            }
        }
        
        // TODO: nicht mehr nötig?

//        Process p(opt_num_threads, all_cards, decks, your_deck, enemy_decks, enemy_decks_factors, gamemode,
//            opt_bg_effects[0], opt_bg_effects[1], opt_bg_skills[0], opt_bg_skills[1]);

        for (Todo op: opt_todo) {
            switch(op.operation) {
            case noop:
                break;
            case simulate: {
                EvaluatedResults results = { EvaluatedResults.first_type(enemy_decks.size()), 0 };
                results = p.evaluate(std.get<0>(op), results);
                print_results(results, p.factors);
                break;
            }
            case climb: {
                //hill_climbing(op.iterations, op.iterations.get, your_deck, p, requirement);
                hill_climbing(op.iterations, op.iterationsFine, your_deck, p, requirement);
                break;
            }
            case reorder: 
                your_deck.setStrategy(DeckStrategy.ORDERED);
                use_owned_cards = true;
                use_top_level_card = false;
                use_top_level_commander = false;
                use_dominion_climbing = false;
                if (min_deck_len == 1 && max_deck_len == 10)
                {
                    min_deck_len = max_deck_len = your_deck.cards.size();
                }
                fund = 0;
                debug_print = -1;
                owned_cards.clear();
                claim_cards(Arrays.asList(your_deck.getCommander()));
                claim_cards(your_deck.getCards());
                hill_climbing(std.get<0>(op), std.get<1>(op), your_deck, p, requirement
                );
                break;
            case debug: 
                ++ debug_print;
                debug_str.clear();
                EvaluatedResults results = {EvaluatedResults.first_type(enemy_decks.size()), 0};
                results = p.evaluate(1, results);
                print_results(results, p.factors);
                -- debug_print;
                break;
            case debuguntil: 
                ++ debug_print;
                ++ debug_cached;
                while (true) {
                    debug_str.clear();
                    EvaluatedResults results = {EvaluatedResults.first_type(enemy_decks.size()), 0};
                    results = p.evaluate(1, results);
                    auto score = compute_score(results, p.factors);
                    if (score.points >= op.iterations && score.points <= op.iterationsFine) {
                        System.out.print(debug_str);
                        print_results(results, p.factors);
                        break;
                    }
                }
                -- debug_cached;
                -- debug_print;
                break;
        }
        return 0;
    }
    
    private void claim_cards(List<Card> card_list) {
        Map<Card, Integer> num_cards = new HashMap<>();
        getRequiredCardsBeforeUpgrade(card_list, num_cards);
        for (Entry<Card, Integer> it: num_cards.entrySet()) {
            Card card = it.getKey();
            Integer num_to_claim = Utils.safeMinus(it.getValue(), owned_cards.get(card.getId()));
            if (num_to_claim > 0) {
                
                //NumStop = 
                AnragWiederV = owned_cards.get(card.getId()) += num_to_claim;
                owned_cards = 
                if (Main.debug_print >= 0)
                {
                    System.err.println("WARNING: Need extra " + num_to_claim + " " + card.getName() + " to build your initial deck: adding to owned card list.\n");
                }
            }
        }
    }
    
    private void ensure_opt_value(boolean has_value, String opt_name) {
            if (!has_value) { throw new RuntimeException("climb-opts:" + opt_name + " requires an argument"); }
    }
}
