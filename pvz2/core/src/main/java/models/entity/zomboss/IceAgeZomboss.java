package models.entity.zomboss;

import models.entity.*;
import models.games.BaseGame;
import models.factory.ZombieFactory;
import models.gamepanes.Tile;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IceAgeZomboss extends Zomboss {

    private final Random random = new Random();

    public IceAgeZomboss(float x, float y, int lowestLine) {
        super(x, y, lowestLine, "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_ZOMBOSS/ZOMBIE_ICEAGE_ZOMBOSS.PAM");
        setAttackClips("wind_4", "slingshot", "glacier_column_2");
        setAttackCooldown(5f);
    }

    @Override
    protected void executeAttack(String clipName, BaseGame game) {
        switch (clipName) {
            case "wind_4":
                attackWind(game);
                break;

            case "slingshot":
                attackSlingshot(game);
                break;

            case "glacier_column_2":
                attackGlacierColumn(game);
                break;
        }
    }

    // ====== ATTACK 1: WIND ======
    private void attackWind(BaseGame game) {
        int row1 = random.nextInt(5);
        int row2;
        do {
            row2 = random.nextInt(5);
        } while (row2 == row1);

        // محل خالی برای باد یخی
        // TODO: applyIceWind(row1, row2, game);
    }

    // ====== ATTACK 2: SLINGSHOT ======
    private void attackSlingshot(BaseGame game) {
        int targetRow = random.nextInt(5);
        int targetCol = random.nextInt(8) + 1;

        Plant targetPlant = game.getPlantAt(targetRow, targetCol);
        if (targetPlant != null) {
            targetPlant.setHp(0);
        }
    }

    // ====== ATTACK 3: GLACIER COLUMN ======
    private void attackGlacierColumn(BaseGame game) {
        int targetCol = random.nextInt(8) + 1;

        for (int row = 0; row < 5; row++) {
            Plant plant = game.getPlantAt(row, targetCol);
            if (plant != null) {
                plant.setHp(0);

                Zombie hunter = ZombieFactory.createZombie("hunter");
                hunter.setLine(row);
                hunter.setTileIndex(targetCol);
                hunter.setX(targetCol * Tile.getWidth() + Tile.getWidth() / 2);
                hunter.setY(row * Tile.getHeight() + Tile.getHeight() / 2);
                hunter.addEffect(new Effect(EffectType.FROZEN, 9999f));
                game.getZombies().add(hunter);
            }
        }
    }
}
