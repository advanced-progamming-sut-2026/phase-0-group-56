package models.entity.ability;

import models.entity.*;
import models.games.BaseGame;

public class MoveAbility implements Ability {

    public enum MoveType {
        PUSH_ARCADE,
        PUSH_ICE,
        PUSH_BARREL,
        PULL_PLANT,
        SWAP_ZOMBIE,
        PIANO
    }

    private final MoveType type;
    private final float cooldown;
    private float timer;
    private boolean isCarrying;

    public MoveAbility(MoveType type, float cooldown) {
        this.type = type;
        this.cooldown = cooldown;
        this.timer = 0;
        this.isCarrying = false;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        timer -= deltaTime;
        if (timer > 0) return;

        if (zombie.hasEffect(EffectType.HYPNOTIZED) && type == MoveType.PULL_PLANT) {
            Zombie target = game.getRandomZombieInRange(zombie, 4.0f);
            if (target != null && target != zombie) {
                game.pullZombie(zombie, target);
            }
            timer = cooldown;
            return;
        }

        switch (type) {
            case PULL_PLANT:
                handlePull(zombie, game);
                break;
            case SWAP_ZOMBIE:
                handleSwap(zombie, game);
                break;
        }

        timer = cooldown;
    }

    private void handlePull(Zombie zombie, BaseGame game) {
        Plant target = game.findPullablePlant(zombie);
        if (target != null) {
            game.pullPlant(zombie, target);
        }
    }

    private void handleSwap(Zombie zombie, BaseGame game) {
        Zombie target = game.getRandomZombie();
        if (target != null && target != zombie) {
            game.swapZombieToRow(target, zombie.getLine());
        }
    }

    public void pianoSwap(BaseGame game){
        Zombie target = game.getRandomZombie();
        if(target != null)
            target.changeLine();
    }

    public boolean isCarrying() { return isCarrying; }

    public MoveType getType() { return type; }
}
