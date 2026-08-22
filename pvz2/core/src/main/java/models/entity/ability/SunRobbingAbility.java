package models.entity.ability;

import models.entity.Zombie;
import models.games.BaseGame;

public class SunRobbingAbility implements Ability {

    private int stolenSun = 0;
    private final int maxStolenSun;
    private final float stealRate;
    private float timer;

    public SunRobbingAbility(int maxStolenSun, float stealRate) {
        this.maxStolenSun = maxStolenSun;
        this.stealRate = stealRate;
        this.timer = 0;
    }

    @Override
    public void execute(Zombie zombie, float deltaTime, BaseGame game) {
        if (stolenSun >= maxStolenSun) return;
        if (!zombie.isNearHouse()) return;

        timer += deltaTime;
        if (timer >= 1.0f) {
            timer = 0;
            int amount = (int) stealRate;
            if (stolenSun + amount > maxStolenSun) {
                amount = maxStolenSun - stolenSun;
            }
            stolenSun += amount;
            game.removeSun(amount);
            zombie.extra();
        }
    }

    public int getStolenSun() { return stolenSun; }
    public void releaseStolenSun(BaseGame game) {
        if (stolenSun > 0) {
            game.addSun(stolenSun / 2);
            stolenSun = 0;
        }
    }
}
