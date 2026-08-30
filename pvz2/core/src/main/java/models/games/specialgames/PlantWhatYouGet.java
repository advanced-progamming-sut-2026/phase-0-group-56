package models.games.specialgames;

import controllers.Start.PlantSelection;
import models.App;
import models.Constants;
import models.entity.PlantCategory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
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
        if (App.getCurrentuser() == null) {
            return plantTypes;
        }
        for (PlantType plant : App.getCurrentuser().getUnlockedPlants()) {
            if(plant.getCategory() != PlantCategory.SunProducer) plantTypes.add(plant);
        }
        return plantTypes;
    }

    @Override
    public void attack() {

    }

    private boolean selectionFinished;
    private boolean wavesStarted;

    @Override
    public boolean startGame(String input) {
        if (availablePlants.size() != Constants.PLANTS_COUNT_IN_A_GAME) {
            return false;
        }

        selectionFinished = true;
        wavesStarted = false;
        availablePlants.values().forEach(packet -> {
            packet.setRecharge(0f);
            packet.setAvailable(true);
        });
        return true;
    }

    @Override
    public String playGame(float delta) {
        if (!selectionFinished) {
            return "Choose your plants first.";
        }
        if (!wavesStarted) {
            for (var packet : availablePlants.values()) {
                packet.update(Math.max(0f, delta));
            }
            return "Plant your seeds, then press START WAVES.";
        }
        return super.playGame(delta);
    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        if (!selectionFinished || wavesStarted) {
            return false;
        }
        return super.plant(plantName, x, y);
    }

    public void startWaves(){
        if (selectionFinished) {
            wavesStarted = true;
        }
    }

    public boolean isWavesStarted() {
        return wavesStarted;
    }



}
