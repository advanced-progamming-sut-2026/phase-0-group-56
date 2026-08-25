package models.entity.zomboss;

import models.entity.*;

public class IceAgeZomboss implements Zomboss{
    public IceAgeZomboss(float x , float y , int lowestLine){
        super(x , y , lowestLine , "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_ZOMBOSS/ZOMBIE_ICEAGE_ZOMBOSS.PAM");
        setAttackClips("wind_4" , "slingshot" , "glacier_column_2");
    }
}
