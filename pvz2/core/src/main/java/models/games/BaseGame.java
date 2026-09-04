package models.games;

import commands.GameCommands;
import controllers.Start.PlantSelection;
import controllers.datacontroller.SeedPackage;
import controllers.observer.WizardObserver;
import models.gameadventure.*;
import models.Constants;
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


    protected Chapters chapter;
    protected Level level;
    protected Field field ;
    protected ArrayList<Wave> waves = new ArrayList<>();
    protected ArrayList<Plant> plantsInField = new  ArrayList<>();
    protected LinkedHashMap<PlantType , SeedPackage> availablePlants = new  LinkedHashMap<>();
    protected SunBuilder sunBuilder = new  SunBuilder();
    protected Wave currentWave;
    protected Wave previousWave;
    protected ArrayList<Zombie> zombies = new ArrayList<>(); ///combination of current wave and next wave
    protected ArrayList<Projectile> projectiles =  new ArrayList<>();
    protected ArrayList<Sun> suns =  new  ArrayList<>();
    /** Collectibles dropped by defeated zombies and waiting for player input. */
    protected ArrayList<RewardDrop> rewardDrops = new ArrayList<>();
    protected GameCommands startGameCommand;
    protected ChapterSpecialEvent event;
    protected PlantFactory plantFactory = new PlantFactory();

    /** Short, structured HUD messages emitted only when a wave starts. */
    private String lastWaveAnnouncement = "";
    private String lastEventAnnouncement = "";



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
    public ArrayList<RewardDrop> getRewardDrops() { return rewardDrops; }
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
    public String getLastWaveAnnouncement() { return lastWaveAnnouncement; }
    public String getLastEventAnnouncement() { return lastEventAnnouncement; }

    @Override
    public void initGame(Chapters chapter , Level level) {

    }

    @Override
    public boolean startGame(String plantName) {
        return availablePlants.size() == Constants.PLANTS_COUNT_IN_A_GAME;
    }

    protected boolean plantSelection = false;
    public String add(String name){
        return null;
    }

    StringBuilder output = new StringBuilder();
    @Override
    public String playGame(float delta) {
        output = new StringBuilder();
        lastWaveAnnouncement = "";
        lastEventAnnouncement = "";


        for (SeedPackage x : availablePlants.values()){
            x.update(delta);
        }

        updateSuns(output , delta);

        updatePendingWaveZombies(delta);
        updateZombies(delta);
        updateRewardDrops(delta);
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
        // Zombie abilities may add/remove entities while they execute (for
        // example a dead barrel releasing Imps).  A snapshot keeps the
        // ArrayList iterator stable during that legitimate mutation.
        for (Zombie zombie : new ArrayList<>(zombies)) {
            if (zombie == null || !zombies.contains(zombie)) {
                continue;
            }
            // Tornado owns the position of carried zombies until it drops them.
            // Running Zombie.update() here as well makes the zombie walk ahead of
            // the sandstorm and lets its abilities act while it is still carried.
            if (event instanceof Tornado tornado && tornado.isCarrying(zombie)) {
                continue;
            }
            zombie.update(delta, this);
        }
        gridController.checkAndAttachZombies(zombies);
        gridController.updateItems();
        updateMowers(delta);

        for (Zombie zombie : new ArrayList<>(zombies)) {
            if (zombie != null && zombie.isDead()) {
                createRewardDrop(zombie);
                System.out.println("zombie died at (" + zombie.getX() + ", " + zombie.getY() + ")");
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

    private void updateRewardDrops(float delta) {
        for (RewardDrop drop : rewardDrops) {
            if (drop != null) {
                drop.update(delta);
            }
        }
        rewardDrops.removeIf(drop -> drop == null || drop.isExpired());
    }

    private void createRewardDrop(Zombie zombie) {
        if (zombie == null || zombie.isRewardDropCreated()
            || zombie.getRewardDropType() == null) {
            return;
        }
        float x = Math.max(0f, Math.min(8f * Tile.getWidth(), zombie.getX()));
        float y = Math.max(0f, Math.min(4f * Tile.getHeight(), zombie.getY()));
        rewardDrops.add(new RewardDrop(
            zombie.getRewardDropType(),
            zombie.getRewardDropAmount(),
            x,
            y
        ));
        zombie.markRewardDropCreated();
    }

    @Override
    public void updateScene(float delta) {
        field.updateScene(delta , this);
    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        return true;
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
            if(z.getX() <= -50) {
                won = false;
                state = GameState.END;
                return new Result(true , "Loss" , null);
            }
        }
        return new  Result(false, null,null);
    }

    @Override
    public void endGame() {
        state = GameState.END;
    }

    protected int waveID = 0;
    /** Zombies which belong to the active wave but have not entered the lawn yet. */
    private final ArrayDeque<Zombie> pendingWaveZombies = new ArrayDeque<>();
    /** Only zombies released from the current wave are used by the release gate. */
    private final Set<Zombie> spawnedWaveZombies =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private float waveSpawnTimer;
    private float waveSpawnElapsed;
    private float waveSpawnInterval = 1.25f;
    private float waveSpawnHealthThreshold;

    private static final float MIN_INITIAL_WAVE_RATIO = 0.15f;
    private static final float MAX_INITIAL_WAVE_RATIO = 0.75f;
    private static final float RELEASE_HEALTH_RATIO = 0.65f;
    private static final float RELEASE_MAX_WAIT = 6f;

    protected Result attack(float delta) {
        // A wave is complete only after every zombie belonging to that wave
        // is dead and the release queue is empty. Checking only the visible
        // burst could finish a level before later wave content was started.
        if (currentWave == null || currentWaveCleared()) {
            if (currentWave != null && waves != null && !waves.isEmpty()
                && currentWave == waves.getLast()) {
                won = true;
                state = GameState.END;
                return new Result(true , "Won" , null);
            }
            if (waves == null || waveID >= waves.size()) {
                won = true;
                state = GameState.END;
                return new Result(true, "Won", null);
            }
            previousWave = currentWave;
            currentWave = waves.get(waveID);
            boolean lastWave = waveID == waves.size() - 1;
            waveID += 1;
            StringBuilder spawnLog = startWave(currentWave, lastWave);
            event = switch (chapter){
                case AncientEgypt -> new Tornado(this);
                case FrozenCaves -> new IcyWind(this);
                case BigWaveBeach -> new Water(this);
                default -> new GraveSpawner(this);
            };
            lastWaveAnnouncement = setTheWaveZombies(lastWave);
            lastEventAnnouncement = eventMessage(chapter);
            StringBuilder announcement = new StringBuilder(lastWaveAnnouncement);
            if (spawnLog.length() > 0) {
                announcement.append('\n').append(spawnLog);
            }
            if (!lastEventAnnouncement.isBlank()) {
                announcement.append('\n').append(lastEventAnnouncement);
            }
            return new Result(true , announcement.toString() , null);
        }
        return new  Result(false, null,null);
    }

    private boolean currentWaveCleared() {
        if (currentWave == null || !pendingWaveZombies.isEmpty()) {
            return false;
        }

        ArrayList<Zombie> waveZombies = currentWave.getZombies();
        if (waveZombies == null) {
            return true;
        }
        for (Zombie zombie : waveZombies) {
            if (zombie != null && !zombie.isDead() && zombie.getHp() > 0f) {
                return false;
            }
        }

        // Some zombie abilities can create extra entities (for example imps
        // released from a barrel). Those entities are not stored in the wave's
        // original list, but they must still be cleared before advancing.
        for (Zombie zombie : zombies) {
            if (zombie != null && !zombie.isDead() && zombie.getHp() > 0f) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts a wave with a wave-id-dependent burst, then schedules the rest.
     * The final wave intentionally bypasses the queue and enters in full.
     */
    private StringBuilder startWave(Wave wave, boolean last) {
        pendingWaveZombies.clear();
        spawnedWaveZombies.clear();
        waveSpawnTimer = 0f;
        waveSpawnElapsed = 0f;
        waveSpawnHealthThreshold = 0f;

        StringBuilder spawnLog = new StringBuilder();

        ArrayList<Zombie> waveZombies = wave == null ? null : wave.getZombies();
        if (waveZombies == null || waveZombies.isEmpty()) {
            return spawnLog;
        }

        ArrayList<Zombie> validWaveZombies = new ArrayList<>();
        for (Zombie zombie : waveZombies) {
            if (zombie != null) {
                validWaveZombies.add(zombie);
            }
        }
        int total = validWaveZombies.size();
        if (total == 0) {
            return spawnLog;
        }

        // Later wave ids open with a larger burst.  The final wave is the one
        // deliberate exception: all of its zombies enter simultaneously.
        int waveNumber = wave.getId() > 0
            ? wave.getId()
            : Math.max(1, waveID);
        int totalWaves = Math.max(1, waves.size());
        float idProgress = totalWaves <= 1
            ? 1f
            : (waveNumber - 1f) / (totalWaves - 1f);
        float initialRatio = last
            ? 1f
            : Math.min(
            MAX_INITIAL_WAVE_RATIO,
            MIN_INITIAL_WAVE_RATIO
                + (MAX_INITIAL_WAVE_RATIO - MIN_INITIAL_WAVE_RATIO) * idProgress
        );
        int initialCount = last
            ? total
            : total <= 1
            ? 1
            : Math.max(
            1,
            Math.min(total - 1, (int) Math.ceil(total * initialRatio))
        );

        // Make releases visibly staggered; the health gate below can release a
        // zombie sooner when the lawn is being cleared quickly.
        waveSpawnInterval = last
            ? 0f
            : Math.max(0.65f, 2.0f - waveNumber * 0.05f);
        for (int index = 0; index < total; index++) {
            Zombie zombie = validWaveZombies.get(index);
            placeWaveZombie(zombie, index, index < initialCount);
            if (index < initialCount) {
                zombies.add(zombie);
                spawnedWaveZombies.add(zombie);
                appendSpawnMessage(spawnLog, zombie);
            } else {
                pendingWaveZombies.addLast(zombie);
            }
        }

        float initialHealth = activeWaveHealth();
        waveSpawnHealthThreshold = initialHealth * RELEASE_HEALTH_RATIO;
        waveSpawnTimer = waveSpawnInterval;
        return spawnLog;
    }

    private void updatePendingWaveZombies(float delta) {
        if (pendingWaveZombies.isEmpty()) {
            return;
        }

        float safeDelta = Math.max(0f, delta);
        waveSpawnTimer -= safeDelta;
        waveSpawnElapsed += safeDelta;

        if (waveSpawnTimer > 0f) {
            return;
        }

        float activeHealth = activeWaveHealth();
        boolean healthReady = activeHealth <= waveSpawnHealthThreshold;
        boolean timeReady = waveSpawnElapsed >= RELEASE_MAX_WAIT;
        if (!healthReady && !timeReady) {
            // Check the health gate frequently without spinning a release on
            // every frame while the timer is waiting.
            waveSpawnTimer = 0.25f;
            return;
        }

        Zombie zombie = pendingWaveZombies.removeFirst();
        zombies.add(zombie);
        spawnedWaveZombies.add(zombie);
        appendSpawnMessage(output, zombie);

        activeHealth = activeWaveHealth();
        waveSpawnHealthThreshold = activeHealth * RELEASE_HEALTH_RATIO;
        waveSpawnTimer = waveSpawnInterval;
        waveSpawnElapsed = 0f;
    }

    private float activeWaveHealth() {
        float health = 0f;
        for (Zombie zombie : spawnedWaveZombies) {
            if (zombie != null && !zombie.isDead()) {
                health += Math.max(0f, zombie.getHp());
            }
        }
        return health;
    }

    private void appendSpawnMessage(StringBuilder destination, Zombie zombie) {
        if (destination == null || zombie == null) {
            return;
        }
        destination.append("Zombie ")
            .append(zombie.getType())
            .append(" spawned at wave ")
            .append(waveID)
            .append(" in lane ")
            .append(zombie.getLine())
            .append(".\n");
    }

    private void placeWaveZombie(Zombie zombie, int index, boolean initiallyActive) {
        if (zombie == null) {
            return;
        }
        int line = Math.floorMod(index, 5);
        zombie.setLine(line);
        ArrayList<Tile> frozenTiles = frozenTilesForInitialWave();
        if (initiallyActive && previousWave == null && index < frozenTiles.size()) {
            Tile tile = frozenTiles.get(index);
            zombie.setLine(tile.getLine());
            zombie.setTileIndex(tile.getCol());
            zombie.setX(tile.getX() + Tile.getWidth() * 0.5f - zombie.getWidth() * 0.5f);
            zombie.setY(tile.getY());
            zombie.setFrozen(true);
            return;
        }
        zombie.setTileIndex(8);
        zombie.setX((int) (9 * Tile.getWidth() + 200));
        zombie.setY((int) (line * Tile.getHeight()));
    }

    private ArrayList<Tile> frozenTilesForInitialWave() {
        ArrayList<Tile> frozenTiles = new ArrayList<>();
        if (chapter != Chapters.FrozenCaves || field == null || previousWave != null) {
            return frozenTiles;
        }
        for (ArrayList<Tile> row : field.getTiles()) {
            for (Tile tile : row) {
                if (tile != null && tile.getTileType() == TileType.FROZEN) {
                    frozenTiles.add(tile);
                }
            }
        }
        return frozenTiles;
    }

    protected String setTheWaveZombies(boolean last) {
        return last
            ? "The final wave has come."
            : "Wave " + waveID + " started.";
    }

    private String eventMessage(Chapters currentChapter) {
        if (currentChapter == null) {
            return "";
        }
        return switch (currentChapter) {
            case AncientEgypt -> "Tornado is coming!";
            case FrozenCaves -> "Wind is blowing! ready to be frozen!";
            case BigWaveBeach -> "Low tide alert: the water surface is changing!";
            case DarkAge -> "Necromancy alert: zombies may rise from the graves!";
            default -> "Zombies are approaching!";
        };
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
            if (Math.abs(dx) <= range * 80 && z.getLine() == center.getLine()) {
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
            if (z.getLine() != row) continue;
            float dx = Math.abs(z.getX() - x);
            if (dx <= range * 80) {
                z.takeDamage(damage);
            }
        }
    }

    public Plant findPullablePlant(Zombie zombie) {
        int row = zombie.getLine();
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
        target.setLine(row);
    }

    public Plant getRandomPlantInRange(Zombie zombie, float range) {
        List<Plant> candidates = new ArrayList<>();
        for (Plant p : plantsInField) {
            if (p.getLine() != zombie.getLine()) continue;
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
            if (Math.abs(dx) <= range * 80 && z.getLine() == center.getLine()) {
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
                imp.setLine(source.getLine());
                imp.setPosition(source.getX() + 50, source.getY());
                zombies.add(imp);
            }
        } else if (spawnType.equals("grave")) {
            int row = source.getLine();
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

    /** Breaks ice encasing zombies near a fire plant. */
    public void meltFrozenZombiesNear(Plant source, float radius) {
        if (source == null) {
            return;
        }
        float maxDistance = Math.max(0f, radius) * Tile.getWidth();
        for (Zombie zombie : zombies) {
            if (zombie == null || !zombie.isEncasedInIce()
                || zombie.getLine() != source.getLine()) {
                continue;
            }
            if (Math.abs(zombie.getX() - source.getX()) <= maxDistance) {
                zombie.breakIce();
            }
        }
    }

    public boolean isDay() {
        return day;
    }
}
