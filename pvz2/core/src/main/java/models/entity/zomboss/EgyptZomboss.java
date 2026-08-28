package models.entity.zomboss;

import models.entity.*;
import models.games.BaseGame;
import models.factory.ZombieFactory;
import models.gamepanes.Tile;

import java.util.Random;

public class EgyptZomboss extends Zomboss {

    private final Random random = new Random();
    private float stompStartX;
    private boolean stompReturning = false;
    private boolean stompActive = false;
    private float stompTimer = 0f;
    private float stompDuration = 0f;

    public EgyptZomboss(float x, float y, int lowestLine) {
        super(x, y, lowestLine, "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM");
        setAttackClips("stomp", "zombie_portal_loop", "rocket_launch");
        setDieClip("die_idle");
        setStunClip("stun_loop");
        setAttackCooldown(5f);
    }

    @Override
    protected void executeAttack(String clipName, BaseGame game) {
        switch (clipName) {
            case "stomp":
                attackStomp(game);
                break;

            case "zombie_portal_loop":
                attackPortal(game);
                break;

            case "rocket_launch":
                attackRocket(game);
                break;
        }
    }

    // ====== ATTACK 1: STOMP ======
    private void attackStomp(BaseGame game) {
        stompActive = true;
        stompStartX = this.x;
        stompReturning = false;
        stompTimer = 0f;

        // مدت زمان انیمیشن (از PAM گرفته می‌شود)
        stompDuration = 1.0f; // placeholder
    }

    private void updateStomp(float delta, BaseGame game) {
        if (!stompActive) return;

        stompTimer += delta;

        if (!stompReturning) {
            // حرکت به چپ با سرعت بالا
            this.x -= 200f * delta;

            // برخورد با گیاهان
            for (Plant plant : game.getPlantsInField()) {
                if (plant.getLine() == this.line || plant.getLine() == this.line + 1) {
                    if (Math.abs(plant.getX() - this.x) < 50) {
                        plant.setHp(0);
                    }
                }
            }

            // نصف مدت زمان انیمیشن گذشته، برگرد
            if (stompTimer >= stompDuration * 0.5f) {
                stompReturning = true;
            }
        } else {
            // برگشت به جای اول با سرعت بسیار بالا
            this.x += 1000f * delta;

            if (this.x >= stompStartX) {
                this.x = stompStartX;
                stompActive = false;
            }
        }
    }

    // ====== ATTACK 2: PORTAL ======
    private void attackPortal(BaseGame game) {
        // هر 0.1 ثانیه یک زامبی brick
        float duration = 2.0f; // مدت زمان انیمیشن
        int count = (int)(duration / 0.1f);

        for (int i = 0; i < count; i++) {
            int row = random.nextInt(5);
            int col = random.nextInt(8) + 1;

            Zombie brick = ZombieFactory.createZombie("brick");
            brick.setLine(row);
            brick.setTileIndex(col);
            brick.setX(col * Tile.getWidth() + Tile.getWidth() / 2);
            brick.setY(row * Tile.getHeight() + Tile.getHeight() / 2);
            game.getZombies().add(brick);
        }
    }

    // ====== ATTACK 3: ROCKET ======
    private void attackRocket(BaseGame game) {
        int targetRow = random.nextInt(5);
        int targetCol = random.nextInt(8) + 1;

        Plant targetPlant = game.getPlantAt(targetRow, targetCol);
        if (targetPlant != null) {
            targetPlant.setHp(0);
        }

        // مانع مصر در دو خانه تصادفی دیگر (به جز خانه هدف)
        for (int i = 0; i < 2; i++) {
            int row;
            int col;
            do {
                row = random.nextInt(5);
                col = random.nextInt(8) + 1;
            } while (row == targetRow && col == targetCol);

            // TODO: قرار دادن مانع مصر
            game.spawnGrave(row, col);
        }
    }

    // ====== CALL UPDATE STOMP ======
    public void update(float delta, BaseGame game, PamPlayer player) {
        super.update(delta, game, player);

        if (stompActive) {
            updateStomp(delta, game);
        }
    }
}
