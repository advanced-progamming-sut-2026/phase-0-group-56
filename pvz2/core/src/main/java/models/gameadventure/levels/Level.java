package models.gameadventure.levels;

import models.gameadventure.Chapters;
import models.factory.builder.PlantType;

import java.io.Serializable;
import java.util.ArrayList;

public class Level implements Serializable {
    private int id;
    private Chapters chapters;
    private String levelType; // Normal, ConveyorBelt, LockedPlants, ...
    private int waves;
    private float baseHardness;
    private ArrayList<String> allowedZombies;
    private ArrayList<PlantType> unlockingPlants;

    public Level(){

    }



    public Level(int id, Chapters chapters, String levelType, int waves, float baseHardness) {
        this.id = id;
        this.chapters = chapters;
        this.levelType = levelType;
        this.waves = waves;
        this.baseHardness = baseHardness;
        this.allowedZombies = new ArrayList<>();
        this.unlockingPlants = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Chapters getChapters() { return chapters; }
    public void setChapters(Chapters chapters) { this.chapters = chapters; }

    public String getLevelType() { return levelType; }
    public void setLevelType(String levelType) { this.levelType = levelType; }

    public int getWaves() { return waves; }
    public void setWaves(int waves) { this.waves = waves; }

    public float getBaseHardness() { return baseHardness; }
    public void setBaseHardness(float baseHardness) { this.baseHardness = baseHardness; }

    public ArrayList<String> getAllowedZombies() { return allowedZombies; }
    public void setAllowedZombies(ArrayList<String> allowedZombies) { this.allowedZombies = allowedZombies; }

    public ArrayList<PlantType> getUnlockingPlants() { return unlockingPlants; }
    public void setUnlockingPlants(ArrayList<PlantType> unlockingPlants) { this.unlockingPlants = unlockingPlants; }
}
