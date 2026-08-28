package models.entity.zomboss;

import models.entity.*;
import models.games.BaseGame;
import models.factory.ZombieFactory;

import java.util.Random;

public class DarkZomboss extends Zomboss {

    private final Random random = new Random();

    public DarkZomboss(float x, float y, int lowestLine) {
        super(x, y, lowestLine, "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM");
        setAttackClips("fire_bomb", "summoning", "fire_attack_idle");
        setStunClip("stun_loop");
        setAttackCooldown(5f);
    }

    @Override
    protected void executeAttack(String clipName, BaseGame game) {
        switch (clipName) {
            case "fire_bomb":
                attackFireBomb(game);
                break;

            case "summoning":
                attackSummoning(game);
                break;

            case "fire_attack_ability":
                attackFireRows(game);
                break;
        }
    }

    // ====== ATTACK 1: FIRE BOMB ======
    private void attackFireBomb(BaseGame game) {
        int targetRow = random.nextInt(5);
        int targetCol = random.nextInt(8) + 1;

        Plant targetPlant = game.getPlantAt(targetRow, targetCol);
        if (targetPlant != null) {
            targetPlant.setHp(0);
        }

        spawnDragonImp(targetRow, targetCol, game);
    }

    // ====== ATTACK 2: SUMMONING =====
    private void attackSummoning(BaseGame game) {
        int targetRow = random.nextInt(5);
        int targetCol = random.nextInt(8) + 1;

        Plant targetPlant = game.getPlantAt(targetRow, targetCol);
        if (targetPlant != null) {
            targetPlant.setHp(0);
        }

        spawnDragonImp(targetRow, targetCol, game);
    }

    // ====== ATTACK 3: FIRE ROWS ======
    private void attackFireRows(BaseGame game) {
        int row1 = this.line;
        int row2 = this.line + 1;

        for (int col = 0; col < 9; col++) {
            Plant plant1 = game.getPlantAt(row1, col);
            if (plant1 != null) {
                plant1.setHp(0);
            }

            Plant plant2 = game.getPlantAt(row2, col);
            if (plant2 != null) {
                plant2.setHp(0);
            }
        }

        spawnDragonImp(row1, 5, game);
        spawnDragonImp(row1, 5, game);
        spawnDragonImp(row2, 4, game);
        spawnDragonImp(row2, 4, game);
    }

    // ====== HELPER: SPAWN DRAGON IMP ======
    private void spawnDragonImp(int row, int col, BaseGame game) {
        Zombie imp = ZombieFactory.createZombie("dragon_imp");
        imp.setLine(row);
        imp.setTileIndex(col);
        game.getZombies().add(imp);
    }
}
