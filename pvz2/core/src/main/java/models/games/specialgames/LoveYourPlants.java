package models.games.specialgames;

import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;

public class LoveYourPlants extends NormalGame implements SpecialGame {
    int deadPlants = 0;

    public LoveYourPlants(Chapters chapter, Level level) {
        super(chapter,level);
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return new ArrayList<>(selection.getPlantsToChoose());
    }

    @Override
    public void attack() {

    }

    @Override
    public void updatePlants(float delta) {
        int before = plantsInField.size();
        super.updatePlants(delta);
        deadPlants += Math.max(0, before - plantsInField.size());
    }

    @Override
    public Result check_endGame() {
        if(deadPlants >= 5){
            won = false;
            state = GameState.END;
            return new Result(true , "Loss" , null);
        }
        return super.check_endGame();
    }
}
