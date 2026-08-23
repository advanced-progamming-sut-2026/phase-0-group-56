package models.games.minigames;

import models.Constants;
import models.entity.Entity;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.ArrayList;

/** A rolling Wall-Nut projectile used by Wall-Nut Bowling. */
public class BowlingNut extends Entity {
    private final float damage;
    private final boolean explosive;

    public BowlingNut(float damage, boolean explosive) {
        this.damage = damage;
        this.explosive = explosive;
        width = Tile.getWidth() * 0.62f;
        height = Tile.getHeight() * 0.62f;
    }

    public BowlingNut(float damage) {
        this(damage, false);
    }

    public void go(float delta, BaseGame game) {
        float safeDelta = Math.max(0f, delta);
        x += velocityX * safeDelta;
        y += velocityY * safeDelta;

        if (block()) {
            y = Math.max(0f, Math.min(y, 5f * Tile.getHeight() - height));
            velocityY *= -1f;
        } else if (hit(game.getZombies())) {
            if (explosive) {
                dispose(game);
                return;
            }
            velocityY *= -1f;
        }

        tileIndex = Math.max(0, Math.min(8, (int) (x / Tile.getWidth())));
        line = Math.max(0, Math.min(4, (int) (y / Tile.getHeight())));
        if (x >= 9f * Tile.getWidth() || x < -Tile.getWidth()) {
            dispose(game);
        }
    }

    protected void dispose(BaseGame game) {
        WallnutBowling wallnutBowling = (WallnutBowling) game;
        wallnutBowling.getNuts().remove(this);
        if (!explosive) {
            return;
        }

        for (Zombie zombie : game.getZombies()) {
            int dx = Math.abs(tileIndex - zombie.getTileIndex());
            int dy = Math.abs(line - zombie.getLine());
            if (dx <= 1 && dy <= 1) {
                zombie.takeDamage((int) damage);
                zombie.setHurt(true);
            }
        }
    }

    private boolean block() {
        return y <= 0f || y + height >= 5f * Tile.getHeight();
    }

    private boolean hit(ArrayList<Zombie> zombies) {
        for (Zombie zombie : zombies) {
            if (Constants.overlap(this, zombie)) {
                zombie.takeDamage((int) damage);
                zombie.setHurt(true);
                return true;
            }
        }
        return false;
    }

    public boolean isExplosive() {
        return explosive;
    }
}
