package models.games.minigames;

import models.entity.Plant;
import models.entity.Projectile;
import models.entity.Zombie;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Vase Breaker gameplay model.
 *
 * Important separation rule: this class knows nothing about LibGDX input,
 * cursors, textures or PAM animations. The view asks it to mutate game state.
 */
public class VaseBraker extends BaseGame {
    private static final int ROWS = 5;
    private static final int COLUMNS = 9;

    private final ArrayList<Vase> vases = new ArrayList<>();
    private final ArrayList<VaseSeedDrop> seedDrops = new ArrayList<>();
    private final ArrayList<PlantType> vasePlants = new ArrayList<>();
    private final ArrayList<String> vaseZombies = new ArrayList<>();
    private final Random random = new Random();

    private VaseSeedDrop selectedSeedDrop;
    private final int levelId;

    public VaseBraker(MinigameLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("Vase Breaker level cannot be null.");
        }

        levelId = Math.max(1, Math.min(3, level.getId()));

        if (level.getPlants() != null) {
            vasePlants.addAll(level.getPlants());
        }
        if (level.getZombiesNames() != null) {
            vaseZombies.addAll(level.getZombiesNames());
        }

        if (vasePlants.isEmpty()) {
            vasePlants.add(PlantType.PEASHOOTER);
            vasePlants.add(PlantType.WALL_NUT);
            vasePlants.add(PlantType.SNOW_PEA);
        }
        if (vaseZombies.isEmpty()) {
            vaseZombies.add("normal");
            vaseZombies.add("cone");
            vaseZombies.add("bucket");
        }

        // BaseGame only declares field; it does not construct it.
        // Vase Breaker uses a clean Ancient Egypt lawn, without chapter specials.
        field = new Field().initField(Chapters.AncientEgypt, 0);

