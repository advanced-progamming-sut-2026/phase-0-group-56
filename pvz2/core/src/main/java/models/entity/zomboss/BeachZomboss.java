package models.entity.zomboss;

import models.entity.*;

public class BeachZomboss implements Zomboss{
    public BeachZomboss(float x , float y , int lowestLine){
        super(x , y , lowestLine , "768/FULL/ZOMBIE/ZOMBIE_BEACH_ZOMBOSS/ZOMBIE_BEACH_ZOMBOSS.PAM");
        setAttackClips("spawn" , "suction_loop" , "emerge");
        setStunClip("stun_loop");
    }
}
