package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.*;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.parsers.CardsParser;
import com.github.tyrantsim.jtuo.parsers.DeckParser;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
import org.junit.Test;

import java.util.*;

public class FieldSimulatorTest {

    private static final String yourDeckString = "[70000], [70001]";
    private static final String enemyDeckString = "[70000], [70003]";
    private static final Random random = new Random();
    private static List<Deck> enemyDecks = new ArrayList<>();

    /*
    @BeforeClass
    public static void loadCards() throws Exception {
        CardsParser.initialize();
    }
    */

    @Test
    public void testFieldSim() throws Exception {

        createCustomCards();

        CardsParser.initialize();

        Deck yourDeck;

        try {
            yourDeck = Decks.findDeck(yourDeckString).clone();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error: Deck " + yourDeckString + ": " + e.getMessage());
            return;
        }

        if (yourDeck == null) {
            System.err.println("Error: Invalid attack deck name/hash " + yourDeckString);
            return;
        }

        Map<String, Double> deckListParsed = DeckParser.parseDeckList(enemyDeckString);

        for (String deckStr: deckListParsed.keySet()) {

            // Construct the enemy decks
            Deck deck;
            try {
                deck = Decks.findDeck(deckStr).clone();
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.err.println("Error: Deck " + deckStr + ": " + e.getMessage());
                return;
            }

            if (deck == null) {
                System.err.println("Error: Invalid attack deck name/hash " + deckStr);
                return;
            }

            enemyDecks.add(deck);
        }

        // Passive BGEs
        int[] passiveBGEs = new int[PassiveBGE.values().length];
        passiveBGEs[PassiveBGE.DEVOUR.ordinal()] = 1;

        // BGE Activation Skills
        List<SkillSpec> bgSkills = new ArrayList<>();
//        bgSkills.add(new SkillSpec(Skill.ENHANCE, 50, Faction.ALL_FACTIONS, 0, 0,
//                Skill.POISON, Skill.NO_SKILL, false, 0, SkillTrigger.ACTIVATE));
//        bgSkills.add(new SkillSpec(Skill.STRIKE, 3, Faction.ALL_FACTIONS, 0, 0,
//                Skill.NO_SKILL, Skill.NO_SKILL, true, 0, SkillTrigger.ACTIVATE));

        // Prepare decks
        Hand yourHand = new Hand(yourDeck);
        Hand enemyHand = new Hand(enemyDecks.get(0));
        yourHand.reset(random);
        enemyHand.reset(random);

        Field fd = new Field(
                random,
                null,
                yourHand, enemyHand,
                GameMode.FIGHT,
                OptimizationMode.WINRATE,
//                passiveBGEs, passiveBGEs,
                bgSkills, bgSkills
        );
        FieldSimulator.play(fd);
        Results result = FieldSimulator.play(fd);
        System.out.println(result);

        System.out.println("Done");
    }

    @Test
    public void testCardSkills() throws Exception {
        CardsParser.initialize();
        Card card = Cards.getCardByName("Alpha Replicant");
        System.out.println(Cards.cardDescription(card));
        System.out.println(card.getCategory());
    }

    @Test
    public void testDeck() throws Exception {

        CardsParser.initialize();

        Deck yourDeck;
        try {
            yourDeck = Decks.findDeck(yourDeckString).clone();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error: Deck " + yourDeckString + ": " + e.getMessage());
            return;
        }

        if (yourDeck == null) {
            System.err.println("Error: Invalid attack deck name/hash " + yourDeckString);
            return;
        }

        Hand yourHand = new Hand(yourDeck);
        yourHand.reset(new Random());

        System.out.println(yourHand.getStructures().get(0));

    }

    public void createCustomCards() {

        // Custom test cards
        Card noSkillsCommander = new Card(
                70000,
                70000,
                CardType.COMMANDER,
                CardCategory.NORMAL,
                "Commander Test Card",
                6,
                Faction.PROGENITOR,
                Rarity.MYTHIC.ordinal(),
                2,
                0,
                0,
                50,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new int[Skill.values().length],
                new SkillTrigger[Skill.values().length],
                null,
                0,
                new HashMap<>(),
                new HashMap<>()
        );

        noSkillsCommander.setTopLevelCard(noSkillsCommander);

        Cards.allCards.add(noSkillsCommander);

        ////////////////////////////////////////////////////////////////////
        Card c = new Card(
                70001,
                70001,
                CardType.ASSAULT,
                CardCategory.NORMAL,
                "Jam Test Card",
                1,
                Faction.PROGENITOR,
                Rarity.MYTHIC.ordinal(),
                0,
                0,
                15,
                70,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new int[Skill.values().length],
                new SkillTrigger[Skill.values().length],
                null,
                0,
                new HashMap<>(),
                new HashMap<>()
        );

        c.setTopLevelCard(c);

        c.addSkill(SkillTrigger.ACTIVATE, Skill.JAM, 0, Faction.ALL_FACTIONS, 2, 3,
                Skill.NO_SKILL, Skill.NO_SKILL, false, 0);

        Cards.allCards.add(c);

        ////////////////////////////////////////////////////////////////////
        Card c2 = new Card(
                70002,
                70002,
                CardType.ASSAULT,
                CardCategory.NORMAL,
                "Summoned Card",
                1,
                Faction.PROGENITOR,
                Rarity.MYTHIC.ordinal(),
                0,
                0,
                1,
                700,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new int[Skill.values().length],
                new SkillTrigger[Skill.values().length],
                null,
                0,
                new HashMap<>(),
                new HashMap<>()
        );

        c2.setTopLevelCard(c2);

        Cards.allCards.add(c2);

        ////////////////////////////////////////////////////////////////////
        Card c3 = new Card(
                70003,
                70003,
                CardType.ASSAULT,
                CardCategory.NORMAL,
                "Defender Test Card",
                1,
                Faction.PROGENITOR,
                Rarity.MYTHIC.ordinal(),
                0,
                0,
                5,
                100,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new int[Skill.values().length],
                new SkillTrigger[Skill.values().length],
                null,
                0,
                new HashMap<>(),
                new HashMap<>()
        );

        c3.setTopLevelCard(c3);

        Cards.allCards.add(c3);

    }

}

