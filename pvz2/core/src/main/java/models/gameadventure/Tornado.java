package models.gameadventure;

import models.Constants;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ancient Egypt sandstorm event.
 *
 * A subset of the current wave is temporarily carried from the right side of
 * the board to a random destination column. While a zombie is carried it stays
 * in BaseGame#getZombies(), but BaseGame should skip its normal update and let
 * this event own its horizontal movement.
 */
public class Tornado implements ChapterSpecialEvent {

    private final ArrayList<Zombie> carriedZombies = new ArrayList<>();
    private final Map<Zombie, Integer> destinations = new IdentityHashMap<>();

    public Tornado(BaseGame game) {
        if (game == null || game.getCurrentWave() == null) {
            return;
        }

        Random random = new Random();
        List<Zombie> waveZombies = game.getCurrentWave().getZombies();
        if (waveZombies == null || waveZombies.isEmpty()) {
            return;
        }

        int hardness = Math.max(1, game.getCurrentWave().getHardness());
        int upperBound = Math.max(1, hardness * Constants.DISASTER_ZOMBIES_BASE_COUNT);

        // At least one zombie when a Tornado event is actually created, but never
        // more than the number that exists in this wave.
        int count = Math.min(waveZombies.size(), random.nextInt(upperBound) + 1);

        // Keep zombies in the wave list. Wave.isFinished() calculates progress from
        // that list, so removing tornado zombies would make the wave finish early.
        for (int i = 0; i < count; i++) {
            Zombie zombie = waveZombies.get(i);
            carriedZombies.add(zombie);

            // Original intent was columns 5..8.
            int destinationColumn = 5 + random.nextInt(4);
            destinations.put(zombie, destinationColumn);
        }
    }

    @Override
    public void run(BaseGame game, float delta) {
        if (game == null) {
            return;
        }

        if (carriedZombies.isEmpty()) {
            dispose(game);
            return;
        }

        float movement = Math.max(0f, delta) * Constants.TORNADO_VELOCITY;
        for (int i = carriedZombies.size() - 1; i >= 0; i--) {
            Zombie zombie = carriedZombies.get(i);
            if (zombie == null || zombie.isDead()) {
                destinations.remove(zombie);
                carriedZombies.remove(i);
                continue;
            }

            Integer destinationColumn = destinations.get(zombie);
            if (destinationColumn == null) {
                carriedZombies.remove(i);
                continue;
            }

            // All entity/projectile coordinates are expressed in Tile units.
            // The previous legacy 100 + column * 50 conversion did not match
            // the current 82px-wide model tiles, so collision and rendering
            // disagreed about where a carried zombie actually was.
            float tileWidth = Math.max(1f, Tile.getWidth());
            float destinationX = destinationColumn * tileWidth;
            float nextX = zombie.getX() - movement;

            if (nextX <= destinationX) {
                zombie.setX(destinationX);
                zombie.setTileIndex(destinationColumn);
                destinations.remove(zombie);
                carriedZombies.remove(i);
            } else {
                zombie.setX(nextX);
                int currentColumn = (int) Math.floor(nextX / tileWidth);
                zombie.setTileIndex(Math.max(0, Math.min(8, currentColumn)));
            }
        }

        if (carriedZombies.isEmpty()) {
            dispose(game);
        }
    }

    /** Zombies currently hidden inside / transported by the sandstorm. */
    public List<Zombie> getCarriedZombies() {
        return Collections.unmodifiableList(carriedZombies);
    }

    /** Used by BaseGame so normal Zombie.update() does not fight this event. */
    public boolean isCarrying(Zombie zombie) {
        return zombie != null && destinations.containsKey(zombie);
    }
}
