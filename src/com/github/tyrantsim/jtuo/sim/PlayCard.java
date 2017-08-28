package com.github.tyrantsim.jtuo.sim;

import com.github.tyrantsim.jtuo.cards.Card;
import com.github.tyrantsim.jtuo.optimizer.TyrantOptimize;
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
        setStorage();
        placeCard();

        // resolve On-Play skills
        for (SkillSpec skillSpec : card.getSkillsOnPlay()) {
            field.addSkillToQueue(status, skillSpec);
        }

        return status;
    }

    void setStorage() {
    }

    void placeCard() {
        status = new CardStatus();
        storage.add(status);
        status.setIndex(storage.size() - 1);
        status.setPlayer(actorIndex);

        if (status.getDelay() == 0) {
            FieldSimulator.checkAndPerformValor(field, status);
        }
    }

}
