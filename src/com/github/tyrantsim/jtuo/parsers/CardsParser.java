package com.github.tyrantsim.jtuo.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;
import com.github.tyrantsim.jtuo.skills.SkillTrigger;
/**
 * @author Brikikeks
 */
public class CardsParser {

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
        CardsParser.initialize();
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

    private static Map<Integer, Card> readCards(Document document) {
        TreeMap<Integer, Card> cards = new TreeMap<>();
        NodeList units = document.getElementsByTagName("unit");
        
        for (int i = 0; i < units.getLength(); i++) {
            Element unit = (Element) units.item(i);
            
            String name = unit.getElementsByTagName("name").item(0).getTextContent();
            Integer baseIdInt = Integer.parseInt(unit.getElementsByTagName("id").item(0).getTextContent());
            Card baseCard = new Card();
            baseCard.setBaseId(baseIdInt);
            baseCard.setId(baseIdInt);
            baseCard.setName(name);

            cards.put(baseIdInt, baseCard);

            baseCard.setRarity(Integer.parseInt(unit.getElementsByTagName("rarity").item(0).getTextContent()));
            baseCard.setFaction(Faction.values()[Integer.parseInt(unit.getElementsByTagName("type").item(0).getTextContent())]);

            NodeList unitChilds = unit.getChildNodes();
            for (int j = 0; j < unitChilds.getLength(); j++) {
                Node unitChild = unitChilds.item(j);
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
                updateSameCardAttributes(unitChild, baseCard, false);
            }
            
            recognizeCardType(baseCard);
            Card topLevelCard = baseCard.clone();
            
            //System.out.println(baseCard.toString());
            
            Map<Integer, Card> upgrades = new TreeMap<>();

            NodeList upgradeChilds = unit.getElementsByTagName("upgrade");
            for (int l = 0; l < upgradeChilds.getLength(); l++) {
                Card card = topLevelCard.clone();
                card.setTopLevelCard(topLevelCard);
                topLevelCard.addUsedForCard(card, 1);
                
                Element upgradeChild = (Element) upgradeChilds.item(l);
                Integer idInt = Integer.parseInt(upgradeChild.getElementsByTagName("card_id").item(0).getTextContent());
                card.setId(idInt);
                card.setLevel(Integer.parseInt(upgradeChild.getElementsByTagName("level").item(0).getTextContent()));

                upgrades.put(card.getLevel(), card);
                cards.put(idInt, card);
                NodeList nameList = upgradeChild.getElementsByTagName("name");
                if (nameList.getLength() > 0) {
                    card.setName(nameList.item(0).getTextContent());
                }
                for (int j = 0; j < upgradeChild.getChildNodes().getLength(); j++) {
                    updateSameCardAttributes(upgradeChild.getChildNodes().item(j), card, true);
                }
                topLevelCard = card;
                //System.out.println(card.toString());
            }

            baseCard.setTopLevelCard(topLevelCard);
        }
        return cards;
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
            if (id == 43451 || id == 43452) {
                baseCard.setType(CardType.ASSAULT);
                baseCard.setCategory(CardCategory.DOMINION_MATERIAL);
                break;
            }
        case 44:
        case 45:
        case 46:
        case 47:
        case 48:
        case 49:
            baseCard.setType(CardType.ASSAULT);
            break;
        case 50:
        case 51:
        case 52:
        case 53:
        case 54:
            // Nexus start at 50238
            // // [50001 .. 55000]
            // else if (card->m_id < 55001)
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

    private static void updateSameCardAttributes(Node unitChild, Card card, boolean upgrade) {
        if (unitChild.getNodeName().equals("cost") && unitChild.getFirstChild() != null) {
            String cost = unitChild.getFirstChild().getNodeValue();
            card.setDelay(Integer.parseInt(cost));
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
            try {
                Skill id = skill.getNamedItem("id") == null ? Skill.NO_SKILL
                        : Skill.valueOf(skill.getNamedItem("id").getNodeValue().toUpperCase());
                int x = skill.getNamedItem("x") == null ? 0
                        : Integer.parseInt(skill.getNamedItem("x").getNodeValue());
                Faction y = skill.getNamedItem("y") == null ? Faction.ALL_FACTIONS
                        : Faction.values()[Integer.parseInt(skill.getNamedItem("y").getNodeValue())];
                boolean all = skill.getNamedItem("all") != null
                        && skill.getNamedItem("all").getNodeValue().equals("1");
                int c = skill.getNamedItem("c") == null ? 0
                        : Integer.parseInt(skill.getNamedItem("c").getNodeValue());
                int card_id = skill.getNamedItem("card_id") == null ? 0
                        : Integer.parseInt(skill.getNamedItem("card_id").getNodeValue());
                SkillTrigger trigger = skill.getNamedItem("trigger") == null ? SkillTrigger.ACTIVATE
                        : SkillTrigger.valueOf(skill.getNamedItem("trigger").getNodeValue().toUpperCase());
                int n = skill.getNamedItem("n") == null ? 0
                        : Integer.parseInt(skill.getNamedItem("n").getNodeValue());
                Skill s = skill.getNamedItem("s") == null ? Skill.NO_SKILL
                        : Skill.valueOf(skill.getNamedItem("s").getNodeValue().toUpperCase());
                Skill s2 = skill.getNamedItem("s2") == null ? Skill.NO_SKILL
                        : Skill.valueOf(skill.getNamedItem("s2").getNodeValue().toUpperCase());

                card.addSkill(trigger, id, x, y, n, c, s, s2, all, card_id);

            } catch (IllegalArgumentException e) {
                System.err.println("Error: failed to parse skills for " + card.getName());
            }
        }
    }
}