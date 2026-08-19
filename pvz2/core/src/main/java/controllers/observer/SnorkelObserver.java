package controllers.observer;
import models.entity.Zombie;
import models.entity.Projectile;

public class SnorkelObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {

        boolean underWater = zombie.isInWater();
        if(!underWater)
            return;

        if (!projectile.isGrounded()) {
            projectile.setProved(true);
            projectile.setActive(false);
            System.out.println("just lobber can hit snorkel under water");
        }
    }
}
