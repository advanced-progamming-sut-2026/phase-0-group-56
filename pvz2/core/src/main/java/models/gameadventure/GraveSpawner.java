package models.gameadventure;

import models.entity.Zombie;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.games.BaseGame;

import java.util.ArrayList;
import java.util.List;

public class GraveSpawner implements  ChapterSpecialEvent {


    public GraveSpawner(BaseGame game){


    }
    @Override
    public void run(BaseGame game, float delta) {
        if (game == null || game.getCurrentWave() == null || game.getField() == null) {
            return;
        }

        List<Tile> graveTiles = new ArrayList<>();
        for (ArrayList<Tile> row : game.getField().getTiles()) {
            for (Tile tile : row) {
                if (tile != null && tile.getTileType() == TileType.NECROMANCY) {
                    graveTiles.add(tile);
                }
            }
        }

        List<Zombie> waveZombies = game.getCurrentWave().getZombies();
        int count = Math.min(graveTiles.size(), waveZombies == null ? 0 : waveZombies.size());
        for (int index = 0; index < count; index++) {
            Zombie zombie = waveZombies.get(index);
            Tile tile = graveTiles.get(index);
            if (zombie != null) {
                zombie.setLine(tile.getLine());
                zombie.setTileIndex(tile.getCol());
                zombie.setX(tile.getX() + Tile.getWidth() / 2f);
                zombie.setY(tile.getY() + Tile.getHeight() / 2f);
            }
        }

        dispose(game);
    }


}
