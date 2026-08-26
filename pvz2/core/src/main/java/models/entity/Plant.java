package models.entity;

import models.App;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import models.factory.builder.PlantLevel;
import models.factory.plantSkills.Explosive;
import models.factory.plantSkills.Skill;
import models.factory.plantSkills.skillDatas.ExplosionData;
import models.factory.plantSkills.skillDatas.PlantArmor;
import models.factory.plantSkills.skillobserver.Observer;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.ArrayList;

public class Plant extends Entity {
    protected float damage;
    protected int cost;
    protected float ActionInterval;
    public float t;
    protected PlantCategory category;
    protected PlantType type;
    protected ArrayList<PlantTags> tags;
    protected ArrayList<Skill> baseSkill = new ArrayList<>();
    protected ArrayList<Skill> plantfoodSkill = new ArrayList<>();
    protected boolean frozen = false;
    protected boolean cat = false;
    public boolean onLilyPad = false;
    protected float lifeTime = -5;
    protected int freezeLevel = 0;
    protected ArrayList<PlantArmor> armor =  new ArrayList<>();
    protected Observer skillObserver;

    /*
     * Presentation events are deliberately represented as monotonically
     * increasing counters instead of putting any LibGDX/PAM code in the
     * model.  PlantRenderer observes these counters and starts the matching
     * one-shot clip exactly once for each gameplay action.
     */
    private long animationActionVersion;
    private long animationPlantFoodVersion;
    private long animationDamageVersion;
    private float animationActionAt = Float.NEGATIVE_INFINITY;
    private float animationPlantFoodAt = Float.NEGATIVE_INFINITY;
    private float animationDamageAt = Float.NEGATIVE_INFINITY;

    public ArrayList<PlantArmor> getArmor() {
        return armor;
    }

    public void setArmor(ArrayList<PlantArmor> armor) {
        this.armor = armor;
    }

    public Plant(float actionInterval) {
        ActionInterval = actionInterval;
    }
    public Plant(){

    }

    public void boost(){}

    public PlantCategory getCategory() {
        return category;
    }

    public ArrayList<PlantTags> getTags() {
        return tags;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }


    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setCategory(PlantCategory category) {
        this.category = category;
    }

    public void setTags(ArrayList<PlantTags> tags) {
        this.tags = tags;
    }

    public ArrayList<Skill> getBaseSkill() {
        return baseSkill;
    }

    public void setBaseSkill(ArrayList<Skill> baseSkill) {
        this.baseSkill = baseSkill;
    }

    public float getActionInterval() {
        return ActionInterval;
    }

    public float getT() {
        return t;
    }

    public void setT(float t) {
        this.t = t;
    }

    public PlantType getType() {
        return type;
    }

    public void setType(PlantType type) {
        this.type = type;
    }

    public float getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(float lifeTime) {
        this.lifeTime = lifeTime;
    }

    public ArrayList<Skill> getPlantfoodSkill() {
        return plantfoodSkill;
    }

