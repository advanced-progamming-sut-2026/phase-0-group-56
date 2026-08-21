package models.entity;

import models.Constants;
import models.gamepanes.Tile;
import models.games.BaseGame;

public class LawnMower extends Entity {

    private static final int BOARD_COLUMN_COUNT = 9;

    public enum State {
        IDLE,
        RUNNING,
        USED
    }

    private State state = State.IDLE;

    public LawnMower(int line) {
        this.line = line;
        this.tileIndex = 0;

        this.width = Tile.getWidth();
        this.height = Tile.getHeight();

        /*
         * The mower starts at the left edge of the lawn.
         * Keeping x at zero also makes sure it activates before a zombie
         * reaches the game's loss boundary.
         */
        this.x = 0f;
        this.y = line * Tile.getHeight();

        this.stateTime = 0f;
    }

    public String run(float delta, BaseGame game) {
        if (delta <= 0f || game == null || state == State.USED) {
            return null;
        }

        if (state == State.IDLE) {
            return updateIdle(delta, game);
        }

        updateRunning(delta, game);
        return null;
    }

    private String updateIdle(float delta, BaseGame game) {
        stateTime += delta;

        Zombie triggeringZombie = findCollidingZombie(game);

        if (triggeringZombie == null) {
            return null;
        }

        state = State.RUNNING;
        stateTime = 0f;

        /*
         * Kill every zombie currently touching the mower.
         * This prevents the triggering zombie from surviving for one
         * additional frame.
         */
        killCollidingZombies(game);

        return "Lawn mower activated at line " + line + ".";
    }

    private void updateRunning(float delta, BaseGame game) {
        stateTime += delta;
        x += Constants.MOANER_SPEED * delta;

        killCollidingZombies(game);

        float boardRightEdge = Tile.getWidth() * BOARD_COLUMN_COUNT;

        if (x >= boardRightEdge) {
            state = State.USED;
            stateTime = 0f;
        }
    }

    private Zombie findCollidingZombie(BaseGame game) {
        if (game.getZombies() == null) {
            return null;
        }

        for (Zombie zombie : game.getZombies()) {
            if (canHit(zombie) && Constants.overlap(zombie, this)) {
                return zombie;
            }
        }

        return null;
    }

    private void killCollidingZombies(BaseGame game) {
        if (game.getZombies() == null) {
            return;
        }

        for (Zombie zombie : game.getZombies()) {
            if (canHit(zombie) && Constants.overlap(zombie, this)) {
                killZombie(zombie);
            }
        }
    }

    private boolean canHit(Zombie zombie) {
        return zombie != null
            && zombie.isAlive()
            && zombie.getHp() > 0
            && zombie.getLine() == line;
    }

    private void killZombie(Zombie zombie) {
        zombie.setHurt(true);
        zombie.setHp(0);
        zombie.setAlive(false);
    }

    public State getState() {
        return state;
    }

    public boolean isUsed() {
        return state == State.USED;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }
}
