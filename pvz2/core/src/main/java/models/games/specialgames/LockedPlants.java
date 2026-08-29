package models.games.specialgames;

import models.entity.PlantCategory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;

import java.util.ArrayList;

public class LockedPlants extends NormalGame implements SpecialGame {
    public LockedPlants(LockType type) {
        this.lockType = type;
    }

    public LockedPlants(Chapters chapter, Level level, LockType type) {
        super(chapter, level);
        this.lockType = type == null ? LockType.ByCategory : type;
    }
    public enum LockType{ByCategory , Random}
    LockType lockType ;
    @Override
    public ArrayList<PlantType> filterPlants() {
        return new ArrayList<>(selection.getPlantsToChoose());
    }


    @Override
    public boolean startGame(String plantName) {

        if(!availablePlants.isEmpty() && lockType == LockType.ByCategory) {
            PlantCategory lock;
            lock = availablePlants.lastEntry().getValue().getPlant().getCategory();

            selection.getPlantsToChoose().removeIf(plant -> plant.getCategory().equals(lock));
        }
        return super.startGame(plantName);

    }

    @Override
    public void attack() {

    }
}
