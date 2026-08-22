package models.entity.ability;

import models.entity.*;
import models.entity.EffectType;
import models.games.BaseGame;

public class BulletAbility implements Ability {

    private final ProjectileType projectileType;
    private final float cooldown;
    private final float range;
    private float timer;

    public BulletAbility(ProjectileType projectileType, float cooldown, float range) {
        this.projectileType = projectileType;
        this.cooldown = cooldown;
        this.range = range;
        this.timer = 0;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        timer -= deltaTime;
        if (timer > 0) return;

        boolean isHypnotized = zombie.hasEffect(EffectType.HYPNOTIZED);

        if (isHypnotized) {
            Zombie target = game.findNearestZombie(zombie, range);
            if (target == null) return;
            Projectile projectile = new Projectile(zombie.getX(), zombie.getY(), projectileType,zombie.getLine());
            projectile.setToLockIn(target);
            zombie.fire();
            game.getBullets().add(projectile);
        } else {
            Plant target = game.findTargetPlant(zombie, range);
            if (target == null) return;
            Projectile projectile = new Projectile(zombie.getX(), zombie.getY(), projectileType
            , zombie.getLine());
            projectile.setToLockIn(null);
            zombie.fire();
            game.getBullets().add(projectile);
        }

        timer = cooldown;
    }
}
