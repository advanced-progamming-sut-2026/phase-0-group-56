package models.factory.plantSkills.skillDatas;

import models.entity.Zombie;
import models.factory.plantSkills.Skill;
import models.games.BaseGame;
import models.entity.Plant;
import models.entity.Sun;
import models.entity.SunType;

import java.util.ArrayList;

public class SunProduce implements Skill {
    private SunProduceData data;

    public SunProduce(SunProduceData data) {
        this.data = data;
    }


    @Override
    public void do_skill(Plant producer, BaseGame game) {
        produce(producer , data, game);
    }

    @Override
    public ArrayList<Zombie> random(Plant plant, BaseGame game, int numbers) {
        System.out.println(plant.getType() + " has produced sun ..");
        return Skill.super.random(plant, game, numbers);
    }

    @Override
    public void all(Plant plant, BaseGame game) {

    }

    @Override
    public void setRandom(boolean random) {

    }

    @Override
    public void setAll(boolean all) {

    }

    @Override
    public void dispose(Plant self, BaseGame game) {
        Skill.super.dispose(self, game);
    }

    @Override
    public boolean disposable() {
        return Skill.super.disposable();
    }



    private void produce(Plant producer,SunProduceData data , BaseGame game) {
        /* Older plant definitions leave SunType unset. A generated sun is
           still a normal collectible in that case, rather than crashing the
           game while reading remainingTime from null. */
        SunType type = data.getSunType() == null ? SunType.NORMAL : data.getSunType();
        float spawnX = producer.getX() + Math.max(0f, producer.getWidth() - 50f) * 0.5f;
        float spawnY = producer.getY() + Math.max(0f, producer.getHeight());
        Sun sun = new Sun(data.amount , type.getRemainingTime() , spawnX, spawnY);
        /* Keep the model coordinates tied to the producer's cell so landing
           and world rendering agree even for rows other than zero. */
        sun.setLine(producer.getLine());
        sun.setTileIndex(producer.getTileIndex());
        sun.setX(spawnX);
        sun.setY(spawnY);
        sun.setGround(false);
        if (data.amount >= SunType.PREMIUM.getAmount()) {
            sun.setAnimationType(Sun.AnimationType.SPECIAL);
        }
        {
            game.getSuns().add(sun);
            sun.setProducer(producer);
        }
    }



    @Override
    public void update(){
        data.amount += 25;
    }


}
