package models.entity;

import models.Constants;
import models.games.BaseGame;

public class LawnMower extends Entity {


    private boolean on = false;
    public String run(float delta , BaseGame game) {


      if(on){
          x += Constants.MOANER_SPEED * delta;
      }
        for (Zombie z : game.getZombies()) {
            if(Constants.overlap(z , this)){
                if(!on){
                    on = true;
                    return "Lawn LawnMower turned on at line " + line;
                }
                z.setHurt(true);
                z.setAlive(false);
                z.setHp(0);
            }
        }
        return null;
    }
    public LawnMower(int line){
        this.line = line;
        this.y = line * height;
    }

}
