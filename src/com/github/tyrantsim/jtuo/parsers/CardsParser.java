package com.github.tyrantsim.jtuo.parsers;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.skills.Skill;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

/**
 * @author Brikikeks
 */
public class CardsParser {

    public static Hashtable<Integer, Card> cards = new Hashtable<>();

    public static boolean initialized = false;

    static {

        // cards.putAll(readCards("cards_section_9.xml"));
    }

    public static void initialize() {
        if (!initialized) {
            for (int i = 1; i < 100; i++) {
                try {
                    readCards("cards_section_" + i + ".xml");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        initialized = true;
    }

    /**
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        CardsParser.initialize();
    }


    public static void readCards(String file) throws FileNotFoundException {

        File card1 = new File(new File(".", "data"), file);

        if (!card1.getParentFile().exists()) {
            if (!card1.getParentFile().mkdirs()) {
                System.err.println("Failed to create data folder");
            }
        }

        System.out.println(file);
        
        if (!card1.exists() || ((new Date().getTime() - card1.lastModified()) > 86400000)) {
            try {
                // http://mobile.tyrantonline.com/assets/cards_section_1.xml
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("http://mobile.tyrantonline.com/assets/" + file);
                cards.putAll(readCards(document));

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(document);
                StreamResult console = new StreamResult(card1);
                transformer.transform(source, console);
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FileNotFoundException fnfe) {
                // TODO: handle exception
                throw fnfe;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TransformerConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TransformerFactoryConfigurationError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (TransformerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                cards.putAll(readCards(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(card1)));
            } catch (SAXException | ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Hashtable<Integer, Card> readCards(Document parse) {
        Hashtable<Integer, Card> cards = new Hashtable<>();

        NodeList units = parse.getElementsByTagName("unit");
        for (int i = 0; i < units.getLength(); i++) {
            Node unit = units.item(i);
            NodeList unitChilds = unit.getChildNodes();
            String name = "";
            String baseId = "";
            Integer baseIdInt = null;
            Integer idInt = null;
            Card baseCard = null;
            for (int j = 0; j < unitChilds.getLength(); j++) {
                Node unitChild = unitChilds.item(j);
                System.out.println(unitChild.getNodeName());
                System.out.println(unitChild.getNodeValue());
                if (unitChild.getNodeName().equals("id")) {
                    if (unitChild.getFirstChild().getNodeValue() != null) {
                        baseId = unitChild.getFirstChild().getNodeValue();
                        baseIdInt = Integer.parseInt(baseId);
                    }
                    baseCard = new Card(baseIdInt, name);
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

                if (baseCard != null) {
                    updateSameCardAttributes(unitChild, baseCard);
                }
                // attack
                // health
                // cost
                // rarity
                // skill
                //
                if (baseIdInt != null && name != null && !name.isEmpty()) {
                    // card = new Card(baseIdInt, name);

                    cards.put(baseIdInt, baseCard);

                    // System.out.println("" + name);
                    if (unitChild.getNodeName().equals("upgrade")) {
                        // Card card = (Card)baseCard.clone();
                        Card card = baseCard.clone();
                        NodeList upgradeChilds = unitChild.getChildNodes();
                        String level_prefix = "";
                        String id = "";
                        for (int l = 0; l < upgradeChilds.getLength(); l++) {
                            Node upgradeChild = upgradeChilds.item(l);

                            if (upgradeChild.getNodeName().equals("level")) {
                                String level = upgradeChild.getFirstChild().getNodeValue();
                                if (level != null && Integer.valueOf(level) < 6) {
                                    System.out.println(level + ":" + name);
                                    level_prefix = "-" + level;
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

                            updateSameCardAttributes(unitChild, card);

                        }

                        if (id != null) {
                            System.out.println(id + ":" + name + level_prefix);
                            baseCard.addUsedForCard(card, 1);
                            if(baseCard.getTopLevelCard().getLevel() < card.getLevel()) {
                                baseCard.setTopLevelCard(card);
                            }

                            card.setId(idInt);
                            cards.put(idInt, card); // name + level_prefix
                        }

                    }
                }
            }
        }

        return cards;
    }

    private static void updateSameCardAttributes(Node unitChild, Card card) {
        if (unitChild.getNodeName().equals("attack") && unitChild.getFirstChild() != null) {
            if (unitChild.getFirstChild() != null) {
                System.out.println(card.getName());
                String attack = unitChild.getFirstChild().getNodeValue();
                card.setAttack(Integer.parseInt(attack));
            }
        }
        if (unitChild.getNodeName().equals("health") && unitChild.getFirstChild() != null) {
            String health = unitChild.getFirstChild().getNodeValue();
            card.setHealth(Integer.parseInt(health));
        }
        if (unitChild.getNodeName().equals("cost") && unitChild.getFirstChild() != null) {
            String cost = unitChild.getFirstChild().getNodeValue();
            card.setRecipeCost(Integer.parseInt(cost));
        }
        if (unitChild.getNodeName().equals("fusion_level") && unitChild.getFirstChild() != null) {
            String level = unitChild.getFirstChild().getNodeValue();
            card.setFusionLevel(Integer.parseInt(level));
        }
        if (unitChild.getNodeName().equals("skill")) {
            NamedNodeMap skill = unitChild.getAttributes();
            String id = skill.getNamedItem("id") == null ? "" : skill.getNamedItem("id").getNodeValue();
            String x = skill.getNamedItem("x") == null ? "" : skill.getNamedItem("x").getNodeValue();
            String all = skill.getNamedItem("all") == null ? "" : skill.getNamedItem("all").getNodeValue();
            // Integer.parseInt(level)
            ArrayList<SkillSpec> skillSpecs = card.getSkills();
            boolean skill_found = false;
            for (SkillSpec skillSpec : skillSpecs) {
                if (skillSpec.getId().equals(id)) {
                    skillSpec.setAll(all.equals("1"));
                    if (!x.isEmpty()) {
                        skillSpec.setX(Float.parseFloat(x));
                    }
                    // skillSpec.setAll(all);
                    skill_found = true;
                }
            }
            if (!skill_found) {
                SkillSpec new_skill = new SkillSpec();
                new_skill.setAll(all.equals("1"));
                System.out.println(id.toUpperCase());
                new_skill.setId(Skill.valueOf(id.toUpperCase()));
                if (!x.isEmpty()) {
                    new_skill.setX(Float.parseFloat(x));
                }
                skillSpecs.add(new_skill);
            }

            // <skill id="allegiance" x="2"/>
            // <skill all="1" id="enfeeble" x="4"/>
            // <skill id="legion" x="4"/>
        }
    }
}