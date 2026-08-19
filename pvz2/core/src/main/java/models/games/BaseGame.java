package models.games;

import commands.GameCommands;
import controllers.Start.PlantSelection;
import controllers.datacontroller.SeedPackage;
import controllers.observer.WizardObserver;
import models.App;
import models.gameadventure.*;
//import models.collection.ZombieRegistry;
import models.gameadventure.levels.Level;
import models.entity.*;
import models.factory.*;
import models.factory.builder.PlantType;
import models.factory.builder.SunBuilder;
import models.gamepanes.*;
import models.utils.Result;
import models.entity.ability.*;

import java.util.*;


public class BaseGame implements Game {
    public enum GameState{STARTING , PLAYING , PAUSE , END}
    protected GameState state =  GameState.STARTING;
    protected PlantSelection selection =  new PlantSelection();
    protected int sunCount = 0;
    protected int plantFoodsCount = 0;
    GridController gridController = new  GridController();
    protected boolean day = true;

    public int getPlantFoodsCount() {
        return plantFoodsCount;
    }

    public void setPlantFoodsCount(int plantFoodsCount) {
        this.plantFoodsCount = plantFoodsCount;
    }

    public ChapterSpecialEvent getEvent() {
        return event;
    }



    protected Field field ;
    protected ArrayList<Wave> waves = new ArrayList<>();
    protected ArrayList<Plant> plantsInField = new  ArrayList<>();
    protected LinkedHashMap<PlantType , SeedPackage> availablePlants = new  LinkedHashMap<>();
    protected SunBuilder sunBuilder = new  SunBuilder();
    protected Wave currentWave;
    protected Wave previousWave;
    protected ArrayList<Zombie> zombies = new ArrayList<>(); ///combination of current wave and next wave
    protected ArrayList<Projectile> projectiles =  new ArrayList<>();
    protected ArrayList<Sun> suns =  new ArrayList<>();
    protected GameCommands startGameCommand;
    protected ChapterSpecialEvent event;
    protected PlantFactory plantFactory = new PlantFactory();



    // ====== WIZARD OBSERVER ======
    private final WizardObserver wizardObserver = new WizardObserver();

    // ====== GETTERS & SETTERS ======
    public GridController getGridController() { return gridController; }

    public int getWaveID() { return waveID; }
    public void setWaveID(int waveID) { this.waveID = waveID; }
    public GameCommands getStartGameCommand() { return startGameCommand; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public int getSunCount() { return sunCount; }
    public void setSunCount(int sunCount) { this.sunCount = sunCount; }
    public ArrayList<Projectile> getBullets() { return projectiles; }
    public ArrayList<Sun> getSuns() { return suns; }
    public Wave getCurrentWave() { return currentWave; }
    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }
    public ArrayList<Wave> getWaves() { return waves; }
    public void setWaves(ArrayList<Wave> waves) { this.waves = waves; }
    public ArrayList<Plant> getPlantsInField() { return plantsInField; }
    public void setCurrentWave(Wave currentWave) { this.currentWave = currentWave; }
    public Wave getPreviousWave() { return previousWave; }
    public ArrayList<Zombie> getZombies() { return zombies; }
    public void setZombies(ArrayList<Zombie> zombies) { this.zombies = zombies; }
    public PlantSelection getSelection() { return selection; }
    public LinkedHashMap<PlantType, SeedPackage> getAvailable_plants() { return availablePlants; }
    public void setEvent(ChapterSpecialEvent event) { this.event = event; }

    @Override
    public void initGame(Chapters chapter , Level level) {

    }

    @Override
    public boolean startGame(String plantName) {
        return availablePlants.size() == 5;
    }

    protected boolean plantSelection = false;
    public String add(String name){
        return null;
    }

    StringBuilder output = new StringBuilder();
    @Override
    public String playGame(float delta) {
        output = new StringBuilder();


            for (SeedPackage x : availablePlants.values()){
                x.update(delta);
            }

            updateSuns(output , delta);

            updateZombies(delta);
            updatePlants(delta);
            updateScene(delta);
            Result result = attack(delta);
            if(result != null){
                output.append(result.message());
            }
            if(event!=null){
               event.run(this , delta);
            }


            return output.toString();

    }


    protected void updateSuns(StringBuilder output , float delta){
        for (Iterator<Sun> iterator = suns.iterator(); iterator.hasNext(); ) {
            Sun sun = iterator.next();
            String sunLanding = sun.land(delta, this);
            if(sunLanding != null){
                output.append(sunLanding);
            }
            if(sun.getRemainingTime() <= 0){
                sun.dispose(this);
                iterator.remove();
            }
        }
        Result sunlight = sunBuilder.sunLight(delta , this);
        if(sunlight != null){
            output.append(sunlight.message());
        }
    }




