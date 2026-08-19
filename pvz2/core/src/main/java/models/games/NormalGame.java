package models.games;

import controllers.datacontroller.SeedPackage;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.entity.*;
import models.factory.builder.PlantType;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.gamepanes.Wave;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Iterator;

public class NormalGame extends BaseGame{

    @Override
    public void initGame(Chapters chapter , Level level) {
        waves = new  ArrayList<>();
        this.field = new Field().initField(chapter , level.getId());
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; j++) {
                Tile tile = field.getTileByCoordinats(j , i);
                System.out.println("coordinates (" + tile.getCol()+ ", " + tile.getLine() +
                        " )" + " , type : " + tile.getTileType());
            }
        }
        initWaves(level);
        //initTestWave();

    }



    private void initWaves(Level level){
        int wavesCount = level.getWaves();
        float baseCost = level.getBaseHardness();
        ArrayList<String>  zombies = level.getAllowedZombies();///filtered zombies for this level
        for (int i = 0; i < wavesCount - 1; i++) {
            Wave wave = new Wave();
            float lastCost ;
            try {
                lastCost = waves.getLast() == null ? baseCost : waves.getLast().getCost();
            } catch (Exception e){
                lastCost = baseCost;
            }
            wave.setId(i + 1);
            wave.setCost(lastCost * 1.25f);
            wave.initWave(zombies);
            waves.add(wave);
        }

        Wave finalWave = new Wave();
        finalWave.setId(wavesCount);
        finalWave.setCost(waves.getLast().getCost() * 2);
        finalWave.initWave(zombies);
        waves.add(finalWave);
    }

    protected void updateProjectiles(float delta) {
        ArrayList<Projectile> snapshot = new ArrayList<>(projectiles);

        for (Projectile projectile : snapshot) {
            if (projectile != null && projectiles.contains(projectile)) {
                projectile.run(delta, this);
            }
        }

        float margin = 300f;
        float maxX = 9f * Tile.getWidth() + margin;
        float maxY = 5f * Tile.getHeight() + margin;

        projectiles.removeIf(projectile ->
            projectile == null
                || projectile.getX() < -margin
                || projectile.getX() > maxX
                || projectile.getY() < -margin
                || projectile.getY() > maxY
        );
    }

    @Override
    public String playGame(float delta) {
        updateProjectiles(delta);
        mawners(delta);
        return super.playGame(delta);
    }

    @Override
    public String plant(String plantName, int x, int y) {
        String name = plantName.replaceAll(" " , "_").toUpperCase();
        Result findPlant = plantAvailable(name);
        if(!findPlant.success()){
            return findPlant.message();
        }
        try {
            if(availablePlants.get(findPlant.plantType()).getCost() > sunCount){
                return "Can't plant , not enough suns.";
            }
        }catch (Exception e){
            return "This plant , you haven't selected!\n - Yoda";
        }

            Plant newPlant = plantFactory.createPlant(findPlant.plantType());
        if(!isEmpty(newPlant ,x, y)) {
            return "The coordination is not empty or plantable.";
        }
        plantsInField.add(newPlant);
        Tile tile = field.getTiles().get(y).get(x);
        if(plantName.equals("LILY_PAD")){
            tile.setPlantable(true);
        } else {
            tile.setEmpty(false);
        }
        newPlant.setLine(y);
        newPlant.setTileIndex(x);
        SeedPackage seedPackage = availablePlants.get(findPlant.plantType());
        if(seedPackage.getBoost()){
            newPlant.setPlantFood(true);
            seedPackage.setBoost(false);
        }
        this.sunCount -= (int) availablePlants.get(findPlant.plantType()).getCost();
        return "New plant : " + findPlant.plantType().name() + " planted successfully at coordination :" +
                " ( " + x + "," + y + ")";
    }

    protected Result plantAvailable(String plantName) {
        try {
            PlantType type = PlantType.valueOf(plantName.toUpperCase());
            if(!availablePlants.containsKey(type)) {
                return new Result(false , "The plant doesn't exist on the available plants.",null);
            }
            return new Result(true, null,type);

        } catch (IllegalArgumentException e) {
            return new Result(false , "The plant doesn't exist on the available plants.",null);
        }

    }

    protected boolean isEmpty(Plant type, int x, int y) {
        boolean waterPlant = type.getTags().contains(PlantTags.WATER);
        Tile tile = field.getTileByCoordinats(x, y);
        Plant existingPlant = findByCoordinates(x, y);

        // Pea Pod Stacking logic
        if (existingPlant != null && existingPlant.getType() == PlantType.PEA_POD && type.getType() == PlantType.PEA_POD) {
            return true;
        }
        else if (type.getType() == PlantType.GRAVE_BUSTER) {
            return tile.getTileType() == TileType.EGYPTIAN_GRAVE || tile.getTileType() == TileType.DARK_AGE_GRAVE;
        }

        // FIXED: get(y).get(x) because y = row, x = col
        Tile toPlantOn = field.getTiles().get(y).get(x);
        boolean water = toPlantOn.isWater() || !waterPlant;

        return toPlantOn.isEmpty() &&
                (toPlantOn.isPlantable() || (type.getArmor().isEmpty() && type.getType() == PlantType.PUMPKIN)) &&
                water;
    }


    @Override
    public String pluck(int x, int y) {
        // FIXED: y is row (line), x is column (tileIndex)
        Tile toPluckOn = field.getTiles().get(y).get(x);
        Iterator<Plant> iterator = plantsInField.iterator();
        boolean found = false;

        while (iterator.hasNext()) {
            Plant p = iterator.next();
            if (p.getLine() == y && p.getTileIndex() == x) {
                // Check to avoid plucking Lily Pads if they have a plant on them
                if (toPluckOn.isEmpty() && toPluckOn.isPlantable() && toPluckOn.isWater()) {
                    continue;
                }
                p.dispose(this);
                iterator.remove(); // FIXED: Actually removes the plant from the list
                found = true;
            }
        }

        if (found) {
            toPluckOn.setEmpty(true);
            return "Plant plucked successfully.";
        }

        return "ُThere's no plant to pluck.";
    }


    @Override
    public String add(String name) {
        if(availablePlants.size() == 8) return "Impossible. Slots are full";
        if(name.equalsIgnoreCase("Imitater")) name = availablePlants.lastEntry().getKey().name();
        SeedPackage seedPackage = selection.selectPlant(name);
       if(seedPackage != null){
           availablePlants.put(seedPackage.getPlant(), seedPackage);
       }
       else {
           return "This plant is not on the list.";
       }
        if(availablePlants.size() == 8) {
            plantSelection = true;
        }
        return "Plant added successfully";
    }


    public String nuke(){
        for (Zombie x : zombies){
            x.setHp(0);
            x.setAlive(false);
        }
        return "Booooom. zombies got nuked";
    }

    private void mawners(float delta){
        for (LawnMower x : this.field.getMoaners()){
            String a = x.run(delta , this);
            if(a != null) output.append(a);
        }
    }

    public String boost(PlantType type){
        SeedPackage seedPackage = availablePlants.get(type);
        seedPackage.setBoost(true);
        return type + " boosetd.";
    }
}
