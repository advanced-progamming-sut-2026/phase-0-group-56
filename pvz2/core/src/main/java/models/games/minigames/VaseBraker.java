package models.games.minigames;

import controllers.datacontroller.SeedPackage;
import models.gameadventure.Chapters;
import models.entity.Plant;
import models.entity.Zombie;
import models.factory.builder.PlantType;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class VaseBraker extends BaseGame {
    ArrayList<Vase> vases = new ArrayList<>();
    ArrayList<PlantType> availablePlants;
    HashMap<PlantType , SeedPackage>  seedPackages = new HashMap<>();

    int plantVaseCount;
    public  VaseBraker(MinigameLevel level) {
        availablePlants = level.getPlants();
        field.initField(Chapters.AncientEgypt , level.getId());
        initVases(level.getId() , 5 *(4  + level.getId()));
        plantVaseCount = level.getId() + 2;
    }

    public String breakVase(int x , int y){
        Iterator<Vase> iterator = vases.iterator();
        while (iterator.hasNext()){
            Vase v =  iterator.next();
            if(v.line == y && v.tileIndex == x){
                switch (v.type) {
                    case PLANT -> v.breakPlantVase(this, seedPackages);
                    case ZOMBIE -> v.breakZombieVase(this, zombies);
                    case RANDOM -> v.breakVase(this);
                }
                String output = "Vase broken , a " + v.type+ "came out of It.";
                iterator.remove();
                Tile tile = field.getTiles().get(v.line).get(v.tileIndex);
                tile.setEmpty(true);
                return output;
            }
        }
        return "Sometimes in the life you're too competitive , as mush as it makes you blind!";
    }

    @Override
    public String playGame(float delta) {
        updatePlants(delta);
        updateZombies(delta);
        updateScene(delta);
        return "game goes on ... ";
    }

    @Override
    public Result check_endGame() {
        for (Zombie zombie : zombies){
            if(zombie.getX() <= 0) return new Result(true , "Loss" , null);
        }
        if(vases.isEmpty() && zombies.isEmpty()){
            return  new Result(true , "Won", null);
        }
        return new Result(false,null,null);
    }

    public ArrayList<PlantType> getAvailablePlants() {
        return availablePlants;
    }

    Random  rand = new Random();
    private void initVases(int level , int count){
        if(count == 0) return;
        int type = rand.nextInt(20);
        int line = rand.nextInt(5);
        int tileIndex = rand.nextInt(9 - 6 - level) + 6 - level ;
        Tile tile = field.getTiles().get(line).get(tileIndex);
        if((line +  tileIndex )% 3 == 0 && tile.isEmpty() && plantVaseCount > 0){
            vases.add(new Vase(line , tileIndex , Vase.Type.PLANT));
            tile.setEmpty(false);
            plantVaseCount--;
            initVases(level , count - 1);
        }
        else{
            if(type <= 5)vases.add(new Vase(line , tileIndex , Vase.Type.ZOMBIE));
            else vases.add(new Vase(line , tileIndex , Vase.Type.RANDOM));
            tile.setEmpty(false);
            initVases(level , count - 1);
        }

    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        try {
            PlantType type = PlantType.valueOf(plantName);
            if(!seedPackages.containsKey(type)){
                return false;
            }
            Tile tile = field.getTileByCoordinats(x, y);
            if(!tile.isEmpty() || !tile.isPlantable()) return false;
            Plant plant = plantFactory.createPlant(type);
            plant.setTileIndex(x);
            plant.setLine(y);
            tile.setEmpty(false);
            plantsInField.add(plant);
            return true;

        }catch (IllegalArgumentException e){
            return false;
        }
    }

    public ArrayList<Vase> getVases() {
        return vases;
    }
}