    @Override
    public void updatePlants(float delta) {
        Iterator<Plant> iterator = plantsInField.iterator();
        while (iterator.hasNext()) {
            Plant p = iterator.next();
            p.update(delta, this);
            if (p.getHp() <= 0) {
                p.dispose(this);
                iterator.remove();

                output.append("\n").append("Plant ")
                        .append(p.getType()).append(" died at (")
                        .append(p.getTileIndex()).append(" , ")
                        .append(p.getLine()).append(")");

                Tile tile = field.getTileByCoordinats(p.getTileIndex(), p.getLine());

                // QUICK FIX: Don't set empty to true if it's a water tile (implying a Lily Pad is still there)
                // You may need to expand this logic based on how you implemented Lily Pads
                if (!tile.isWater()) {
                    tile.setEmpty(true);
                }
            }
        }
    }
    protected void updateMowers(float delta) {
        if (field == null) {
            return;
        }

        for (LawnMower mower : field.getMoaners()) {
            if (mower == null) {
                continue;
            }

            String message = mower.run(delta, this);
            if (message != null && !message.isBlank()) {
                output.append(message).append('\n');
            }
        }
    }


    @Override
    public void updateZombies(float delta) {
        for (Zombie zombie : zombies) {
            zombie.update(delta, this);
        }
        gridController.checkAndAttachZombies(zombies);
        gridController.updateItems();
        updateMowers(delta);

        for (Zombie zombie : zombies) {
            if (event instanceof Tornado tornado && tornado.isCarrying(zombie)) {
                continue;
               }

            if (zombie.isDead()) {
                SunRobbingAbility sun = zombie.getAbility(SunRobbingAbility.class);
                if (sun != null && sun.getStolenSun() > 0) {
                    int released = sun.getStolenSun() / 2;
                    addSun(released);
                }
                String type = zombie.getType();
                if (type.toLowerCase().contains("barrel")) {
                    spawn(zombie, "imp", 2);
                }
                if (type.equals("wizard") || type.equals("ZombieWizard")) {
                    wizardObserver.releaseCats(zombie);
                }
            }
        }

        zombies.removeIf(Zombie::isDead);
    }

    @Override
    public void updateScene(float delta) {
        field.updateScene(delta , this);
    }

    @Override
    public String plant(String plantName, int x, int y) {
        return "";
    }


    @Override
    public String pluck(int x, int y) {
       return null;
    }

    protected boolean won = false;
    public boolean isWon() { return won; }

    @Override
    public Result check_endGame() {
        for (Zombie z : zombies) {
            if(z.getX() <= -50) return new Result(true , "Loss" , null);
        }
        return new  Result(false, null,null);
    }

    @Override
    public void endGame() {}

    protected int waveID = 0;
    protected Result attack(float delta) {
        if(currentWave == null || currentWave.isFinished()){
            if(currentWave!= null && currentWave == waves.getLast()){
                won = true;
                return new Result(true , "Won" , null);
            }
            previousWave = currentWave;
            currentWave = waves.get(waveID);
            zombies.addAll(currentWave.getZombies());
            waveID += 1;
         event = switch (App.getCurrentuser().getChapter()){
               case AncientEgypt -> new Tornado(this);
               case FrozenCaves -> new IcyWind(this);
               case BigWaveBeach -> new Water(this);
               default -> new GraveSpawner(this);
            };
           return new Result(true , setTheWaveZombies(waveID == waves.size()) , null);
        }
        return new  Result(false, null,null);
    }

    protected String setTheWaveZombies(boolean last) {
        StringBuilder output = new StringBuilder();
        int line = 0;
        for (Zombie z : zombies) {
            z.setLine(line % 5);
            z.setTileIndex(8);
            z.setX((int)(9 * Tile.getWidth() + 200));
            z.setY((int)(line * Tile.getHeight()));
            output.append("Zombie spawned at line " + line % 5 + " , watch out human!\n");
            line++;
        }
        String waveFlag = "wave " + waveID + " is approaching ... ";
        String lastWave = "final Wave is coming , be ready to be eaten!!! ar ara rararararara";
        if(!last) output.append(waveFlag);
        else output.append(lastWave);
        return output.toString();
    }

    public void addCat(Zombie wizard, Plant plant) {
        wizardObserver.addCat(wizard, plant);
    }



    // ====== PLANT HELPERS ======
    public Plant findByCoordinates(int x, int y) {
        for (Plant p : this.plantsInField) {
            if (p.getLine() == y && p.getTileIndex() == x) {
                return p;
            }
        }
        return null;
    }

    public Plant getPlantAt(int row, int col) {
        for (Plant p : plantsInField) {
            if (p.getLine() == row && p.getTileIndex() == col) {
                return p;
            }
        }
        return null;
    }

    public boolean isCellEmpty(int row, int col) {
        return getPlantAt(row, col) == null;
    }

