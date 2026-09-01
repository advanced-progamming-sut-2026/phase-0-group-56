package models.games.specialgames;

import models.entity.PlantCategory;
import models.factory.builder.PlantType;
import controllers.datacontroller.SeedPackage;
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
            SeedPackage lastPackage = null;
            for (SeedPackage packageItem : availablePlants.values()) {
                lastPackage = packageItem;
            }
            if (lastPackage == null || lastPackage.getPlant() == null) {
                return false;
            }
            lock = lastPackage.getPlant().getCategory();

            selection.getPlantsToChoose().removeIf(plant -> plant != null && plant.getCategory() == lock);
        }
        return super.startGame(plantName);

    }

    @Override
    public void attack() {

    }
}
