package controllers.observer;

import models.entity.*;

public class TorchObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {
        if (!zombie.getType().toLowerCase().contains("explorer")) return;

        if (projectile.getTags() != null) {
            if (projectile.getTags().contains(Projectile.Tag.ICE)) {
                zombie.setTorchOn(false);
            } else if (projectile.getTags().contains(Projectile.Tag.FIRE)) {
                zombie.setTorchOn(true);
            }
        }
    }
}
