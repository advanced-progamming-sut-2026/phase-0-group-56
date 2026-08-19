package controllers.observer;

import models.entity.Zombie;
import models.entity.Projectile;

public class ParasolObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {
        if (projectile.isGrounded()) {
            projectile.setProved(true);
            projectile.setActive(false);
            System.out.println("no lobber can hit parasol :)");
        }
    }
}
