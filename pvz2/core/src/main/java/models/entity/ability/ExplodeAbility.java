package models.entity.ability;

import models.entity.Zombie;
import models.entity.EffectType;
import models.games.BaseGame;

public class ExplodeAbility implements Ability {

    private final float range;
    private final int damage;
    private final float cooldown;
    private float timer;
    private boolean triggered = false;
    private Condition condition;

    public enum Condition {
        NONE,
        IS_CARRYING,
        TORCH_ON,
        NOT_FROZEN
    }

    public ExplodeAbility(float range, int damage, float cooldown) {
        this(range, damage, cooldown, Condition.NONE);
    }

    public ExplodeAbility(float range, int damage, float cooldown, Condition condition) {
        this.range = range;
        this.damage = damage;
        this.cooldown = cooldown;
        this.condition = condition;
        this.timer = 0;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        if (triggered) return;
        if (zombie.isDead()) return;

        // شرط
        if (!checkCondition(zombie)) return;

        timer += deltaTime;
        if (timer >= cooldown) {
            triggered = true;

            if (zombie.hasEffect(EffectType.HYPNOTIZED)) {
                game.explodeAreaOnZombies(zombie.getLine(), zombie.getX(), range, damage);
                zombie.fire();
            } else {
                game.explodeArea(zombie.getLine(), zombie.getX(), range, damage);
                zombie.fire();
            }
        }
    }

    private boolean checkCondition(Zombie zombie) {
        switch (condition) {
            case IS_CARRYING:
                MoveAbility move = zombie.getAbility(MoveAbility.class);
                return move != null && move.isCarrying();
            case TORCH_ON:
                return zombie.isTorchOn();
            case NOT_FROZEN:
                return !zombie.isDynamiteFrozen();
            case NONE:
            default:
                return true;
        }
    }

    public boolean isTriggered() { return triggered; }
    public void reset() { triggered = false; timer = 0; }
}