        initVases();
        state = GameState.PLAYING;
    }

    private void initVases() {
        // Level 1 -> 4 right-most columns, level 2 -> 5, level 3 -> 6.
        // This also fixes the old off-by-one formula that requested more vases
        // than there were unique candidate cells.
        int vaseColumns = Math.min(6, 3 + levelId);
        int firstColumn = COLUMNS - vaseColumns;

        ArrayList<Cell> cells = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            for (int column = firstColumn; column < COLUMNS; column++) {
                cells.add(new Cell(column, row));
            }
        }
        Collections.shuffle(cells, random);

        int plantVaseCount = Math.min(levelId + 2, cells.size());
        int markedZombieVases = Math.min(Math.max(1, levelId), cells.size() - plantVaseCount);

        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            Vase.Type type;

            if (i < plantVaseCount) {
                type = Vase.Type.PLANT;
            } else if (i < plantVaseCount + markedZombieVases) {
                type = Vase.Type.ZOMBIE;
            } else {
                type = Vase.Type.RANDOM;
            }

            vases.add(new Vase(cell.row, cell.column, type));
            field.getTileByCoordinats(cell.column, cell.row).setEmpty(false);
        }
    }

    public VaseBreakResult breakVase(int column, int row) {
        Iterator<Vase> iterator = vases.iterator();

        while (iterator.hasNext()) {
            Vase vase = iterator.next();
            if (vase.getLine() != row || vase.getTileIndex() != column) {
                continue;
            }

            iterator.remove();
            Tile tile = field.getTileByCoordinats(column, row);
            tile.setEmpty(true);

            return switch (vase.getType()) {
                case PLANT -> dropPlant(vase);
                // The special gargantuar vase always contains a Gargantuar.
                case ZOMBIE -> spawnZombie(vase, "gargantuar");
                // Brown vases are actually random contents, not an empty third case.
                case RANDOM -> random.nextBoolean()
                    ? dropPlant(vase)
                    : spawnZombie(vase, randomZombieName());
            };
        }

        return VaseBreakResult.miss();
    }

    private VaseBreakResult dropPlant(Vase vase) {
        PlantType type = vasePlants.get(random.nextInt(vasePlants.size()));
        seedDrops.add(new VaseSeedDrop(type, vase.getLine(), vase.getTileIndex()));
        return VaseBreakResult.plant(vase, type);
    }

    private VaseBreakResult spawnZombie(Vase vase, String zombieName) {
        Zombie zombie = ZombieFactory.createZombie(zombieName);

        // See README_APPLY.md: Zombie.setRow/setLine must use 0..4 consistently.
        zombie.setRow(vase.getLine());
        zombie.setLine(vase.getLine());
        zombie.setTileIndex(vase.getTileIndex());
        zombie.setX((vase.getTileIndex() + 0.5f) * Tile.getWidth());
        zombie.setY(vase.getLine() * Tile.getHeight());
        zombies.add(zombie);

        return VaseBreakResult.zombie(vase, zombieName);
    }

    private String randomZombieName() {
        return vaseZombies.get(random.nextInt(vaseZombies.size()));
    }

    public boolean selectSeedDrop(VaseSeedDrop drop) {
        if (drop == null || !seedDrops.contains(drop)) {
            return false;
        }
        selectedSeedDrop = drop;
        return true;
    }

    public void cancelSeedSelection() {
        selectedSeedDrop = null;
    }

    public VaseSeedDrop getSelectedSeedDrop() {
        return selectedSeedDrop;
    }

    public PlantType getSelectedPlantType() {
        return selectedSeedDrop == null ? null : selectedSeedDrop.getPlantType();
    }

    /**
     * Plants the currently selected vase seed. The seed packet is consumed only
     * after a successful placement.
     */
    public boolean plantSelectedSeed(int column, int row) {
        if (selectedSeedDrop == null || !isValidCell(column, row)) {
            return false;
        }

        Tile tile = field.getTileByCoordinats(column, row);
        if (!tile.isEmpty() || !tile.isPlantable()) {
            return false;
        }

        Plant plant = plantFactory.createPlant(selectedSeedDrop.getPlantType());
        if (plant == null) {
            return false;
        }

        plant.setTileIndex(column);
        plant.setLine(row);
        plantsInField.add(plant);
        tile.setEmpty(false);

        seedDrops.remove(selectedSeedDrop);
        selectedSeedDrop = null;
        return true;
    }

    /**
     * Keeps Game's boolean plant contract valid. Graphical Vase Breaker normally
     * calls plantSelectedSeed() instead.
     */
    @Override
    public boolean plant(String plantName, int x, int y) {
        if (selectedSeedDrop == null || plantName == null) {
            return false;
        }
        if (!selectedSeedDrop.getPlantType().name().equalsIgnoreCase(plantName.trim())) {
            return false;
        }
        return plantSelectedSeed(x, y);
    }

    @Override
    public String pluck(int x, int y) {
        if (!isValidCell(x, y)) {
            return "Outside the lawn.";
        }

        Iterator<Plant> iterator = plantsInField.iterator();
        while (iterator.hasNext()) {
            Plant plant = iterator.next();
            if (plant.getTileIndex() == x && plant.getLine() == y) {
                iterator.remove();
                field.getTileByCoordinats(x, y).setEmpty(true);
                return "Plant removed.";
            }
        }

        return "There is no plant here.";
    }

    @Override
    public String playGame(float delta) {
        if (state != GameState.PLAYING) {
            return "";
        }

        float safeDelta = Math.max(0f, delta);
        updatePlants(safeDelta);

        // BaseGame updates plants but does not advance the projectiles they emit.
        // Iterate over a copy because Projectile.run() can remove itself from the game.
        for (Projectile projectile : new ArrayList<>(projectiles)) {
            if (projectile != null && projectiles.contains(projectile)) {
                projectile.run(safeDelta, this);
            }
        }

        updateZombies(safeDelta);
        updateScene(safeDelta);
        return "";
    }

    @Override
    public Result check_endGame() {
        for (Zombie zombie : zombies) {
            if (zombie.getX() <= 0f) {
                won = false;
                state = GameState.END;
                return new Result(true, "Loss", null);
            }
        }

        if (vases.isEmpty() && zombies.isEmpty()) {
            won = true;
            state = GameState.END;
            return new Result(true, "Won", null);
        }

        return new Result(false, null, null);
    }

    public List<Vase> getVases() {
        return Collections.unmodifiableList(vases);
    }

    public List<VaseSeedDrop> getSeedDrops() {
        return Collections.unmodifiableList(seedDrops);
    }

    public ArrayList<PlantType> getVasePlants() {
        return new ArrayList<>(vasePlants);
    }

    private static boolean isValidCell(int column, int row) {
        return column >= 0 && column < COLUMNS && row >= 0 && row < ROWS;
    }

    private static final class Cell {
        private final int column;
        private final int row;

        private Cell(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }
}