    public void setPlantfoodSkill(ArrayList<Skill> plantfoodSkill) {
        this.plantfoodSkill = plantfoodSkill;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isCat() {
        return cat;
    }

    public void setCat(boolean cat) {
        this.cat = cat;
    }

    public void update(float delta , BaseGame game){
        stateTime += delta;
        if (tags != null && tags.contains(PlantTags.Fire)) {
            game.meltFrozenZombiesNear(this, 1.5f);
        }
        if(tags.contains(PlantTags.Day) && !game.isDay()){
            System.out.println("This plant is for the day.");
            return;
        }
        else if(tags.contains(PlantTags.Night) && game.isDay()){
            System.out.println("This plant is for the night.");
            return;
        }
        if(freezeLevel >= 3 || cat || frozen) {
            heat(game ,delta);
            return;
        }
        if(t <= 0){
            t = ActionInterval;
            if(Trap(game)) {
                boolean shouldAct = skillObserver == null
                    || skillObserver.observe(this, game);
                if (shouldAct) {
                    animationActionVersion++;
                    animationActionAt = stateTime;
                    for (Skill skill : baseSkill) {
                        skill.do_skill(this ,game);
                    }
                }
                if(tags.contains(PlantTags.ONCE_USAGE)){
                    dispose(game);
                }
            }

        }
        else{
            t -= delta;
        }

        if(plantFood){
            animationPlantFoodVersion++;
            animationPlantFoodAt = stateTime;
            for (Skill x : plantfoodSkill) x.do_skill(this , game);
            plantFood = false;
        }

        if(lifeTime <= 0 && lifeTime >= -1){
            dispose(game);
        }
        else if(lifeTime > 0) lifeTime -= delta;
    }

    private boolean Trap(BaseGame game){
        if(!this.tags.contains(PlantTags.Trap)){
            return true;
        }

        for (Zombie x : game.getZombies()) {
            if(Math.abs(x.getX() - this.x) < 20){
                return true;
            }
        }
        return false;
    }

    public void dispose(BaseGame game){
        if(type == PlantType.LILY_PAD){
            Tile tile = game.getField().getTileByCoordinats(tileIndex , line);
            tile.setPlantable(false);
        }
        if(tags.contains(PlantTags.EXPLOSIVE)){
            ExplosionData data = new ExplosionData( 3 ,3);
            new Explosive(data).do_skill(this , game);
        }
        //game.getPlantsInField().remove(this);

    }

    public void setPlantFood(boolean plantFood , BaseGame game) {
        if (plantFood) {
            animationPlantFoodVersion++;
            animationPlantFoodAt = stateTime;
        }
        for (Skill x : plantfoodSkill){
            x.do_skill(this , game);
        }
    }
    boolean plantFood;
    public void setPlantFood(boolean plantFood) {
        this.plantFood = plantFood;
    }

    /** Number of completed base-ability activations for visual consumers. */
    public long getAnimationActionVersion() {
        return animationActionVersion;
    }

    /** Number of consumed Plant Food activations for visual consumers. */
    public long getAnimationPlantFoodVersion() {
        return animationPlantFoodVersion;
    }

    /** Number of damage events received by this plant for visual consumers. */
    public long getAnimationDamageVersion() {
        return animationDamageVersion;
    }

    public float getAnimationActionAt() {
        return animationActionAt;
    }

    public float getAnimationPlantFoodAt() {
        return animationPlantFoodAt;
    }

    public float getAnimationDamageAt() {
        return animationDamageAt;
    }


    public int getFreezeLevel() {
        return freezeLevel;
    }

    public void setFreezeLevel(int freezeLevel) {
        if(freezeLevel >= 3){
            freezeLevel = 3;
            frozen = true;
            if(freezeHp <= 0) freezeHp = 700;
        }
        this.freezeLevel = freezeLevel;
    }

    public void setHp(float hp , Zombie eater , BaseGame game){
        if (hp < this.hp && hp > 0f) {
            animationDamageVersion++;
            animationDamageAt = stateTime;
        }
        this.hp = hp;
        if(hp <= 0){
            dispose(eater , game);
        }
    }
    public void setHP(float hp){
        this.hp = hp;
    }

    private void dispose(Zombie eater , BaseGame game){
        if(tags.contains(PlantTags.Shroom) && tags.contains(PlantTags.MAGICAL)){
            eater.setHypnotized(true);
            if(plantFood){
                game.getZombies().remove(eater);
                Zombie zombie = ZombieFactory.createZombie("Gargantur");
                zombie.setHypnotized(true);
                game.getZombies().add(zombie);
            }
        }
        else if(tags.contains(PlantTags.EXPLOSIVE)){
            if (PlantLevel.current(this.type) >= 3) damage += 200;
            ExplosionData data = new ExplosionData(3 , 3);
            Explosive boom = new Explosive(data);
            boom.do_skill(this ,game );
        }
    }

    float freezeHp;

    public void setFreezeHp(float freezeHp) {
        if(freezeHp <= 0){
            frozen = false;
            freezeLevel = 0;
        }
        this.freezeHp = freezeHp;
    }

    private void heat(BaseGame game , float delta){
        for (Plant x : game.getPlantsInField()){
            float dx =  Math.abs(x.getX() - this.x);
            float dy = Math.abs(x.getY() - this.y);
            if(dx <= Tile.getWidth() * 1 && dy  <= Tile.getHeight() * 1){
                setFreezeHp(freezeHp - 60 * delta);
            }
        }
    }

    public void setActionInterval(float actionInterval) {
        ActionInterval = actionInterval;
    }

    public void setSkillObserver(Observer skillObserver) {
        this.skillObserver = skillObserver;
    }
}
