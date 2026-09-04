package models.factory.builder;

import controllers.datacontroller.Data;
import controllers.datacontroller.SeedPackage;
import controllers.datacontroller.Upgrade;
import models.App;
import models.entity.Plant;
import models.entity.PlantTags;
import models.entity.WrampUpPlant;
import models.gamepanes.Tile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantBuilder {
    private final String hp = "$HP\\s+(?<amount>-?\\d+)^";
    private final String cost = "$Cost\\s+(?<amount>-?\\d+)^";
    private final String damage = "$Dmg\\s+(?<amount>-?\\d+)^";
    private final String cooldown = "$Cooldown\\s+(?<amount>-?\\d+)^";
    private final String lifeSpan = "$Lifespan\\s+(?<>-?\\d+)\\s*s^";
    private final String Speed = "$Speed\\s+(?<amount>-?\\d+)^";
    private  Plant plant = new Plant();


    public Plant build(PlantType plantType){
        plant = new Plant();
        plant.setWidth(Tile.getWidth());
        plant.setHeight(Tile.getHeight());
        plant.setType(plantType);
        plant.setCategory(plantType.getCategory());
        int level = PlantLevel.current(plantType);
        plant.setHp(upgradedHP(plantType , level));
        plant.setDamage(upgradedDamage(plantType , level));
        plant.setActionInterval(upgradedSpeed(plantType , level));
        ArrayList<PlantTags> loadedTags = Data.getPlants().get(plantType).getTags();
        plant.setTags(loadedTags == null ? new ArrayList<>() : new ArrayList<>(loadedTags));
        explodeOnFinish();
        plantType.allocateSkill(plant);
        if(plantType == PlantType.SEA_SHROOM ||
            plantType == PlantType.PUFF_SHROOM){
            plant.setLifeTime(60);
        }
        if(plant.getTags().contains(PlantTags.Wramp_up)){
            plant = new WrampUpPlant(plant);
        }
        return plant;
    }




    private void upgradeEffect(){
        int level = PlantLevel.current(plant.getType());
        plant.setHp(upgradedHP(plant.getType() , level));
        plant.setDamage(upgradedDamage(plant.getType() , level));


        explodeOnFinish();

    }

    private void explodeOnFinish(){
        for (int i = 2; i <= PlantLevel.current(plant.getType()); i++) {
            if(Data.getPlants().get(plant.getType()).getUpgrades().get(i - 2)
                .getEffect().matches("Explode on Finish")){
                plant.getTags().add(PlantTags.EXPLOSIVE);
            }
        }
    }

    private void plantFoodFromStart(){
        for (int i = 2; i <= PlantLevel.current(plant.getType()); i++) {
            if(Data.getPlants().get(plant.getType()).getUpgrades().get(i - 2)
                .getEffect().matches("Plant Food From Start")){
                plant.setPlantFood(true);
            }
        }
    }

    private float hp(String effect){
        Pattern pattern = Pattern.compile(hp);
        Matcher matcher = pattern.matcher(effect);
        matcher.find();
        return Float.parseFloat(matcher.group(1));
    }

    private float cost(String effect){
        Pattern pattern = Pattern.compile(cost);
        Matcher matcher = pattern.matcher(effect);
        matcher.find();
        return Float.parseFloat(matcher.group(1));
    }
    private float damage(String effect){
        Pattern pattern = Pattern.compile(damage);
        Matcher matcher = pattern.matcher(effect);
        matcher.find();
        return Float.parseFloat(matcher.group(1));
    }
    private float cooldown(String effect){
        Pattern pattern = Pattern.compile(cooldown);
        Matcher matcher = pattern.matcher(effect);
        matcher.find();
        return Float.parseFloat(matcher.group(1));
    }

    public float upgradedHP(PlantType plantType , int level){
        float hp = Data.getPlants().get(plantType).getHp();
        for (int i = 2; i <= level ; i++) {
            String effect = Data.getPlants().get(plantType).getUpgrades().get(i).getEffect();
            if(effect.matches(this.hp)){
                hp += hp(effect);
            }
        }
        System.out.println("upgradedHP: " + hp);
        return hp;
    }

    public float  upgradedCost(PlantType plantType , int level){
        float cost = Data.getPlants().get(plantType).getCost();
        for (int i = 2; i <= level ; i++) {
            String effect = Data.getPlants().get(plantType).getUpgrades().get(i).getEffect();
            cost += effect.matches(this.cost) ?  cost(effect) : 0;
        }
        return cost;
    }

    public float upgradedCooldown(PlantType plantType , int level){
        float cooldown = Data.getPlants().get(plantType).getRecharge();
        for (int i = 2; i <= level ; i++) {
            String effect = Data.getPlants().get(plantType).getUpgrades().get(i).getEffect();
            cooldown += effect.matches(this.cooldown) ?  cooldown(effect) : 0;
        }
        return cooldown;
    }

    public float upgradedDamage(PlantType plantType , int level){
        float damage = Data.getPlants().get(plantType).getDamage();
        for (int i = 2; i <= level ; i++) {
            String effect = Data.getPlants().get(plantType).getUpgrades().get(i).getEffect();
            damage += effect.matches(this.damage) ? damage(effect) : 0;
        }
        return damage;
    }

    public float upgradedSpeed(PlantType plantType , int level){
        float cooldown = Data.getPlants().get(plantType).getActionInterval();
        for (int i = 2; i <= level ; i++) {
            String effect = Data.getPlants().get(plantType).getUpgrades().get(i).getEffect();
            cooldown += effect.matches(this.cooldown) ?  speed(effect) : 0;
        }
        return cooldown;
    }

    private float speed(String effect){
        Matcher m = Pattern.compile(Speed).matcher(effect);
        m.find();
        return Float.parseFloat(m.group(1));
    }
}
