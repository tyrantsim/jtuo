package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.cards.CardType;
import com.github.tyrantsim.jtuo.skills.SkillSpec;

import java.util.List;

public class PlayCard {

    private Card card;
    private Field field;
    private CardStatus status;
    private List<CardStatus> storage;
    private int actorIndex;
    private CardStatus actorStatus;

    PlayCard(Card card, Field field, int actorIndex, CardStatus actorStatus) {
        this.card = card;
        this.field = field;
        this.status = null;
        this.storage = null;
        this.actorIndex = actorIndex;
        this.actorStatus = actorStatus;
    }

    CardStatus op() {
        setStorage(card.getType());
        placeCard();

        if (status.getDelay() == 0)
            FieldSimulator.checkAndPerformValor(field, status);

        // resolve On-Play skills
        for (SkillSpec skillSpec : card.getSkillsOnPlay())
            field.addSkillToQueue(status, skillSpec);

        return status;
    }

    private void setStorage(CardType cardType) {
        switch (cardType) {
            case ASSAULT:
                storage = field.getPlayer(actorIndex).getAssaults();
                break;
            case STRUCTURE:
                storage = field.getPlayer(actorIndex).getStructures();
                break;
            default:
                throw new RuntimeException("Unexpected card type: " + cardType);
        }
    }

    private void placeCard() {
        status = new CardStatus();
        storage.add(status);
        status.set(card);
        status.setIndex(storage.size() - 1);
        status.setPlayer(actorIndex);
    }

}
