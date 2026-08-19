package controllers.observer;

import models.entity.Projectile;
import models.entity.Zombie;

public class DragonObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {
        if (projectile.getTags() != null && projectile.getTags().contains(Projectile.Tag.FIRE)) {
            projectile.setProved(true);
        }
    }
}
