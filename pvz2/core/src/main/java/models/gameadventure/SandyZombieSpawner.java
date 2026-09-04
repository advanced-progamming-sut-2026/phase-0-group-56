package models.gameadventure;

import models.entity.Zombie;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.games.BaseGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Big Wave Beach event.
 * Moves wave zombies that are selected by the wave system to sandy tiles,
 * making them emerge from the beach sand.
 */
public class SandyZombieSpawner implements ChapterSpecialEvent {

    public SandyZombieSpawner(BaseGame game) {
    }

    @Override
    public void run(BaseGame game, float delta) {
        if (game == null || game.getCurrentWave() == null || game.getField() == null) {
            return;
        }

        List<Tile> sandTiles = new ArrayList<>();
        for (ArrayList<Tile> row : game.getField().getTiles()) {
            for (Tile tile : row) {
                if (tile != null && tile.getTileType() == TileType.SANDY_TILE) {
                    sandTiles.add(tile);
                }
            }
        }

        List<Zombie> waveZombies = game.getCurrentWave().getZombies();
        int count = Math.min(sandTiles.size(), waveZombies == null ? 0 : waveZombies.size());

        for (int i = 0; i < count; i++) {
            Zombie zombie = waveZombies.get(i);
            Tile tile = sandTiles.get(i);

            if (zombie == null) {
                continue;
            }

            zombie.setLine(tile.getLine());
            zombie.setTileIndex(tile.getCol());
            zombie.setX(tile.getX() + Tile.getWidth() / 2f);
            zombie.setY(tile.getY() + Tile.getHeight() / 2f);
        }

        dispose(game);
    }
}
