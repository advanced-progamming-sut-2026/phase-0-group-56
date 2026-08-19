package models.games.specialgames;

import controllers.Start.PlantSelection;
import models.App;
import models.Constants;
import models.entity.Plant;
import models.entity.PlantCategory;
import models.factory.builder.PlantBuilder;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.gamepanes.Tile;
import models.games.NormalGame;

import java.util.ArrayList;

public class PlantWhatYouGet extends NormalGame implements SpecialGame {


    public PlantWhatYouGet(Chapters chapter, Level level) {
        super(chapter, level);
        selection = new PlantSelection(filterPlants());
        sunCount = Constants.PLANT_WHAT_YOU_GET_STARTING_SUN_COUNT;
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        ArrayList<PlantType> plantTypes = new ArrayList<>();
        for (PlantType plant : App.getCurrentuser().getUnlockedPlants()) {
            if(plant.getCategory() != PlantCategory.SunProducer) plantTypes.add(plant);
        }
        return plantTypes;
    }

    @Override
    public void attack() {

    }

    boolean selectionFinished;
    boolean plantFinished;
    @Override
    public boolean startGame(String input) {
        return selectionFinished;
    }

    @Override
    public String playGame(float delta) {
        if(selectionFinished &&  plantFinished){
             return super.playGame(delta);
        }
        return "No you cannot play game now , you gotta ask the cute zombies to come \n, they're a " +
                "little bit shy :>";
    }

    @Override
    public String plant(String plantName, int x, int y) {
        if(selectionFinished) return "Yo What? Wanna plant? Lol , you idiot , you're fucked up.";
        try {
            PlantBuilder builder = new PlantBuilder();
            PlantType type = PlantType.valueOf(plantName);
            if(!availablePlants.containsKey(type)){
                return "Plant is not in the slots.";
            }
            float cost = availablePlants.get(type).getCost();
            if(sunCount < cost){
                return "Not enough suns to plant " + plantName;
            }
            sunCount -= (int) cost;
            Plant plant = builder.build(type);
            plant.setTileIndex(x);
            plant.setTileIndex(y);
            plantsInField.add(plant);
            Tile tile = field.getTileByCoordinats(x, y);
            tile.setEmpty(true);
            return "Plant " + type +" planted at (" + x + ", " + y + ")" + "and cost you " + cost + " suns.";
        }catch (IllegalArgumentException e){
            return ("Invalid PlantType");
        }
    }

    public void startWaves(){
        selectionFinished = true;
    }



}
