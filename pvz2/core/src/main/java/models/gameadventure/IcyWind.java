package models.gameadventure;

import models.entity.Plant;
import models.games.BaseGame;

import java.util.Random;

public class IcyWind implements ChapterSpecialEvent{
    private static final float BLOW_DURATION_SECONDS = 1.25f;

    private float remaining = BLOW_DURATION_SECONDS;
    private boolean freezeApplied;
    private int affectedRow = -1;

    public IcyWind(BaseGame game) {

    }

    @Override
    public void run(BaseGame game, float delta) {
        if (!freezeApplied) {
            Random rand = new Random();
            affectedRow = rand.nextInt(5);
            for (Plant plant : game.getPlantsInField()) {
                if (plant.getLine() == affectedRow) {
                    plant.setFreezeLevel(plant.getFreezeLevel() + 1);
                }
            }
            freezeApplied = true;
        }

        remaining -= Math.max(0f, delta);
        if (remaining <= 0f) {
            dispose(game);
        }
    }

    /** True while this wave's visual wind is still blowing. */
    public boolean isBlowing() {
        return remaining > 0f;
    }

    public float getRemaining() {
        return remaining;
    }

    public int getAffectedRow() {
        return affectedRow;
    }

}
