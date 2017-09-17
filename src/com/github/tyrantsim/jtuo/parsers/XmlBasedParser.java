package com.github.tyrantsim.jtuo.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.tyrantsim.jtuo.Constants;
import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardCategory;
import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.cards.Cards;
import com.github.tyrantsim.jtuo.cards.Faction;
import com.github.tyrantsim.jtuo.decks.Deck;
import com.github.tyrantsim.jtuo.decks.DeckType;
import com.github.tyrantsim.jtuo.decks.Decks;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
/**
 * @author Brikikeks
 */
public class XmlBasedParser {

    public static Map<Integer, Card> cards = new TreeMap<>();

    public static boolean initialized = false;

    static {// cards.putAll(readCards("cards_section_9.xml"));
    }

    public static void initialize() {
        if (!initialized) {
            try {
                readRaids("raids.xml");
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
            }
        }
        initialized = true;
        // Cards.allCards.addAll(cards.values());
        // Cards.organize();
    }

    public static Card getCardCopy(int id) {
        return cards.get(id).clone();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CardsParser.initialize();
        XmlBasedParser.initialize();
    }

    public static void readDocument(String file) throws FileNotFoundException {

        File cardFile = new File(new File(".", Constants.DATA), file);
        if (!cardFile.getParentFile().exists()) {
            if (!cardFile.getParentFile().mkdirs()) {
                System.err.println("Failed to create data folder");
            }
        }
        System.out.println(file);

        if (!cardFile.exists() || ((new Date().getTime() - cardFile.lastModified()) > 86400000)) {
            try {
                // http://mobile.tyrantonline.com/assets/cards_section_1.xml
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Constants.GITHUB + file);
                // cards.putAll(
                readRaids(document);
                // );

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(document);
                StreamResult console = new StreamResult(cardFile);
                transformer.transform(source, console);
            } catch (FileNotFoundException fnfe) {
                throw fnfe;
            } catch (SAXException | IOException | ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException e) {
                e.printStackTrace();
            }
        } else {
            try {
                //cards.putAll(
                        readRaids(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cardFile));
            } catch (SAXException | ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readRaids(Document document) {

        Node root = document.getFirstChild();

        if (root == null) {
            return;
        }
        NodeList raids = document.getElementsByTagName("raid");
        for (int i = 0; i < raids.getLength(); i++) {
            // raid_node = raid_node->next_sibling("raid"))
            Node raid = raids.item(i);
            NodeList childes = raid.getChildNodes(); //
            int id = 0;
            String deck_name = null;
            String idString = null;
            for (int childe_id = 0; childe_id < childes.getLength(); childe_id++) {
                Node childe_node = childes.item(childe_id); //
                assert (childe_node != null);
                if (childe_node.getNodeName().equals("id")) {
                    idString = childe_node.getTextContent();
                    System.out.println(idString);

                    id = idString != null ? Integer.parseInt(idString) : 0;
                }
                if (childe_node.getNodeName().equals("name")) {
                    // System.out.println(childe_node.getTextContent());
                    String nameString = childe_node.getTextContent();
                    // int id = childe_node.getTextContent() != null ?
                    // Integer.parseInt(childe_node.getTextContent()) : 0;
                    deck_name = nameString;
                    System.out.println("" + idString + "");
                    System.out.println(deck_name);
                }
                // xml_node<>* name_node(raid_node->first_node("name"));

            }
            readDeck((Element) raid, DeckType.RAID, id, deck_name);
            // int id = id_node ? Integer.parseInt(id_node.getNodeValue()) : 0;
            // xml_node<>* name_node(raid_node->first_node("name"));
            // std::string deck_name{name_node->value()};
            // try
            // {
            // read_deck(decks, all_cards, raid_node, DeckType::raid, id,
            // deck_name);
            // }
            // catch (const std::runtime_error& e)
            // {
            // std::cerr << "WARNING: Failed to parse raid [" << deck_name << "]
            // in file " << filename << ": [" << e.what() << "]. Skip the
            // raid.\n";
            // continue;
            // }
        }

        // for (xml_node<>* campaign_node = root->first_node("campaign");
        // campaign_node;
        // campaign_node = campaign_node->next_sibling("campaign"))
        // {
        // xml_node<>* id_node(campaign_node->first_node("id"));
        // unsigned id(id_node ? atoi(id_node->value()) : 0);
        // for (auto && name_node = campaign_node->first_node("name");
        // name_node;
        // name_node = name_node->next_sibling("name"))
        // {
        // try
        // {
        // read_deck(decks, all_cards, campaign_node, DeckType::campaign, id,
        // name_node->value());
        // }
        // catch (const std::runtime_error& e)
        // {
        // std::cerr << "WARNING: Failed to parse campaign [" <<
        // name_node->value() << "] in file " << filename << ": [" << e.what()
        // << "]. Skip the campaign.\n";
        // continue;
        // }
        // }
        // }
    }

    private static Deck readDeck(Element raidElement, DeckType raid2, int id, String base_deck_name) {
        // TODO: implement Raid parser

        // int id =
        // Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent());
        String name = raidElement.getElementsByTagName("name").item(0).getTextContent();

        Deck deck = new Deck();
        String deckName = "";
        int levels = Integer.parseInt(raidElement.getElementsByTagName("levels").item(0).getTextContent());
        Card commander_card;
        try {
            commander_card = Cards.getCardById(Integer.parseInt(raidElement.getElementsByTagName("commander").item(0).getTextContent()));
            int commander_max_level = commander_card.getTopLevelCard().getLevel();
            if (raidElement.getElementsByTagName("commander_max_level").getLength() > 0) {
                commander_max_level = Integer.parseInt(raidElement.getElementsByTagName("commander_max_level").item(0).getTextContent());
            }
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        NodeList fortressCardNodeList = raidElement.getElementsByTagName("fortress_card");
        for (int i = 0; i < fortressCardNodeList.getLength(); i++) {
            Node fortressCardNode = fortressCardNodeList.item(i);
            try {
                Card fortressCard = Cards.getCardById(Integer.parseInt(fortressCardNode.getAttributes().getNamedItem("id").getTextContent()));
                System.out.println(fortressCard.getName());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (raidElement.getElementsByTagName("effects").getLength() > 0) {
            NodeList effectsNodeList = ((Element) raidElement.getElementsByTagName("effects").item(0)).getElementsByTagName("skill");
            for (int i = 0; i < effectsNodeList.getLength(); i++) {
                Node skillNode = effectsNodeList.item(i);
                String skillId = skillNode.getAttributes().getNamedItem("id").getTextContent();
                Boolean all = null;
                if (skillNode.getAttributes().getNamedItem("all") != null) {
                    all = Boolean.parseBoolean(skillNode.getAttributes().getNamedItem("all").getTextContent());
                }
                Float x = null;
                if (skillNode.getAttributes().getNamedItem("x") != null) {
                    x = Float.parseFloat(skillNode.getAttributes().getNamedItem("x").getTextContent());
                }
                try {
                    SkillSpec skillSpec = new SkillSpec(Skill.valueOf(skillId.toUpperCase()), x, Faction.ALL_FACTIONS, 0, 0, null, null, all, 0, SkillTrigger.PLAY);                    
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
        }
        if (raidElement.getElementsByTagName("fortress_pool").getLength() > 0) {
            Element fortressPoolElement = (Element) raidElement.getElementsByTagName("fortress_pool").item(0);
            Integer amount = Integer.parseInt(fortressPoolElement.getAttributes().getNamedItem("amount").getTextContent());
            NodeList fortressPoolCards = fortressPoolElement.getElementsByTagName("card");
            for (int i = 0; i < fortressPoolCards.getLength(); i++) {
                try {
                    Card fortressCard = Cards.getCardById(Integer.parseInt(fortressPoolCards.item(i).getTextContent()));
                    Integer replicates = Integer.parseInt(fortressPoolCards.item(i).getAttributes().getNamedItem("replicates").getTextContent());
                    System.out.println(fortressCard.getName() + "-" + replicates);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Element deckElement = (Element) raidElement.getElementsByTagName("deck").item(0);
        if (raidElement.getElementsByTagName("always_include").getLength() > 0) {
            NodeList alwaysIncludeCardsNodeList = ((Element) deckElement.getElementsByTagName("always_include").item(0)).getElementsByTagName("card");
            for (int i = 0; i < alwaysIncludeCardsNodeList.getLength(); i++) {
                try {
                    Card alwaysIncludeCard = Cards.getCardById(Integer.parseInt(alwaysIncludeCardsNodeList.item(i).getTextContent()));
                    Integer replicates = 1;
                    if (alwaysIncludeCardsNodeList.item(i).getAttributes().getNamedItem("replicates") != null) {
                        replicates = Integer.parseInt(alwaysIncludeCardsNodeList.item(i).getAttributes().getNamedItem("replicates").getTextContent());
                    }
                    System.out.println(alwaysIncludeCard.getName() + "-" + replicates);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        if (raidElement.getElementsByTagName("card_pool").getLength() > 0) {
            for (int i = 0; i < raidElement.getElementsByTagName("card_pool").getLength(); i++) {
                NodeList cardPoolNodeList = ((Element) deckElement.getElementsByTagName("card_pool").item(0)).getElementsByTagName("card");
                Integer amount = Integer.parseInt(deckElement.getElementsByTagName("card_pool").item(0).getAttributes().getNamedItem("amount").getTextContent());
                for (int j = 0; j < cardPoolNodeList.getLength(); j++) {
                    try {
                        Card cardPoolCard = Cards.getCardById(Integer.parseInt(cardPoolNodeList.item(i).getTextContent()));
                        System.out.println(cardPoolCard.getName());
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }
        // std::vector<const Card*> always_cards;
        // std::vector<std::tuple<unsigned, unsigned, std::vector<const Card*>>>
        // some_forts;
        // std::vector<std::tuple<unsigned, unsigned, std::vector<const Card*>>>
        // some_cards;
        // xml_node<>* levels_node(node->first_node("levels"));
        // xml_node<>* effects_node(node->first_node("effects"));
        // xml_node<>* deck_node(node->first_node("deck"));
        // unsigned max_level = levels_node ? atoi(levels_node->value()) : 10;
        //
        // // Effectes (skill based BGEs; assuming that X is a floating point
        // number (multiplier))
        // std::vector<SkillSpecXMult> effects;
        // if (effects_node)
        // {
        // for (xml_node<>* skill_node = effects_node->first_node("skill");
        // skill_node;
        // skill_node = skill_node->next_sibling("skill"))
        // {
        // auto skill_name = skill_node->first_attribute("id")->value();
        // Skill::Skill skill_id = skill_name_to_id(skill_name);
        // if (skill_id == Skill::no_skill) { throw std::runtime_error("unknown
        // skill id:" + to_string(skill_name)); }
        // auto x = node_value_float(skill_node, "x", 0.0);
        // auto y = skill_faction(skill_node);
        // auto n = node_value(skill_node, "n", 0);
        // auto c = node_value(skill_node, "c", 0);
        // auto s = skill_target_skill(skill_node, "s");
        // auto s2 = skill_target_skill(skill_node, "s2");
        // bool all(skill_node->first_attribute("all"));
        // effects.push_back({skill_id, x, y, n, c, s, s2, all});
        // }
        // }
        //
        // // Fixed fortresses (<fortress_card id="xxx"/>)
        // std::vector<const Card*> fortress_cards;
        // for (xml_node<>* fortress_card_node =
        // node->first_node("fortress_card");
        // fortress_card_node;
        // fortress_card_node =
        // fortress_card_node->next_sibling("fortress_card"))
        // {
        // const Card* card =
        // all_cards.by_id(atoi(fortress_card_node->first_attribute("id")->value()));
        // fortress_cards.push_back(card);
        // upgrade_opportunities += card->m_top_level_card->m_level -
        // card->m_level;
        // }
        //
        // // Variable fortresses (<fortress_pool amount="x" replicates="y"> ...
        // </fortress_pool>)
        // for (xml_node<>* fortress_pool_node =
        // node->first_node("fortress_pool");
        // fortress_pool_node;
        // fortress_pool_node =
        // fortress_pool_node->next_sibling("fortress_pool"))
        // {
        // unsigned
        // num_cards_from_pool(atoi(fortress_pool_node->first_attribute("amount")->value()));
        // unsigned
        // pool_replicates(fortress_pool_node->first_attribute("replicates")
        // ? atoi(fortress_pool_node->first_attribute("replicates")->value())
        // : 1);
        // std::vector<const Card*> cards_from_pool;
        // unsigned upgrade_points = 0;
        // for (xml_node<>* card_node = fortress_pool_node->first_node("card");
        // card_node;
        // card_node = card_node->next_sibling("card"))
        // {
        // card = all_cards.by_id(atoi(card_node->value()));
        // unsigned card_replicates(card_node->first_attribute("replicates") ?
        // atoi(card_node->first_attribute("replicates")->value()) : 1);
        // while (card_replicates --)
        // {
        // cards_from_pool.push_back(card);
        // upgrade_points += card->m_top_level_card->m_level - card->m_level;
        // }
        // }
        // some_forts.push_back(std::make_tuple(num_cards_from_pool,
        // pool_replicates, cards_from_pool));
        // upgrade_opportunities += upgrade_points * num_cards_from_pool *
        // pool_replicates / cards_from_pool.size();
        // }
        //
        // // Fixed cards (<always_include> ... </always_include>)
        // xml_node<>* always_node{deck_node->first_node("always_include")};
        // for (xml_node<>* card_node = (always_node ? always_node :
        // deck_node)->first_node("card");
        // card_node;
        // card_node = card_node->next_sibling("card"))
        // {
        // card = all_cards.by_id(atoi(card_node->value()));
        // unsigned replicates(card_node->first_attribute("replicates") ?
        // atoi(card_node->first_attribute("replicates")->value()) : 1);
        // while (replicates --)
        // {
        // always_cards.push_back(card);
        // upgrade_opportunities += card->m_top_level_card->m_level -
        // card->m_level;
        // }
        // }
        //
        // // Variable cards (<card_pool amount="x" replicates="y"> ...
        // </card_pool>)
        // for (xml_node<>* pool_node = deck_node->first_node("card_pool");
        // pool_node;
        // pool_node = pool_node->next_sibling("card_pool"))
        // {
        // unsigned
        // num_cards_from_pool(atoi(pool_node->first_attribute("amount")->value()));
        // unsigned pool_replicates(pool_node->first_attribute("replicates") ?
        // atoi(pool_node->first_attribute("replicates")->value()) : 1);
        // std::vector<const Card*> cards_from_pool;
        // unsigned upgrade_points = 0;
        // for (xml_node<>* card_node = pool_node->first_node("card");
        // card_node;
        // card_node = card_node->next_sibling("card"))
        // {
        // card = all_cards.by_id(atoi(card_node->value()));
        // unsigned card_replicates(card_node->first_attribute("replicates") ?
        // atoi(card_node->first_attribute("replicates")->value()) : 1);
        // while (card_replicates --)
        // {
        // cards_from_pool.push_back(card);
        // upgrade_points += card->m_top_level_card->m_level - card->m_level;
        // }
        // }
        // some_cards.push_back(std::make_tuple(num_cards_from_pool,
        // pool_replicates, cards_from_pool));
        // upgrade_opportunities += upgrade_points * num_cards_from_pool *
        // pool_replicates / cards_from_pool.size();
        // }
        //
        // // Mission requirement
        // xml_node<>* mission_req_node(node->first_node(decktype ==
        // DeckType::mission ? "req" : "mission_req"));
        // unsigned mission_req(mission_req_node ?
        // atoi(mission_req_node->value()) : 0);
        //
        // for (unsigned level = 1; level < max_level; ++ level)
        // {
        // std::string deck_name = base_deck_name + "-" + to_string(level);
        // unsigned upgrade_points = ceil(upgrade_opportunities * (level - 1) /
        // (double)(max_level - 1));
        // decks.decks.push_back(Deck{all_cards, decktype, id, deck_name,
        // upgrade_points, upgrade_opportunities});
        // Deck* deck = &decks.decks.back();
        // deck->set(commander_card, commander_max_level, always_cards,
        // some_forts, some_cards, mission_req);
        // deck->fortress_cards = fortress_cards;
        // deck->effects = effects;
        // deck->level = level;
        // decks.add_deck(deck, deck_name);
        // decks.add_deck(deck, decktype_names[decktype] + " #" + to_string(id)
        // + "-" + to_string(level));
        // }
        //
        // decks.decks.push_back(Deck{all_cards, decktype, id, base_deck_name});
        // Deck* deck = &decks.decks.back();
        // deck->set(commander_card, commander_max_level, always_cards,
        // some_forts, some_cards, mission_req);
        // deck->fortress_cards = fortress_cards;
        // deck->effects = effects;
        // deck->level = max_level;
        //
        // // upgrade cards for full-level missions/raids
        // if (max_level > 1)
        // {
        // while (deck->commander->m_level < commander_max_level)
        // { deck->commander = deck->commander->upgraded(); }
        // for (auto && card: deck->fortress_cards)
        // { card = card->m_top_level_card; }
        // for (auto && card: deck->cards)
        // { card = card->m_top_level_card; }
        // for (auto && pool: deck->variable_forts)
        // {
        // for (auto && card: std::get<2>(pool))
        // { card = card->m_top_level_card; }
        // }
        // for (auto && pool: deck->variable_cards)
        // {
        // for (auto && card: std::get<2>(pool))
        // { card = card->m_top_level_card; }
        // }
        // }
        //
        // decks.add_deck(deck, base_deck_name);
        // decks.add_deck(deck, base_deck_name + "-" + to_string(max_level));
        // decks.add_deck(deck, decktype_names[decktype] + " #" +
        // to_string(id));
        // decks.add_deck(deck, decktype_names[decktype] + " #" + to_string(id)
        // + "-" + to_string(max_level));
        // decks.by_type_id[{decktype, id}] = deck;
        return deck;
    }

    private static void updateSkills(Card card, ArrayList<SkillSpec> skillSpecs) throws AssertionError {
        card.setSkills(skillSpecs);
        card.setSkillsOnPlay(new ArrayList<>());
        card.setSkillsOnDeath(new ArrayList<>());
        for (SkillSpec skillSpec : skillSpecs) {
            if (skillSpec.getTrigger() != null) {
                // add a new one
                switch (skillSpec.getTrigger()) {
                case ACTIVATE:
                    break;
                case PLAY:
                    card.getSkillsOnPlay().add(skillSpec);
                    break;
                case DEATH:
                    card.getSkillsOnDeath().add(skillSpec);
                    break;
                default:
                    throw new AssertionError("No storage for skill with trigger " + skillSpec.getTrigger());
                }
            }
        }
    }


    private static void setSkillSpec(Card card, String x, String y, String all, String c, String card_id, String trigger, SkillSpec new_skill, String n, String s, String s2) {
        new_skill.setAll(all != null && all.equals("1"));
        if (!x.isEmpty()) {
            new_skill.setX(Float.parseFloat(x));
        }
        if (!y.isEmpty()) {
            new_skill.setY(Faction.values()[Integer.parseInt(y)]);
        }

        if (!c.isEmpty()) {
            new_skill.setC(Integer.parseInt(c));
        }
        if (!card_id.isEmpty()) {
            new_skill.setCardId(Integer.parseInt(card_id));
        }
        if (trigger != null && !trigger.isEmpty()) {
            new_skill.setTrigger(SkillTrigger.valueOf(trigger.toUpperCase()));
        }
    }

    public static void load_decks_xml(Decks decks, Cards all_cards, String string, String string2, boolean empty) {
        // TODO Auto-generated method stub

    }

    public static void load_recipes_xml(Cards all_cards, String string, boolean empty) {
        // TODO Auto-generated method stub

    }

    public static void readRaids(String filename, boolean... do_warn_on_missing) throws FileNotFoundException {
        readDocument(filename);
    }
}