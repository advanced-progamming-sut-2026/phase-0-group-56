package models.games.specialgames;

import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;

import java.util.ArrayList;

public class TimedWar extends NormalGame implements SpecialGame {
    public TimedWar(Chapters chapter, Level level) {
        super(chapter,level);
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return null;
    }

    @Override
    public void attack() {

    }
}
