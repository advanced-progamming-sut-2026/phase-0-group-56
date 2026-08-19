package controllers.observer;

import models.entity.Zombie;
import models.entity.Projectile;

public interface BulletObserver {
    void onBulletHit(Zombie zombie, Projectile projectile);
}
