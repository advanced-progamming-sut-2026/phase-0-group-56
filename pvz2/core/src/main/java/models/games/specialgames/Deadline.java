package models.games.specialgames;

import models.Constants;
import models.entity.Zombie;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;

public class Deadline extends NormalGame implements SpecialGame {
    private final int deadLine;

    public Deadline(Chapters chapter, Level level) {
        super(chapter, level);
        // Keep the marker inside the nine-column lawn.  Level ids are global
        // (1..16), so using the raw id would put the deadline outside the map
        // and immediately lose level 7 when zombies spawn at column 8.
        deadLine = Math.min(7, Constants.DEAD_LINE_TILE_INDEX + Math.max(0, level.getId() - 7));
    }

    /**
     * Last column zombies are allowed to enter before the challenge is lost.
     * The renderer draws the visual marker on the right edge of this column.
     */
    public int getDeadLine() {
        return deadLine;
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
        for (Zombie z : zombies) {
            if (z.getTileIndex() <= deadLine) {
                won = false;
                state = GameState.END;
                return new Result(true, "Loss", null);
            }
        }
        return super.check_endGame();
    }

    @Override
    public void endGame() {
        super.endGame();
    }
}
