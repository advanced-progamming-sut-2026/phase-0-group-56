package models.games.specialgames;

import controllers.datacontroller.Data;
import models.App;
import models.Constants;
import models.entity.Plant;
import models.entity.PlantCategory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Random;

public class SaveOurSeeds extends NormalGame implements SpecialGame {
    ArrayList<Plant> toProtect;



    public SaveOurSeeds(Chapters chapter, Level level) {
        super(chapter,level);
        toProtect = new ArrayList<>();
        plantProtecteds();
    }

    Random rand = new Random();
    private void plantProtecteds(){
        if(toProtect.size() == Constants.PROTECTED_SEEDS_COUNT){
            return;
        }
        int x = rand.nextInt(5);
        int y = rand.nextInt(5);
        int index = rand.nextInt(toProtect.size());
        PlantType plantType = App.getCurrentuser().getUnlockedPlants().get(index);
        if(plantType.getCategory() == PlantCategory.Explosive){
            plantProtecteds();
            return;
        }
        if(plant(plantType.name() , x, y)){
            toProtect.add(plantsInField.getLast());
        }
        plantProtecteds();
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
        for (Plant p : toProtect) {
            if(!p.isAlive()) return new Result(true , "Loss" , null);
        }
        return  new Result(false , null , null);
    }
}
