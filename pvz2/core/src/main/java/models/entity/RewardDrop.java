package models.entity;

import models.gamepanes.Tile;
import models.games.BaseGame;

/** A short-lived collectible dropped by a defeated zombie. */
public final class RewardDrop extends Entity {
    public enum Type {
        COIN_GOLD,
        COIN_SILVER,
        DIAMOND,
        PLANT_FOOD
    }

    private static final float LIFETIME_SECONDS = 20f;
    private final Type type;
    private final int amount;
    private float remainingTime = LIFETIME_SECONDS;

    public RewardDrop(Type type, int amount, float x, float y) {
        this.type = type;
        this.amount = Math.max(1, amount);
        this.width = 50f;
        this.height = 50f;
        this.x = x;
        this.y = y;
        this.line = Math.max(0, Math.min(4, (int) (y / Tile.getHeight())));
        this.tileIndex = Math.max(0, Math.min(8, (int) (x / Tile.getWidth())));
    }

    public void update(float delta) {
        remainingTime -= Math.max(0f, delta);
    }

    public boolean isExpired() {
        return remainingTime <= 0f;
    }

    public Type getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public float getRemainingTime() {
        return remainingTime;
    }

    /** Kept for symmetry with other world entities and future special effects. */
    public void dispose(BaseGame game) {
        remainingTime = 0f;
    }
}
