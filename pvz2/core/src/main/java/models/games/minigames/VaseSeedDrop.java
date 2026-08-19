package models.games.minigames;

import models.factory.builder.PlantType;

/**
 * A plant seed packet dropped from a vase and still lying on the board.
 * It stays in the model while selected, so cancelling placement does not eat it.
 */
public final class VaseSeedDrop {
    private final PlantType plantType;
    private final int line;
    private final int tileIndex;

    public VaseSeedDrop(PlantType plantType, int line, int tileIndex) {
        this.plantType = plantType;
        this.line = line;
        this.tileIndex = tileIndex;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public int getLine() {
        return line;
    }

    public int getTileIndex() {
        return tileIndex;
    }
}
