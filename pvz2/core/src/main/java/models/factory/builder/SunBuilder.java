package models.factory.builder;

import models.entity.Sun;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.utils.Result;

import java.util.Random;

public class SunBuilder {
    private float timePassed = 0f;
    private float cooldownTillNextSun;

    public Result sunLight(float delta , BaseGame game) {
        if(!game.isDay()){
            return new Result(true , "In the Night , There's no sun" , null);
        }
        this.timePassed += delta;
        if(cooldownTillNextSun <= 0){
            cooldownTillNextSun = Math.min(6 + 0.05f * timePassed , 12);
            Sun product = drop();
            game.getSuns().add(product);
            String type = product.getPrice() == 100 ? "Special" : product.getPrice() == 50 ?
                    "Big" : product.isRadioActive() ? "Radio Active" : "Normal";
            return new Result(true , "Sun " + type + " is dropping at ("
                    + product.getTileIndex() + " , " + product.getLine() + ")" , null);
        }else{
            cooldownTillNextSun -= delta;
        }
        // The HUD already shows the current sun count; exposing the internal
        // spawn timer every frame only replaces useful gameplay messages.
        return new Result(false, "", null);
    }

    private Sun drop() {
        Random  rand = new Random();
        Sun sun = new Sun();
        sun.setRemainingTime(10);
        int i = rand.nextInt(20) + 1;
        int line = rand.nextInt(5);
        int tileIndex = rand.nextInt(9);
        sun.setLine(line);
        sun.setTileIndex(tileIndex);
        sun.setY(10 * Tile.getHeight());
        if(i == 9){
            sun.setRadioActive(true);
            sun.setPrice(25);
        }
        else if(i >= 10 && i <= 12) {
            sun.setPrice(100);
        }
        else{
            sun.setPrice(25);
        }
        return sun;
    }
}
