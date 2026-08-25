package models.entity.zomboss;

import models.entity.*;

public class EgyptZomboss implements Zomboss{
    public EgyptZomboss(float x , float y , int lowestLine){
        super(x , y , lowestLine , "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM");
        setAttackClips("stomp" , "zombie_portal_loop" , "rocket_launch");
        setDieClip("die_idle");
        setStunClip("stun_loop");
    }
}
