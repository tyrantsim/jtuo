package com.github.tyrantsim.jtuo.cards;

public class CardSpec {

    private int cardId;
    private int cardNum;
    private char numSign;
    private boolean marked;

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getCardNum() {
        return cardNum;
    }

    public void setCardNum(int cardNum) {
        this.cardNum = cardNum;
    }

    public char getNumSign() {
        return numSign;
    }

    public void setNumSign(char numSign) {
        this.numSign = numSign;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }
}
