package models.games.specialgames;

import models.App;
import models.Constants;
import models.entity.Plant;
import models.entity.PlantCategory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.gamepanes.Tile;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SaveOurSeeds extends NormalGame implements SpecialGame {
    private static final int PROTECTED_COLUMN_COUNT = 5;
    private static final int ROW_COUNT = 5;

    private final ArrayList<Plant> toProtect = new ArrayList<>();
    private final Random rand = new Random();

    public SaveOurSeeds(Chapters chapter, Level level) {
        super(chapter, level);
    }

    /**
     * NormalGame creates the Field here. Protected plants must be created only
     * after that has happened, otherwise isEmpty()/tile access cannot work.
     */
    @Override
    public void initGame(Chapters chapter, Level level) {
        super.initGame(chapter, level);
        toProtect.clear();
        plantProtecteds();
    }

    /**
     * Read-only live view used by the world renderer.
     */
    public List<Plant> getProtectedPlants() {
        return Collections.unmodifiableList(toProtect);
    }

    private void plantProtecteds() {
        if (field == null || App.getCurrentuser() == null) {
            return;
        }

        ArrayList<PlantType> candidates = new ArrayList<>(
            App.getCurrentuser().getUnlockedPlants()
        );
        /*
         * PlantType.category is not populated by the legacy plants.json
         * loader, so checking type.getCategory() alone lets zero-HP plants
         * (Cherry Bomb, mints, Imitater, ...) become protected.  They are
         * removed by NormalGame on the first update and the challenge loses
         * immediately.  Build a probe plant and keep only a living,
         * non-single-use candidate instead.
         */
        candidates.removeIf(type -> !isValidProtectedType(type));

        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates, rand);

        ArrayList<Cell> cells = new ArrayList<>();
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int col = 0; col < PROTECTED_COLUMN_COUNT; col++) {
                cells.add(new Cell(col, row));
            }
        }
        Collections.shuffle(cells, rand);

        int nextType = 0;
        for (Cell cell : cells) {
            if (toProtect.size() >= Constants.PROTECTED_SEEDS_COUNT) {
                break;
            }

            Plant protectedPlant = null;
            PlantType protectedType = null;

            for (int attempt = 0; attempt < candidates.size(); attempt++) {
                PlantType type = candidates.get((nextType + attempt) % candidates.size());
                Plant candidate;
                try {
                    candidate = plantFactory.createPlant(type);
                } catch (RuntimeException ignored) {
                    continue;
                }

                if (candidate != null && isEmpty(candidate, cell.col, cell.row)) {
                    protectedPlant = candidate;
                    protectedType = type;
                    nextType = (nextType + attempt + 1) % candidates.size();
                    break;
                }
            }

            if (protectedPlant == null || protectedType == null) {
                continue;
            }

            placeProtectedPlant(protectedPlant, protectedType, cell.col, cell.row);
        }
    }

    private boolean isValidProtectedType(PlantType type) {
        if (type == null || type.getCategory() == PlantCategory.Explosive) {
            return false;
        }
        try {
            Plant probe = plantFactory.createPlant(type);
            return probe != null
                && probe.getHp() > 0f
                && probe.getTags() != null
                && !probe.getTags().contains(models.entity.PlantTags.ONCE_USAGE)
                && !probe.getTags().contains(models.entity.PlantTags.EXPLOSIVE);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Pre-planted challenge plants are not regular seed placements: they do
     * not cost sun and do not need to exist in availablePlants.
     */
    private void placeProtectedPlant(Plant plant, PlantType type, int col, int row) {
        plantsInField.add(plant);

        Tile tile = field.getTiles().get(row).get(col);
        if ("LILY_PAD".equals(type.name())) {
            tile.setPlantable(true);
        }
        // A protected plant occupies its tile in every case.  Leaving Lily
        // Pad tiles empty allowed normal planting over a protected seed.
        tile.setEmpty(false);

        plant.setLine(row);
        plant.setTileIndex(col);
        toProtect.add(plant);
    }

    @Override
    public String pluck(int x, int y) {
        for (Plant plant : toProtect) {
            if (plant != null
                && plant.getTileIndex() == x
                && plant.getLine() == y
                && plant.isAlive()
                && plant.getHp() > 0f) {
                return "Protected plants cannot be plucked.";
            }
        }
        return super.pluck(x, y);
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return new ArrayList<>(selection.getPlantsToChoose());
    }

    @Override
    public void attack() {
    }

    @Override
    public Result check_endGame() {
        for (Plant plant : toProtect) {
            if (plant == null
                || !plant.isAlive()
                || plant.getHp() <= 0f
                || !plantsInField.contains(plant)) {
                won = false;
                state = GameState.END;
                return new Result(true, "Loss", null);
            }
        }
        return super.check_endGame();
    }

    private record Cell(int col, int row) {
    }
}
