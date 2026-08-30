package models.entity;

import models.factory.plantSkills.Skill;
import models.games.BaseGame;

public class WrampUpPlant extends Plant{
    private int level = 1;
    public WrampUpPlant(Plant plant){
        this.x = plant.getX();
        this.y = plant.getY();
        this.width = plant.getWidth();
        this.height = plant.getHeight();
        this.velocityX = plant.getVelocityX();
        this.velocityY = plant.getVelocityY();
        this.line = plant.getLine();
        this.tileIndex = plant.getTileIndex();
        this.hp = plant.getHp();
        this.stateTime = plant.getStateTime();
        this.isAlive = plant.isAlive();
        this.ground = plant.isGround();
        this.hurt = plant.isHurt();
        this.damage = plant.getDamage();
        this.cost = plant.getCost();
        this.ActionInterval = plant.getActionInterval();
        this.t = plant.getT();
        this.category = plant.getCategory();
        this.tags = plant.getTags();
        this.type =  plant.getType();
        this.baseSkill = plant.getBaseSkill();
        this.plantfoodSkill = plant.getPlantfoodSkill();
        this.frozen = plant.isFrozen();
        this.cat = plant.isCat();
        this.onLilyPad = plant.onLilyPad;
        this.lifeTime = plant.getLifeTime();
        this.freezeLevel = plant.getFreezeLevel();
        this.armor = plant.getArmor();
        this.skillObserver = plant.skillObserver;




    }
    private float updateStageTimer = 24f;
    private void grow(float delta){
        if(level >= 3) return;
        if(updateStageTimer <= 0){
            level += 1;
            updateStageTimer = 24 * level;
            for (Skill skill : getBaseSkill()) skill.update();
        }
        else updateStageTimer -= delta;
    }

    @Override
    public void update(float delta, BaseGame game) {
        grow(delta);
        super.update(delta, game);
    }
}
