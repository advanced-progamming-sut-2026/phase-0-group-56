package models.entity.ability;

import models.entity.ArmorType;
import models.entity.Zombie;
import models.entity.Plant;
import models.games.BaseGame;

import java.util.Random;

public class RandomChooserAbility implements Ability {

    public enum TargetType { ZOMBIE, PLANT }
    public enum ActionType { GIVE_ARMOR, TURN_TO_CAT }

    private final float range;
    private final float cooldown;
    private final TargetType targetType;
    private final ActionType actionType;
    private float timer;
    private final Random random = new Random();

    public RandomChooserAbility(float range, float cooldown, TargetType targetType, ActionType actionType) {
        this.range = range;
        this.cooldown = cooldown;
        this.targetType = targetType;
        this.actionType = actionType;
        this.timer = 0;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        timer -= deltaTime;
        if (timer > 0) return;

        Object target = null;
        if (targetType == TargetType.ZOMBIE) {
            target = game.getRandomZombieInRange(zombie, range);
        } else {
            target = game.getRandomPlantInRange(zombie, range);
        }

        if (target == null) {
            timer = cooldown;
            return;
        }

        switch (actionType) {
            case GIVE_ARMOR:
                if (target instanceof Zombie) {
                    Zombie t = (Zombie) target;
                    zombie.fire();
                    t.addArmor(ArmorType.CROWN.create());
                    t.addArmor(ArmorType.SHOULDER.create());
                }
                break;
            case TURN_TO_CAT:
                if (target instanceof Plant) {
                    Plant p = (Plant) target;
                    zombie.fire();
                    p.setCat(true);
                    game.addCat(zombie, p);
                }
                break;
        }

        timer = cooldown;
    }
}
