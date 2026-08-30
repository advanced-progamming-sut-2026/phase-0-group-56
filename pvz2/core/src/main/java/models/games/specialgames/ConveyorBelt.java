package models.games.specialgames;

import models.App;
import models.entity.Plant;
import models.factory.PlantFactory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.gamepanes.Tile;
import models.games.NormalGame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ConveyorBelt extends NormalGame implements SpecialGame {
    ArrayList<PlantType> belt =  new ArrayList<>();
    ArrayList<PlantType> plants =  new ArrayList<>();

    public  ConveyorBelt(Chapters chapter, Level level) {
        super(chapter, level);
        initPlants(5);
        if (!plants.isEmpty()) {
            belt.add(plants.getFirst());
        }
        state = GameState.PLAYING;
    }

    Random random = new Random();



    private void initPlants(int count){
        if (App.getCurrentuser() == null || App.getCurrentuser().getUnlockedPlants().isEmpty()) {
            return;
        }
        ArrayList<PlantType> unlocked = new ArrayList<>(App.getCurrentuser().getUnlockedPlants());
        Collections.shuffle(unlocked, random);
        for (PlantType type : unlocked) {
            if (type != null && !plants.contains(type)) {
                plants.add(type);
            }
            if (plants.size() >= count) {
                break;
            }
        }
    }
    @Override
    public String playGame(float delta) {
        updateBelt(delta);
        return super.playGame(delta);
    }

    int counter = 8;
    Random rand = new Random();
    @Override
    public ArrayList<PlantType> filterPlants() {
        return new ArrayList<>(plants);
    }

    @Override
    public void attack() {

    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        try {
            PlantType type = PlantType.valueOf(plantName.toUpperCase());
            if (field == null || x < 0 || x >= 9 || y < 0 || y >= 5 || !belt.contains(type)) {
                return false;
            }
            Tile tile = field.getTileByCoordinats(x, y);
            PlantFactory factory = new PlantFactory();
            Plant plant = factory.createPlant(type);
            if (plant == null || !tile.isEmpty() || !tile.isPlantable()) {
                return false;
            }
            if (belt.remove(type)) {
                plant.setLine(y);
                plant.setTileIndex(x);
                tile.setEmpty(false);
                this.plantsInField.add(plant);
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    float beltTimer = 0;
    private void updateBelt(float delta){
        if (plants.isEmpty() || belt.size() >= 5) {
            return;
        }
        if(beltTimer <= 0){
            beltTimer = 12;
            ArrayList<PlantType> candidates = new ArrayList<>(plants);
            candidates.removeAll(belt);
            if (!candidates.isEmpty()) {
                belt.add(candidates.get(rand.nextInt(candidates.size())));
            }
        }
        else beltTimer -= delta;
    }


    public ArrayList<PlantType> getBelt() {
        return belt;
    }
}
