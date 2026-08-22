package models.factory.plantSkills;

import models.entity.Plant;
import models.entity.PlantTags;
import models.factory.PlantFactory;
import models.factory.plantSkills.skillDatas.PlantArmor;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.Random;

public class ExtraHP implements Skill{
    public enum Type{CLONE , ARMOR , HEAL , LIFE_RESET}
    Type type;
    public float hp;
    public boolean explosive;
    public ExtraHP(Type type, float hp){
        this.type = type;
        this.hp = hp;
    }

    public ExtraHP(Type t){
        type = t;
    }
    @Override
    public void do_skill(Plant plant, BaseGame game) {
        System.out.println("giving hp ... ");
            switch (type){
                case ARMOR -> armor(plant, game);
                case CLONE -> clone(plant, game);
                case HEAL -> heal(plant, game);
                case LIFE_RESET -> reset(plant, game);
            }
            if(plant.getTags().contains(PlantTags.STACK)) stack(plant, game);
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

    public int cloneNumber;
    Random rand = new Random();
    private void clone(Plant plant, BaseGame game) {
        if(cloneNumber == 0) return;
        int row = rand.nextInt(5);
        int col = rand.nextInt(9);
        Tile tile = game.getField().getTiles().get(row).get(col);
        if(tile.isEmpty() && tile.isPlantable()){
            PlantFactory factory = new PlantFactory();
            Plant clone = factory.createPlant(plant.getType());
            clone.setLine(row);
            clone.setTileIndex(col);
            game.getPlantsInField().add(clone);
            tile.setEmpty(false);
            cloneNumber -= 1;
        }
        clone(plant, game);
    }



    private void armor(Plant plant, BaseGame game) {
        plant.getArmor().add(new PlantArmor(hp));
    }
    private void stack(Plant plant , BaseGame game) {
        int row = plant.getLine();
        int col = plant.getTileIndex();
        for (Plant p :  game.getPlantsInField()) {
            if (p.getTileIndex() == row
                    && p.getTileIndex() == col
                    && !p.getArmor().get(p.getArmor().size() - 1).pumpkin) {
                PlantArmor pumpkinArmor = new PlantArmor(plant.getHp());
                pumpkinArmor.pumpkin = true;
                p.getArmor().add(pumpkinArmor);
            }
        }
    }

    private void heal(Plant plant, BaseGame game) {

    }

    private void reset(Plant plant, BaseGame game) {
            for (Plant x :  game.getPlantsInField()) {
                if(x.getType() == plant.getType()) x.setLifeTime(60);
            }
    }
}
