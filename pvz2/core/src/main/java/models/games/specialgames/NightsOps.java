package models.games.specialgames;

import controllers.datacontroller.SeedPackage;
import models.Constants;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;

public class NightsOps extends NormalGame implements SpecialGame {

    public NightsOps(Chapters chapter, Level level) {
        super(chapter, level);
        day = false;
    }

    @Override
    public Result check_endGame() {
       return super.check_endGame();
    }

    @Override
    public boolean startGame(String plantName) {
        SeedPackage selected = selection.selectPlant(plantName);


        return availablePlants.size() == Constants.PLANTS_COUNT_IN_A_GAME;
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return null;
    }

    @Override
    public void attack() {

    }
}
