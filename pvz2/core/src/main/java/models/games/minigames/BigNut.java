package models.games.minigames;

import models.Constants;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.ArrayList;

public class BigNut extends BowlingNut{
    public BigNut(float damage, boolean explosive) {
        super(damage, explosive);
    }

    public BigNut(float damage) {
        super(damage);
    }

    @Override
    public void go(float delta, BaseGame game) {
        float safeDelta = Math.max(0f, delta);
        x += velocityX * safeDelta;
        y += velocityY * safeDelta;
        syncGridPosition();
        if (this.x > 10 * Tile.getWidth() || this.x < -Tile.getWidth()) {
            dispose(game);
            return;
        }
        hit(game.getZombies());
    }

    private void hit(ArrayList<Zombie> zombies){
        for (Zombie z : zombies) {
            if (z != null && !z.isDead() && Constants.overlap(this , z)) {
                z.takeDamage(Integer.MAX_VALUE);
                if (!z.isDead()) {
                    // Big Nut is the guaranteed-kill variant, including zombies
                    // that still have an armour layer after taking damage.
                    z.die();
                }
                z.setHurt(true);
            }
        }
    }

    private void syncGridPosition() {
        float tileWidth = Math.max(1f, Tile.getWidth());
        float tileHeight = Math.max(1f, Tile.getHeight());
        tileIndex = Math.max(0, Math.min(8, (int) (x / tileWidth)));
        line = Math.max(0, Math.min(4, (int) (y / tileHeight)));
    }
}
