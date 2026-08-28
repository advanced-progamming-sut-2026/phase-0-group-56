package models.entity.zomboss;

import models.entity.*;
import models.games.BaseGame;
import models.gamepanes.Tile;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BeachZomboss extends Zomboss {

    private final Random random = new Random();
    private boolean invincible = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BeachZomboss(float x, float y, int lowestLine) {
        super(x, y, lowestLine, "768/FULL/ZOMBIE/ZOMBIE_BEACH_ZOMBOSS/ZOMBIE_BEACH_ZOMBOSS.PAM");
        setAttackClips("spawn", "suction_loop", "emerge");
        setStunClip("stun_loop");
        setAttackCooldown(5f);
    }

    @Override
    protected void executeAttack(String clipName, BaseGame game) {
        switch (clipName) {
            case "spawn":
                attackSpawn(game);
                break;

            case "suction_loop":
                attackSuction(game);
                break;

            case "emerge":
                attackEmerge(game);
                break;
        }
    }

    // ====== ATTACK 1: SPAWN ======
    private void attackSpawn(BaseGame game) {
        // TODO: 4 بچه کوسه بساز
    }

    // ====== ATTACK 2: SUCTION ======
    private void attackSuction(BaseGame game) {
        int bossRow1 = this.line;
        int bossRow2 = this.line + 1;
        float bossX = this.x;

        // گیاهان در دو ردیف
        for (Plant plant : game.getPlantsInField()) {
            if (plant.getLine() == bossRow1 || plant.getLine() == bossRow2) {
                plant.setVelocityX(500f); // کشیده شدن به سمت دهان
                // اگر به دهان رسید، نابود شود
            }
        }

        // زامبی‌ها در دو ردیف
        for (Zombie zombie : game.getZombies()) {
            if (zombie.getLine() == bossRow1 || zombie.getLine() == bossRow2) {
                zombie.setVelocityX(500f);
            }
        }
    }

    // ====== ATTACK 3: EMERGE ======
    private void attackEmerge(BaseGame game) {
        invincible = true;

        scheduler.schedule(() -> {
            invincible = false;
        }, 3, TimeUnit.SECONDS);
    }

    // ====== OVERRIDE TAKE DAMAGE ======
    @Override
    public void takeDamage(float damage) {
        if (invincible) {
            return;
        }
        super.takeDamage(damage);
    }

    @Override
    public void setMaxHp(float maxHp) {
        super.setMaxHp(maxHp);
    }
}
