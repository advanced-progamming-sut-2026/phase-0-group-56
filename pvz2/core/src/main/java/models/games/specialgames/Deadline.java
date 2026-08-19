package models.games.specialgames;

import models.Constants;
import models.entity.Zombie;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;

public class Deadline extends NormalGame implements SpecialGame {
    public Deadline(Chapters chapter, Level level) {
        super(chapter, level);
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return null;
    }

    @Override
    public void attack() {

    }

    @Override
    public Result check_endGame() {
        for (Zombie z : zombies) {
            if(z.getTileIndex() <= Constants.DEAD_LINE_TILE_INDEX){
                return new Result(true , "Loss" , null);
            }
        }
        return new Result(false , null, null);
    }

    @Override
    public void endGame() {

    }
}
