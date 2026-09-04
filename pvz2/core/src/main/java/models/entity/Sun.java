package models.entity;

import models.Constants;
import models.gamepanes.Tile;
import models.games.BaseGame;

public class Sun extends Entity{
    public enum AnimationType {
        NORMAL,
        SPECIAL,
        RADIOACTIVE,
        /** Radioactive sun after it has landed on the lawn. */
        RADIOACTIVE_GROUNDED
    }

    Plant producer;
    float velocity = 70f;
    static float width = 50;
    static float height = 50;
    private int price;
    private boolean radioActive = false;
    /* Kept separately so the visual type remains radioactive even if callers
       change the gameplay flag while the collectible is alive. */
    private boolean radioactiveVisual;
    private AnimationType animationType = AnimationType.NORMAL;
    boolean ground = false;
    private float remainingTime;
    public Sun(){}
    public Sun(int price, float remainingTime, float x, float y) {
        this.price = price;
        this.remainingTime = remainingTime;
        this.x = x;
        this.y = y;
    }


    public Sun(int price, int remainingTime){}



    public String land(float delta ,  BaseGame game){
        if(!ground)
        {
            this.y -= delta * Constants.SUN_DROPPING_VELOCITY;
            if(this.y + Sun.height / 2 <= this.line * Tile.getHeight()
                + Tile.getHeight() / 2 ){
                ground = true;
                // The plant already starts its next cooldown when it creates
                // this sun. Do not reset it every frame while the sun waits;
                // doing so prevented sun producers from ever producing again.
                return "Sun landed at " + this.tileIndex +
                    " , " + this.line;
            }
            return null;
        }
        if(ground){
            remainingTime  -= delta;
        }
        return "sun is waiting in (" + this.tileIndex + " , " + this.line + ")" +
            "\n time remaining: " + remainingTime ;
    }

    public void dispose(BaseGame game){

        if(radioActive) {
            float centreX = this.x + Sun.width / 2;
            float centreY = this.y + Sun.height / 2;
            for (Zombie zombie : game.getZombies()) {
                float zCentreX = zombie.getX() + zombie.getWidth() / 2;
                float zCentreY = zombie.getY() + zombie.getHeight() / 2;
                if (Math.abs(centreX - zCentreX) <= Tile.getWidth() * 2 &&
                    Math.abs(centreY - zCentreY) <= Tile.getHeight() * 2) {
                    zombie.setHp(zombie.getHp() - 150);
                }
            }
            for (Plant x : game.getPlantsInField()) {
                float pCentreX = x.getX() + x.getWidth() / 2;
                float pCentreY = x.getY() + x.getHeight() / 2;
                if (Math.abs(centreX - pCentreX) <= Tile.getWidth() &&
                    Math.abs(centreY - pCentreY) <= Tile.getHeight()) {
                    x.setHp(x.getHp() - 80);
                }
            }
        }



    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
        if (!radioactiveVisual && price >= 100) {
            animationType = AnimationType.SPECIAL;
        }
    }

    public boolean isRadioActive() {
        return radioActive;
    }

    public void setRadioActive(boolean radioActive) {
        this.radioActive = radioActive;
        if (radioActive) {
            radioactiveVisual = true;
            animationType = AnimationType.RADIOACTIVE;
        }
    }

    public AnimationType getAnimationType() {
        if (radioActive || radioactiveVisual) {
            return ground ? AnimationType.RADIOACTIVE_GROUNDED : AnimationType.RADIOACTIVE;
        }
        if (animationType == AnimationType.SPECIAL || price >= 100) {
            return AnimationType.SPECIAL;
        }
        return AnimationType.NORMAL;
    }

    public void setAnimationType(AnimationType animationType) {
        if (animationType == null) {
            this.animationType = AnimationType.NORMAL;
            return;
        }
        this.animationType = animationType;
        if (animationType == AnimationType.RADIOACTIVE
            || animationType == AnimationType.RADIOACTIVE_GROUNDED) {
            radioactiveVisual = true;
        }
    }

    public float getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(float remainingTime) {
        this.remainingTime = remainingTime;
    }



    public void setLine(int line) {
        this.line = line;
        this.y = line * Tile.getHeight();
    }

    public Plant getProducer() {
        return producer;
    }

    public void setProducer(Plant producer) {
        this.producer = producer;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }



    public boolean isGround() {
        return ground;
    }

    public void setGround(boolean ground) {
        this.ground = ground;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public void setTileIndex(int tileIndex) {
        this.tileIndex = tileIndex;
        this.x =  tileIndex * Tile.getWidth();
    }
}
