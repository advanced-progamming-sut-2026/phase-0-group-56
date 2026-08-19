package controllers.observer;

import models.entity.*;

public class DynamiteObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {
        if (!zombie.getType().toLowerCase().contains("prospector")) return;

        if (projectile.getTags() != null) {
            if (projectile.getTags().contains(Projectile.Tag.ICE)) {
                zombie.setDynamiteFrozen(true);
            } else if (projectile.getTags().contains(Projectile.Tag.FIRE)) {
                zombie.setDynamiteFrozen(false);
            }
        }
    }
}
