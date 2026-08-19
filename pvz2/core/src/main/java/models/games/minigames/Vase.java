package models.games.minigames;

/**
 * One vase placed on the Vase Breaker board.
 *
 * The model intentionally stores only gameplay state. Animation state belongs
 * to VaseBreakerView/VaseRenderer.
 */
public final class Vase {
    public enum Type {
        PLANT,
        ZOMBIE,
        RANDOM
    }

    private final int line;
    private final int tileIndex;
    private final Type type;

    public Vase(int line, int tileIndex, Type type) {
        this.line = line;
        this.tileIndex = tileIndex;
        this.type = type == null ? Type.RANDOM : type;
    }

    public int getLine() {
        return line;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public Type getType() {
        return type;
    }
}
