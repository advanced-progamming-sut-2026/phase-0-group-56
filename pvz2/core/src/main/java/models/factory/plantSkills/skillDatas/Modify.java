package models.factory.plantSkills.skillDatas;

import models.App;
import models.entity.*;
import models.factory.builder.PlantType;
import models.factory.builder.PlantLevel;
import models.factory.plantSkills.Skill;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.games.BaseGame;

import java.util.ArrayList;

public class Modify implements Skill {
    public enum Type {GRAVE_EATER}
    Type type;
    public Modify() {
    }
    public Modify(Type type) {
        this.type = type;
    }
    @Override
    public void do_skill(Plant plant, BaseGame game) {
        System.out.println("due to " + plant.getType() + "'s performance ," + "we have the high ground , Anakin!");
        if(plant.getCategory() == PlantCategory.StrikeThrough) runBack(plant, game);
        else if(plant.getTags().contains(PlantTags.Fire)) fire(plant, game);
        else if(plant.getTags().contains(PlantTags.WATER)) lilyPad(plant, game);
        else if(plant.getType() == PlantType.GRAVE_BUSTER) graveEater(plant, game);
        else if(plant.getType() == PlantType.MAGNET_SHROOM) random(plant , game , 1);
    }

    @Override
    public void all(Plant plant, BaseGame game) {

    }

    @Override
    public void setRandom(boolean random) {

    }

    @Override
    public void setAll(boolean all) {

    }

    private void graveEater(Plant eater , BaseGame game) {
        Tile tile = game.getField().getTiles().get(eater.getLine()).get(eater.getTileIndex());
        TileType newType = switch (tile.getTileType()){
            case EGYPTIAN_GRAVE -> TileType.EGYPTIAN_TILE;
            case DARK_AGE_GRAVE -> TileType.DARK_AGE_TILE;
            case NECROMANCY -> TileType.DARK_AGE_TILE;
            default -> null;
        };
        tile.setTileType(newType);
        tile.setEmpty(true);
    }

    public boolean pf;
    private void fire(Plant plant, BaseGame game) {
        for (Projectile x : game.getBullets()) {
            if(x.getX() >= plant.getX() && x.getY() - plant.getY() <= 20){
                x.setDamage(x.getDamage() * (pf ? 3 : 2));
                x.getTags().add(Projectile.Tag.FIRE);
            }
        }
    }

    private void lilyPad(Plant plant, BaseGame game) {
        if(plant.getArmor() == null){
            for (Plant x : game.getPlantsInField()){
                if(x.getLine() == plant.getLine() && x.getTileIndex() == plant.getTileIndex()){
                    plant.getArmor().add(new PlantArmor(x.getHp()));
                    break;
                }
            }
        }
    }

    private void heat(Plant heater ,  BaseGame game) {
        Plant plant = game.findByCoordinates( heater.getTileIndex() ,heater.getLine());
        if(plant != null){
            plant.setFreezeLevel(0);
        }
        if (PlantLevel.current(heater.getType()) >= 3) {
            for (Plant p :  game.getPlantsInField()){
                if(Math.abs(p.getTileIndex() - heater.getTileIndex()) <= 1
                && Math.abs(p.getLine() - heater.getLine()) <= 1){
                    p.setFreezeLevel(0);
                }
            }
        }
    }

    public void runBack(Plant plant, BaseGame game) {
        for (Zombie x :  game.getZombies()) {
            if(x.getLine() == plant.getLine()) x.setTileIndex(plant.getTileIndex() - 3);
        }
    }

    @Override
    public ArrayList<Zombie> random(Plant plant, BaseGame game, int numbers) {
        Zombie zombie = Skill.super.random(plant, game, 1).get(0);
        Armor armor = null;
        for (Armor x : zombie.getArmors()){
            if(x.isMagnetic()){
                armor = x;
                break;
            }
        }
        if(armor == null){
            random(plant, game, 1);
        }
        else{
            zombie.getArmors().remove(armor);
        }
        return null;
    }
}
