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
import java.util.Random;

public class ConveyorBelt extends NormalGame implements SpecialGame {
    ArrayList<PlantType> belt =  new ArrayList<>();
    ArrayList<PlantType> plants =  new ArrayList<>();

    public  ConveyorBelt(Chapters chapter, Level level) {
        super(chapter, level);
        initPlants(5);
        belt.add(plants.getFirst());
        state = GameState.PLAYING;
    }

    Random random = new Random();



    private void initPlants(int i){
        if(i == 0) return;
        int index = random.nextInt(App.getCurrentuser().getUnlockedPlants().size());
        PlantType type = App.getCurrentuser().getUnlockedPlants().get(index);
        if(plants.contains(type)){
            initPlants(i);
        }
        else {
            plants.add(type);
            initPlants(i - 1);
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
        if(counter == 0) return null;
        int random = rand.nextInt(App.getCurrentuser().getUnlockedPlants().size());
        PlantType toAdd = App.getCurrentuser().getUnlockedPlants().get(random);
       if(!plants.contains(toAdd)){
           plants.add(toAdd);
           counter--;
       }
       filterPlants();
        return null;
    }

    @Override
    public void attack() {

    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        try {
            PlantType type = PlantType.valueOf(plantName.toUpperCase());
            if(belt.contains(type)){
                belt.remove(type);
                PlantFactory factory = new PlantFactory();
                Plant plant = factory.createPlant(type);
                plant.setLine(y);
                plant.setTileIndex(x);
                Tile tile = this.field.getTileByCoordinats(x,y);
                tile.setEmpty(true);
                this.plantsInField.add(plant);
            return true;
            }
            else return false;
        }catch (Exception e){
            return false;
        }
    }

    float beltTimer = 0;
    private void updateBelt(float delta){
        if(beltTimer <= 0){
            beltTimer = 12;
            int index = rand.nextInt(plants.size());
            belt.add(plants.get(index));
        }
        else beltTimer -= delta;
    }


    public ArrayList<PlantType> getBelt() {
        return belt;
    }
}
