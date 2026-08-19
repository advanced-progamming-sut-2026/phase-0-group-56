package controllers.observer;

import models.entity.Projectile;
import models.entity.Zombie;
import models.gamepanes.*;

public class PassThroughObserver implements BulletObserver{

    @Override
    public void onBulletHit(Zombie zombie, Projectile projectile){
        // just for asani structure
        return;
    }

    public boolean canPassThrough(Zombie zombie, GridItem item) {
        // Dodo Rider ignores obstacles
        return zombie != null &&
                zombie.getType() != null &&
                zombie.getType().toLowerCase().contains("dodo");
    }
}
