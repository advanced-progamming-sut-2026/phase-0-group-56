package models.factory.plantSkills;

import models.App;
import models.Constants;
import models.entity.PlantTags;
import models.entity.Sun;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.factory.builder.PlantLevel;
import models.games.BaseGame;
import models.entity.Plant;

public class Block implements Skill{
    public boolean in;
    public boolean damage = false;
    public Block(boolean in) {
        this.in = in;
    }
    public Block(){

    }
    @Override
    public void do_skill(Plant plant, BaseGame game) {
        System.out.println(plant.getType() + " is blocking ..");
            if(all) all(plant , game);
            else if(plant.getTags().contains(PlantTags.MOVE_ZOMBIES)){
                if(in) attract(plant, game);
            }
            else if(plant.getHeight() >= Constants.TALL_WALL_NUT_HEIGHT){
                tall(plant, game);
            }
            damaged_action(plant, game);
    }

    @Override
    public void all(Plant plant, BaseGame game) {
            if(in){

            }
            else{

            }
    }

    @Override
    public void setRandom(boolean random) {

    }

    boolean all = false;
    @Override
    public void setAll(boolean all) {
            this.all = all;
    }

    private void tall(Plant self , BaseGame game) {
        for (Zombie zombie: game.getZombies()) {
            if(zombie.getLine() == self.getLine() && Math.abs(zombie.getX() - self.getX()) < 6 &&
            !zombie.isGround()) {
                zombie.setGround(true);
            }
        }
    }


    private void damaged_action(Plant self, BaseGame game) {
        for (Zombie zombie: game.getZombies()) {
            if(zombie.getX() - self.getX() < 10 && self.isHurt()) {
               if(damage) {
                   float extraDamage = self.getArmor() != null ? Constants.ENDURIAN_ARMOR_DAMAGE : 0;
                   if (PlantLevel.current(self.getType()) >= 2) extraDamage += 5;
                   zombie.setHp(zombie.getHp() - self.getDamage() - extraDamage);
               }
               else if(self.getTags().contains(PlantTags.SUN)) {
                   int level = PlantLevel.current(self.getType());
                   game.getSuns().add(new Sun(level >= 2 ? 10 : 5 , 5 , self.getX() + self.getWidth(),
                           self.getY()));
               }
            }
        }
    }



    private void attract(Plant self, BaseGame game) {
        for (Zombie zombie: game.getZombies()) {
            if(Math.abs(zombie.getLine() - self.getLine()) <= 1){
                float dy = self.getLine() - zombie.getLine() ;
                zombie.setLine(self.getLine());
                zombie.setY(zombie.getY() + dy * Tile.getHeight());
            }
        }
    }

    @Override
    public boolean disposable() {
        return true;
    }

    @Override
    public void dispose(Plant self, BaseGame game) {
        if(self.getTags().contains(PlantTags.MOVE_ZOMBIES) && !in){
            int i = 0;
            for (Zombie zombie: game.getZombies()) {
                if(zombie.getX() - self.getX() < 20) {
                    int newline = zombie.getLine() + (i % 2 == 0 ? -1 : 1);
                    i++;
                    if(newline < 1) newline = 2;
                    else if(newline > 5) newline = 4;
                    zombie.setLine(newline);
                    float dy = Tile.getHeight() * (newline - zombie.getLine());
                    zombie.setY(zombie.getY() + dy);
                }
            }
        }
    }


}
