package controllers.observer;

import models.entity.Zombie;
import models.entity.Projectile;

public class JugglerObserver implements BulletObserver {

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile) {
        zombie.setSpeed(zombie.getSpeed() * 5);
        System.out.println("5 times faster now");

        projectile.setVelocityX(-projectile.getVelocityX());
        projectile.setVelocityY(-projectile.getVelocityY());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        zombie.setSpeed(zombie.getSpeed() / 5);
        System.out.println("return to first speed");
    }
}
