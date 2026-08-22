package models.entity.ability;

import models.entity.Zombie;
import models.games.BaseGame;
import models.factory.*;

public class SpawnAbility implements Ability {

    private final String spawnType;
    private final int count;
    private final float cooldown;
    private final float healthThreshold;
    private final boolean isDeadTriggered;
    private final boolean isReverse;
    private float timer;
    private boolean triggered;

    public SpawnAbility(String spawnType, int count, float healthThreshold) {
        this(spawnType, count, 0, healthThreshold, false, false);
    }

    public SpawnAbility(String spawnType, int count, boolean isDeadTriggered) {
        this(spawnType, count, 0, 0, isDeadTriggered, false);
    }

    public SpawnAbility(String spawnType, int count, boolean isReverse, boolean unused) {
        this(spawnType, count, 0, 0, false, isReverse);
    }

    private SpawnAbility(String spawnType, int count, float cooldown, float healthThreshold,
                         boolean isDeadTriggered, boolean isReverse) {
        this.spawnType = spawnType;
        this.count = count;
        this.cooldown = cooldown;
        this.healthThreshold = healthThreshold;
        this.isDeadTriggered = isDeadTriggered;
        this.isReverse = isReverse;
        this.timer = 0;
        this.triggered = false;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        // Gargantuar
        if (healthThreshold > 0) {
            if (triggered) return;
            if ((float) zombie.getHp() / zombie.getMaxHp() <= healthThreshold) {
                Zombie imp = ZombieFactory.createZombie("imp");
                imp.setLine(zombie.getLine());
                imp.setTileIndex(2);
                imp.setX(imp.getTileIndex() * 80 + 100);
                imp.setY(zombie.getY());
                game.getZombies().add(imp);
                zombie.fire();
                triggered = true;
            }
            return;
        }

        // Barrel
        if (isDeadTriggered) {
            if (triggered) return;
            if (zombie.isDead()) {
                game.spawn(zombie, spawnType, count);
                triggered = true;
            }
            return;
        }

        // Dynamite reverse
        if (isReverse) {
            if (triggered) return;
            ExplodeAbility explode = zombie.getAbility(ExplodeAbility.class);
            if (explode != null && explode.isTriggered()) {
                Zombie explodedDynamite = ZombieFactory.createZombie("normal");
                zombie.fire();
                explodedDynamite.setLine(zombie.getLine());
                explodedDynamite.setSpeed(explodedDynamite.getSpeed() * -1);
                game.getZombies().add(explodedDynamite);
                triggered = true;
            }
            return;
        }

        // Tomb Raiser (timer)
        timer += deltaTime;
        if (timer >= cooldown) {
            timer = 0;
            game.spawn(zombie, spawnType, count);
            zombie.fire();
        }
    }
}
