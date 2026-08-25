package models.entity.zomboss;

import models.entity.*;

public class DarkZomboss implements Zomboss{
    public DarkZomboss(float x , float y , int lowestLine){
        super(x , y , lowestLine , "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM");
        setAttackClips("fire_bomb" , "summoning" , "fire_attack_ability");
    }
}