    // ====== ZOMBIE ABILITIES ======
    public Zombie findNearestZombie(Zombie center, float range) {
        Zombie nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Zombie z : zombies) {
            if (z == center) continue;
            float dx = z.getX() - center.getX();
            if (Math.abs(dx) <= range * 80 && z.getRow() == center.getRow()) {
                float dist = Math.abs(dx);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = z;
                }
            }
        }
        return nearest;
    }

    public Plant findTargetPlant(Zombie zombie, float range) {
        Plant nearest = null;
        float minDist = Float.MAX_VALUE;
        for (Plant p : plantsInField) {
            if (p.getLine() != zombie.getLine()) continue;
            float dx = Math.abs(p.getX() - zombie.getX());
            if (dx <= range) {
                if (dx < minDist) {
                    minDist = dx;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    public void explodeArea(int row, float x, float range, int damage) {
        for (Plant p : plantsInField) {
            if (p.getLine() != row) continue;
            float dx = Math.abs(p.getX() - x);
            if (dx <= range * 80) {
                p.setHP(0);
            }
        }
    }

    public void explodeAreaOnZombies(int row, float x, float range, int damage) {
        for (Zombie z : zombies) {
            if (z.getRow() != row) continue;
            float dx = Math.abs(z.getX() - x);
            if (dx <= range * 80) {
                z.takeDamage(damage);
            }
        }
    }

    public Plant findPullablePlant(Zombie zombie) {
        int row = zombie.getRow();
        int col = zombie.getTileIndex();
        for (int i = 2; i <= 8; i++) {
            int targetCol = col + i;
            if (targetCol >= 9) break;
            Plant p = getPlantAt(row, targetCol);
            if (p != null && p.getHp() > 0) return p;
        }
        return null;
    }

    public void pullPlant(Zombie zombie, Plant plant) {
        int col = plant.getTileIndex();
        if (isCellEmpty(plant.getLine(), col - 1)) {
            plant.setTileIndex(col - 1);
        }
        if(plant.getTileIndex() == zombie.getTileIndex())
            plant.setHP(0.0f);
    }

    public void pullZombie(Zombie source, Zombie target) {
        float dx = target.getX() - source.getX();
        if (dx > 80) {
            target.setPosition(target.getX() - 80, target.getY());
        }
    }

    public void swapZombieToRow(Zombie target, int row) {
        target.setRow(row);
    }

    public Plant getRandomPlantInRange(Zombie zombie, float range) {
        List<Plant> candidates = new ArrayList<>();
        for (Plant p : plantsInField) {
            if (p.getLine() != zombie.getRow()) continue;
            float dx = p.getX() - zombie.getX();
            if (dx > 0 && dx <= range * 80) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    public Zombie getRandomZombieInRange(Zombie center, float range) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z == center) continue;
            float dx = z.getX() - center.getX();
            if (Math.abs(dx) <= range * 80 && z.getRow() == center.getRow()) {
                candidates.add(z);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    public Zombie getRandomZombie() {
        if (zombies.isEmpty()) return null;
        return zombies.get(new Random().nextInt(zombies.size()));
    }

    public void spawn(Zombie source, String spawnType, int count) {
        if (spawnType.equals("imp")) {
            for (int i = 0; i < count; i++) {
                Zombie imp = ZombieFactory.createZombie("imp");
                imp.setRow(source.getRow());
                imp.setPosition(source.getX() + 50, source.getY());
                zombies.add(imp);
            }
        } else if (spawnType.equals("grave")) {
            int row = source.getRow();
            int col = source.getTileIndex() + 1;
            if (col < 9 && isCellEmpty(row, col)) {
                spawnGrave(row, col);
            }
        }
    }

    public void spawnGrave(int row, int col) {
        GridItem grave = new GridItem("grave", row, col, 700, false, true);
        gridController.addGridItem(grave);

        Field field = getField();
        if (field != null) {
            Tile tile = field.getTiles().get(row).get(col);
            if (tile != null) {
                tile.setBlock(true);
                tile.setPlantable(false);
                tile.setTileType(TileType.DARK_AGE_GRAVE);
            }
        }
    }

    public boolean hasKilledPlant(Zombie zombie) {
        return zombie.getAllStarObserver() != null &&
                zombie.getAllStarObserver().isSlowed();
    }

    public boolean isArmorBroken(Zombie zombie, String armorType) {
        for (Armor armor : zombie.getArmors()) {
            if (armor.getType().equals(armorType) && armor.isBroken()) {
                return true;
            }
        }
        return false;
    }

    public void removeSun(int amount) {
        sunCount -= amount;
        if (sunCount < 0) sunCount = 0;
    }

    public void addSun(int amount) {
        sunCount += amount;
    }

    public boolean isDay() {
        return day;
    }
}

