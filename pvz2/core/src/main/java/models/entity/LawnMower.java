package models.entity;

import models.Constants;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.utils.AudioManager;

/**
 * One single-use lawn mower for one lane.
 *
 * Model coordinates use the same logical board space as the rest of the game:
 * 9 * Tile.getWidth() by 5 * Tile.getHeight().
 */
public class LawnMower extends Entity {

    public enum State {
        IDLE,
        RUNNING,
        USED
    }

    private static final float WIDTH_IN_TILES = 0.85f;
    private static final float HEIGHT_IN_ROWS = 0.80f;
    private static final float START_X_IN_TILES = -0.60f;
    private static final float EXIT_MARGIN_IN_TILES = 1.0f;

    private State state = State.IDLE;

    public LawnMower(int line) {
        this.line = line;

        this.width = Tile.getWidth() * WIDTH_IN_TILES;
        this.height = Tile.getHeight() * HEIGHT_IN_ROWS;

        // Keep the mower just to the left of the first playable tile, while
        // allowing a small part of its collision box to touch the lawn.
        this.x = Tile.getWidth() * START_X_IN_TILES;
        this.y = line * Tile.getHeight()
            + (Tile.getHeight() - this.height) * 0.5f;
    }

    /**
     * Updates activation, movement and zombie mowing.
     *
     * @return a log message only on the frame this mower activates.
     */
    public String run(float delta, BaseGame game) {
        if (state == State.USED || game == null) {
            return null;
        }

        float safeDelta = Math.max(0f, delta);
        stateTime += safeDelta;

        String message = null;

        if (state == State.IDLE) {
            boolean activated = false;
            for (Zombie zombie : game.getZombies()) {
                if (canHit(zombie) && Constants.overlap(zombie, this)) {
                    if (!activated) {
                        activate();
                        AudioManager.getInstance().play("lawnmower");
                        activated = true;
                    }
                    kill(zombie);
                }
            }
            if (activated) {
                message = "Lawn Mawner turned on at line " + line;
            }
        }

        if (state == State.RUNNING) {
            float previousX = x;
            x += Constants.MOANER_SPEED * safeDelta;

            // Use the whole swept horizontal interval so a large delta cannot
            // make the mower tunnel through a zombie between two frames.
            for (Zombie zombie : game.getZombies()) {
                if (canHit(zombie)
                    && intersectsSweptArea(zombie, previousX, x)) {
                    kill(zombie);
                }
            }

            float boardRight = 9f * Tile.getWidth();
            float exitX = boardRight
                + EXIT_MARGIN_IN_TILES * Tile.getWidth();

            if (x > exitX) {
                state = State.USED;
                stateTime = 0f;
                setAlive(false);
            }
        }

        return message;
    }

    private void activate() {
        state = State.RUNNING;
        stateTime = 0f;
    }

    private static boolean canHit(Zombie zombie) {
        return zombie != null && !zombie.isDead();
    }

    private boolean intersectsSweptArea(
        Zombie zombie,
        float previousX,
        float currentX
    ) {
        boolean verticalOverlap =
            zombie.getY() < y + height
                && zombie.getY() + zombie.getHeight() > y;

        if (!verticalOverlap) {
            return false;
        }

        float sweptLeft = Math.min(previousX, currentX);
        float sweptRight = Math.max(
            previousX + width,
            currentX + width
        );

        return zombie.getX() < sweptRight
            && zombie.getX() + zombie.getWidth() > sweptLeft;
    }

    private static void kill(Zombie zombie) {
        if (zombie == null || zombie.isDead()) {
            return;
        }

        zombie.setHurt(true);
        zombie.setAlive(false);
        zombie.setHp(0);
        zombie.die();

        // Feed the event-driven quest system from the authoritative mower
        // kill path.  Zombie#die emits the generic kill event, but it cannot
        // identify the killing source; this event is therefore emitted here.
        models.QuestProgress.add("LAWNMOWER_KILL", 1);
    }

    public State getState() {
        return state;
    }

    public boolean isOn() {
        return state == State.RUNNING;
    }

    public boolean isUsed() {
        return state == State.USED;
    }
}
