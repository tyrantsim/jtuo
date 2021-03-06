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
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
/**
 * @author Brikikeks
 */
public class SkillsSetParser {

    public static Map<Integer, Card> cards = new TreeMap<>();

    public static boolean initialized = false;

    static {// cards.putAll(readCards("cards_section_9.xml"));
    }

    public static void initialize() {
        if (!initialized) {
            for (int i = 1; i < 100; i++) {
                try {
                    readCards("cards_section_" + i + ".xml");
                } catch (FileNotFoundException e) {
                    // e.printStackTrace();
                    break;
                }
            }
        }
        initialized = true;
        Cards.allCards.addAll(cards.values());
        Cards.organize();
    }

    public static Card getCardCopy(int id) {
        return cards.get(id).clone();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SkillsSetParser.initialize();
    }

    public static void readCards(String file) throws FileNotFoundException {

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
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Constants.ASSETS + file);
                cards.putAll(readCards(document));

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
                cards.putAll(readCards(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cardFile)));
            } catch (SAXException | ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<Integer, Card> readCards(Document parse) {
        TreeMap<Integer, Card> cards = new TreeMap<>();
        NodeList units = parse.getElementsByTagName("unit");
        for (int i = 0; i < units.getLength(); i++) {
            Node unit = units.item(i);
            NodeList unitChilds = unit.getChildNodes();
            String name = "";
            String baseId = "";
            Integer baseIdInt = null;
            Integer idInt = null;
            Card baseCard = new Card();
            ArrayList<SkillSpec> skillSpecs = new ArrayList<>();
            Map<Integer, Card> upgrades = new TreeMap<>();
            for (int j = 0; j < unitChilds.getLength(); j++) {
                Node unitChild = unitChilds.item(j);
                //System.out.println(unitChild.getNodeName());
                if (unitChild.getNodeName().equals("id")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        baseId = unitChild.getFirstChild().getNodeValue();
                        baseIdInt = Integer.parseInt(baseId);
                    }
                    baseCard.setBaseId(baseIdInt);
                    baseCard.setId(baseIdInt);
                }
                if (unitChild.getNodeName().equals("name")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        name = unitChild.getFirstChild().getNodeValue();
                        baseCard.setName(name);
                    }
                }

                if (unitChild.getNodeName().equals("rarity")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        baseCard.setRarity(Integer.parseInt(unitChild.getFirstChild().getNodeValue()));
                    }
                }
                
                if (unitChild.getNodeName().equals("type")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        baseCard.setFaction(Faction.values()[Integer.parseInt(unitChild.getFirstChild().getNodeValue())]);
                    }
                }
                if (unitChild.getNodeName().equals("fortress_type")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        switch (Integer.parseInt(unitChild.getFirstChild().getNodeValue())) {
                        case 1:
                            baseCard.setCategory(CardCategory.FORTRESS_DEFENSE);                            
                            break;
                        case 2:
                            baseCard.setCategory(CardCategory.FORTRESS_SIEGE);
                        default:
                            break;
                        }
                    }
                }
                
                if (unitChild.getNodeName().equals("set")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        int set = Integer.parseInt(unitChild.getFirstChild().getNodeValue());
                        baseCard.setSet(set);
                    }
                }
                if (baseCard != null) {
                    updateSameCardAttributes(unitChild, baseCard, skillSpecs);
                }
                
                if (baseIdInt != null && name != null && !name.isEmpty()) {
                    cards.put(baseIdInt, baseCard);

                    // System.out.println("" + name);
                    if (unitChild.getNodeName().equals("upgrade")) {
                        // Card card = (Card)baseCard.clone();
                        ArrayList<SkillSpec> skillSpecsUpgraded = new ArrayList<>(); 
                        Card card = baseCard.clone();
                        NodeList upgradeChilds = unitChild.getChildNodes();
                        String id = "";
                        card.setSkills(new ArrayList<SkillSpec>());
                        for (int l = 0; l < upgradeChilds.getLength(); l++) {
                            Node upgradeChild = upgradeChilds.item(l);
                            if (upgradeChild.getNodeName().equals("level")) {
                                String level = upgradeChild.getFirstChild().getNodeValue();
                                if (level != null) {
                                    card.setLevel(Integer.parseInt(level));
                                }
                            }
                            if (upgradeChild.getNodeName().equals("card_id")) {
                                id = upgradeChild.getFirstChild().getNodeValue();
                                idInt = Integer.parseInt(id);
                                card.setId(idInt);
                            }
                            if (unitChild.getNodeName().equals("name")) {
                                if (unitChild.getFirstChild().getNodeValue() != null) {
                                    name = unitChild.getFirstChild().getNodeValue();
                                    card.setName(name);
                                }
                            }
                            updateSameCardAttributes(unitChild, card, skillSpecsUpgraded);
                        }
                        if (!skillSpecsUpgraded.isEmpty()) {
                            card.setSkills(skillSpecsUpgraded);
                            updateSkills(card, skillSpecsUpgraded);
                            skillSpecs = skillSpecsUpgraded;
                        }
                        if (id != null) {
                            upgrades.put(card.getLevel(), card);
                            card.setId(idInt);
                            cards.put(idInt, card);
                        }

                    }
                }
            }
            updateSkills(baseCard, skillSpecs);
            recognizeCardType(baseCard);
            Card topLevelCard = baseCard;
            for (Card card : upgrades.values()) {
                if (card.getLevel() > topLevelCard.getLevel()) {
                    topLevelCard = card;
                    card.setType(baseCard.getType());
                }
            }
            baseCard.setTopLevelCard(topLevelCard);
            for (Card card : upgrades.values()) {
                if (card.getLevel() == baseCard.getLevel() + 1) {
                    baseCard.addUsedForCard(card, 1);
                } else if (card.getLevel() != baseCard.getLevel()) {
                    upgrades.get(card.getLevel() - 1).addUsedForCard(card, 1);
                }
                card.setTopLevelCard(topLevelCard);
            }
        }
        return cards;
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

    private static void recognizeCardType(Card baseCard) {

        int id = baseCard.getBaseId();

        switch (id / 1000) {
        case 0:
            baseCard.setType(CardType.ASSAULT);
            break;
        case 1:
            baseCard.setType(CardType.COMMANDER);
            break;
        case 2:
            baseCard.setType(CardType.STRUCTURE);
            // fortress
            // if (id >= 2700 && id < 2997) {
            // //unsigned fort_type_value = atoi(fortress_type_node->value());
            // switch (baseCard.get) {
            // case 1:
            // baseCard.setCategory(CardCategory.FORTRESS_DEFENSE);
            // break;
            // case 2:
            // baseCard.setCategory(CardCategory.FORTRESS_SIEGE);
            // break;
            // default:
            // System.err.println("WARNING: parsing card [" + id + "]:
            // unsupported fortress_type=");
            // }
            // else if (id < 2748 || id >= 2754) // except Sky Fortress
            // {
            // System.err.println("WARNING: parsing card [" + id + "]: expected
            // fortress_type=" );
            // }
            // }
            break;
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
            baseCard.setType(CardType.ASSAULT);
            break;
        case 8:
        case 9:
            baseCard.setType(CardType.STRUCTURE);
            break;

        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
        case 15:
        case 16:
            baseCard.setType(CardType.ASSAULT);
            break;
        case 17:
        case 18:
        case 19:
        case 20:
        case 21:
        case 22:
        case 23:
        case 24:
            baseCard.setType(CardType.STRUCTURE);
            break;
        case 25:
        case 26:
        case 27:
        case 28:
        case 29:            
            baseCard.setType(CardType.COMMANDER);
            break;
        case 30:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
        case 38:
        case 39:
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
        case 48:
        case 49:
        case 50:
            baseCard.setType(CardType.ASSAULT);
            if (id == 43451 || id == 43452) {
                baseCard.setCategory(CardCategory.DOMINION_MATERIAL);
            }
            break;
        case 51:
        case 52:
        case 53:
        case 54:
            // Nexus start at 50238
//          // [50001 .. 55000]
//          else if (card->m_id < 55001)
            baseCard.setType(CardType.STRUCTURE);
            baseCard.setCategory(CardCategory.DOMINION_ALPHA);
            break;
        default:
            // [55001 .. ...]
            baseCard.setType(CardType.ASSAULT);
            break;
        }
        
        // fortresses
        if (baseCard.getSet() == 8000) {
            if (baseCard.getType() != CardType.STRUCTURE) {
                System.err.println("WARNING: parsing card [" + id + "]: set 8000 supposes fortresses card that implies type Structure, but card has type " + baseCard.getType().name());
            }
            // assume all other fortresses as conquest towers
            if (baseCard.getCategory() == CardCategory.NORMAL) {
                baseCard.setCategory(CardCategory.FORTRESS_CONQUEST);
            }
        }

    }

    private static void updateSameCardAttributes(Node unitChild, Card card, List<SkillSpec> skillSpecs) {
        if (unitChild.getNodeName().equals("cost") && unitChild.getFirstChild() != null) {
            String cost = unitChild.getFirstChild().getNodeValue();
            card.setRecipeCost(Integer.parseInt(cost));
        }

        if (unitChild.getNodeName().equals("attack") && unitChild.getFirstChild() != null) {
            if (unitChild.getFirstChild() != null) {
                String attack = unitChild.getFirstChild().getNodeValue();
                card.setAttack(Integer.parseInt(attack));
            }
        }
        if (unitChild.getNodeName().equals("health") && unitChild.getFirstChild() != null) {
            String health = unitChild.getFirstChild().getNodeValue();
            card.setHealth(Integer.parseInt(health));
        }
        if (unitChild.getNodeName().equals("fusion_level") && unitChild.getFirstChild() != null) {
            String level = unitChild.getFirstChild().getNodeValue();
            card.setFusionLevel(Integer.parseInt(level));
        }
        if (unitChild.getNodeName().equals("skill")) {
            NamedNodeMap skill = unitChild.getAttributes();
            String id = skill.getNamedItem("id") == null ? "" : skill.getNamedItem("id").getNodeValue();
            String x = skill.getNamedItem("x") == null ? "" : skill.getNamedItem("x").getNodeValue();
            String y = skill.getNamedItem("y") == null ? "" : skill.getNamedItem("y").getNodeValue();
            String all = skill.getNamedItem("all") == null ? "" : skill.getNamedItem("all").getNodeValue();
            String c = skill.getNamedItem("c") == null ? "" : skill.getNamedItem("c").getNodeValue();
            String card_id = skill.getNamedItem("card_id") == null ? "" : skill.getNamedItem("card_id").getNodeValue();
            String trigger = skill.getNamedItem("trigger") == null ? "" : skill.getNamedItem("trigger").getNodeValue();
            String n = skill.getNamedItem("n") == null ? "" : skill.getNamedItem("n").getNodeValue();
            String s = skill.getNamedItem("s") == null ? "" : skill.getNamedItem("s").getNodeValue();
            String s2 = skill.getNamedItem("s2") == null ? "" : skill.getNamedItem("s2").getNodeValue();

            
            SkillSpec new_skill = new SkillSpec();
            new_skill.setId(Skill.valueOf(id.toUpperCase()));
            setSkillSpec(card, x, y, all, c, card_id, trigger, new_skill, n, s, s2);
            skillSpecs.add(new_skill);
        }
    }

    private static void setSkillSpec(Card card, String x, String y, String all, String c, String card_id, String trigger, SkillSpec new_skill, String n, String s, String s2) {
        new_skill.setAll(all != null && all.equals("1"));
        if (!x.isEmpty()) {
            new_skill.setX(Integer.parseInt(x));
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

    public static void load_skills_set_xml(Cards all_cards, String string, boolean b) {
        // TODO Auto-generated method stub
        
    }
}