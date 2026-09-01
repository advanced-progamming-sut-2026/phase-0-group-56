package models.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import models.App;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.entity.ability.*;
import controllers.observer.*;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

import java.util.*;

public class Zombie extends Entity{

    // ====== CORE ======
    private final String id;
    private final String type;
    private final int maxHp;
    private int damage;
    private int cost;
    private float speed;
    private int tileIndex;
    private Tile currentTile = null;
    private boolean dead;
    private RewardDrop.Type rewardDropType;
    private int rewardDropAmount;
    private boolean rewardDropCreated;
    private boolean frozen;
    private boolean hypnotized;
    private boolean inWater;
    private float eatCooldown;
    private float eatTimer = 0;

    // ====== ABILITIES ======
    private final List<Ability> abilities = new ArrayList<>();

    // ====== EFFECTS ======
    private final List<Effect> effects = new ArrayList<>();

    // ====== EQUIPMENT ======
    private final List<Armor> armors = new ArrayList<>();

    // ====== OBSERVERS ======
    private final List<BulletObserver> bulletObservers = new ArrayList<>();
    private AllStarObserver allStarObserver;
    private NewspaperObserver newspaperObserver;
    private PassThroughObserver passThroughObserver;

    // ====== STATE FLAGS ======
    private boolean isTorchOn = false;
    private boolean isDynamiteFrozen = false;
    private boolean encasedInIce;
    private float slipperyCooldown;

    // ====== ANIMATION HANDLING FIELDS ======
    private ZombieState currentState = ZombieState.IDLE;
    private float stateTime = 0f;
    private String pamPath;

    private String idleClip = "idle";
    private String walkClip = "walk";
    private String eatClip = "eat";
    private String fireClip = null;
    private String extraClip = null;
    private String dieClip = "die";

    private boolean isFiringRequested = false;
    private boolean isExtraRequested = false;
    /** Presentation timer that keeps one-shot ability clips visible. */
    private float actionTimeRemaining = 0f;
    private Plant targetPlant = null;

    private final Map<String, Boolean> visibilityMap = new HashMap<>();


    // ====== CONSTRUCTOR ======
    public Zombie(String id, String type, int line, int hp, int damage,  float speed, int cost, int width, int height) {
        this.id = id;
        this.type = type;
        this.hp = hp;
        this.line = line;
        this.maxHp = hp;
        this.damage = damage;
        this.speed = speed;
        this.cost = cost;
        this.width = width;
        this.height = height;
        this.eatCooldown = calculateEatCooldown();
        this.dead = false;
        this.frozen = false;
        this.hypnotized = false;
        initObservers();
    }

    // ====== INIT ======
    private float calculateEatCooldown() {
        if (id != null && id.toLowerCase().contains("imp")) return 0.5f;
        if (type != null && type.toLowerCase().contains("gargantuar")) return 2.0f;
        if (type != null && type.toLowerCase().contains("allstar")) return 1.0f;
        return speed > 0.25 ? 0.7f : 1.0f;
    }

    private void initObservers() {
        if (type != null && type.toLowerCase().contains("allstar")) {
            allStarObserver = new AllStarObserver();
        }
        if (type != null && type.toLowerCase().contains("newspaper")) {
            newspaperObserver = new NewspaperObserver();
        }
        if (type != null && type.toLowerCase().contains("dodo")) {
            passThroughObserver = new PassThroughObserver();
        }
    }

    // ====== BULLET OBSERVERS ======
    public void addBulletObserver(BulletObserver observer) {
        bulletObservers.add(observer);
    }

    public void removeBulletObserver(BulletObserver observer) {
        bulletObservers.remove(observer);
    }

    public List<BulletObserver> getBulletObservers() {
        return Collections.unmodifiableList(bulletObservers);
    }

    public void notifyBulletObservers(Projectile bullet) {
        for (BulletObserver observer : bulletObservers) {
            observer.onBulletHit(this, bullet);
            if (!bullet.isActive()) {
                break;
            }
        }
    }

    // ====== UPDATE ======
    public void update(float deltaTime, BaseGame game) {
        if (dead){
            return;
        }

        this.currentTile = game.getField().getTileByCoordinats(this.tileIndex , this.line);
        this.inWater = currentTile.isWater();

        slipperyCooldown = Math.max(0f, slipperyCooldown - Math.max(0f, deltaTime));
        if (encasedInIce) {
            return;
        }

        this.velocityX = this.speed;
        updateEffects(deltaTime);

        if (hasEffect(EffectType.POISONED)) {
            applyPoisonDamage();
        }

        // The boolean/encased flags represent a complete ice shell.  The
        // temporary FROZEN effect (for example from Snow Pea) is a slow only;
        // getActualSpeed() applies its reduced speed below.
        if (frozen) {
            return;
        }

        for (Ability ability : abilities) {
            ability.execute(this, deltaTime, game);
        }

        eatTimer += deltaTime;
        Plant plant = game.findTargetPlant(this, Tile.getWidth() * 0.72f);
        targetPlant = plant;
        if (plant != null) {
            if (eatTimer >= eatCooldown) {
                eatTimer = 0;
                attack(plant, game);
            }
            move(deltaTime, game, plant);
            return;
        }
        targetPlant = null;
        move(deltaTime, game, null);
    }

    // ====== EFFECTS ======
    public void addEffect(Effect effect) {
        if (effect.getType() == EffectType.HYPNOTIZED) {
            removeEffect(EffectType.HYPNOTIZED);
        }
        effects.add(effect);
    }

    public void removeEffect(EffectType type) {
        effects.removeIf(e -> e.getType() == type);
    }

    public boolean hasEffect(EffectType type) {
        return effects.stream().anyMatch(e -> e.getType() == type);
    }

    private void updateEffects(float deltaTime) {
        for (Effect effect : effects) {
            effect.update(deltaTime);
        }
        effects.removeIf(Effect::isExpired);
    }

    private void applyPoisonDamage() {
        this.hp -= 5;
        if (this.hp <= 0) {
            this.hp = 0;
            die();
        }
    }

    public void meltFrozen() {
        removeEffect(EffectType.FROZEN);
    }

    public boolean isEncasedInIce() {
        return encasedInIce;
    }

    public void setEncasedInIce(boolean encasedInIce) {
        this.encasedInIce = encasedInIce;
    }

    public void breakIce() {
        encasedInIce = false;
        frozen = false;
        removeEffect(EffectType.FROZEN);
    }

    public boolean canSlide() {
        return slipperyCooldown <= 0f;
    }

    public void startSlideCooldown(float seconds) {
        slipperyCooldown = Math.max(0f, seconds);
    }

    public float getActualSpeed() {
        if (hasEffect(EffectType.FROZEN)) {
            return speed * 0.3f;
        }
        return speed;
    }

    public void fire() {
        if (fireClip != null) {
            isFiringRequested = true;
            isExtraRequested = false;
            actionTimeRemaining = Math.max(actionTimeRemaining, 1.0f);
            setState(ZombieState.FIRING);
        }
    }

    public void extra() {
        if (extraClip != null) {
            isExtraRequested = true;
            isFiringRequested = false;
            actionTimeRemaining = Math.max(actionTimeRemaining, 1.0f);
            setState(ZombieState.EXTRA);
        }
    }

    public void stopExtra() {
        isExtraRequested = false;
    }

    // ====== CORE METHODS ======
    public void move(float deltaTime, BaseGame game , Plant plant) {
        if (dead) return;
        Field field = game.getField();
        // here i should check whether zombie can move or not
//        if (passThroughObserver != null && passThroughObserver.canPassThrough(this, null)) {
//            x += getActualSpeed() * movingDirection();
//            return;
//        }
        if(plant != null && !this.type.equals("dodo"))
            return;

        x += getActualSpeed() * movingDirection() * deltaTime /15;
        int newTile = (int) (x / Tile.getWidth());
        if (newTile < 0) newTile = 0;
        if (newTile > 8) newTile = 8;

        if (newTile != tileIndex) {
            tileIndex = newTile;
        }
        this.currentTile = game.getField().getTileByCoordinats(this.tileIndex , this.line);
        this.inWater = currentTile.isWater();
    }

    public void attack(Plant plant , BaseGame game) {
        if (plant == null) return;
        if (hasEffect(EffectType.HYPNOTIZED)) return;

        plant.setHp(plant.getHp()- damage , this , game);
        if (plant.getHp()==0 && allStarObserver != null) {
            allStarObserver.onPlantKilled(this);
        }
    }

    public void takeDamage(int damage) {
        if (dead) return;

        for (Armor armor : armors) {
            if (armor.isActive()) {
                armor.takeDamage(damage);
                armor.updateVisibility();
                if (armor.isBroken() && "newspaper".equals(armor.getType())) {
                    if (newspaperObserver != null) {
                        newspaperObserver.onArmorBroken(this);
                    }
                }
                return;
            }
        }

        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            die();
        }
    }

    public void die() {
        if (dead) {
            return;
        }
        dead = true;
        models.User user = App.getCurrentuser();
        int random = (int)(Math.random() * 100);
        if (random <= 15) {
            int kind = random % 4;
            rewardDropType = switch (kind) {
                case 0 -> RewardDrop.Type.COIN_GOLD;
                case 1 -> RewardDrop.Type.COIN_SILVER;
                case 2 -> RewardDrop.Type.DIAMOND;
                default -> RewardDrop.Type.PLANT_FOOD;
            };
            rewardDropAmount = rewardDropType == RewardDrop.Type.COIN_GOLD ? 50
                : rewardDropType == RewardDrop.Type.COIN_SILVER ? 25 : 1;
        }
        if (user != null) {
            user.updateQuestProgress("KILL_ZOMBIE", 1);
        }
    }

    public RewardDrop.Type getRewardDropType() {
        return rewardDropType;
    }

    public int getRewardDropAmount() {
        return rewardDropAmount;
    }

    public boolean isRewardDropCreated() {
        return rewardDropCreated;
    }

    public void markRewardDropCreated() {
        rewardDropCreated = true;
    }

    public void render(SpriteBatch batch, PamPlayer player) {
        if (isDead() && currentState == ZombieState.DYING && stateTime >= 1.5f) {
            return;
        }

        updateAnimation(Gdx.graphics.getDeltaTime());

        String clipName = getCurrentClipName();
        if (clipName == null || clipName.isEmpty() || pamPath == null) {
            return;
        }
        player.draw(batch, pamPath, clipName, stateTime, getX(), getY(), false , visibilityMap);
    }

    // ====== ABILITIES ======
    public void addAbility(Ability ability) {
        abilities.add(ability);
    }

    @SuppressWarnings("unchecked")
    public <T extends Ability> T getAbility(Class<T> type) {
        for (Ability ability : abilities) {
            if (type.isInstance(ability)) {
                return (T) ability;
            }
        }
        return null;
    }

    public List<Ability> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    // ====== ARMOR ======
    public void addArmor(Armor armor) {
        armors.add(armor);
        armor.attachTo(this);
    }

    public List<Armor> getArmors() {
        return Collections.unmodifiableList(armors);
    }

    public boolean hasArmor() {
        return !armors.isEmpty();
    }

    public void changeLine(){
        int random =(int)(Math.random() * 5) ;
        if(random == 0 )
            this.setLine(this.line +1);
        else
            this.setLine(this.line -1);

        return;
    }

    // ====== STATE FLAGS ======
    public boolean isTorchOn() { return isTorchOn; }
    public void setTorchOn(boolean torchOn) { this.isTorchOn = torchOn; }

    public boolean isDynamiteFrozen() { return isDynamiteFrozen; }
    public void setDynamiteFrozen(boolean dynamiteFrozen) { this.isDynamiteFrozen = dynamiteFrozen; }

    // ====== PLANT INTERACTION (placeholder) ======
    public boolean reachedPlant() { return false; }
    public Plant findNextPlant() { return null; }
    public boolean isNearPlant() { return false; }
    public Plant getTargetPlant() { return targetPlant; }
    public boolean isNearHouse() { return x < 50; }
    public int movingDirection() { return hypnotized ? -1 : 1; }

    // ====== GETTERS & SETTERS ======
    public String getId() { return id; }
    public String getType() { return type; }
    public float getHp() { return hp; }
    @Override
    public void setHp(float hp) {
        this.hp = Math.max(0f, hp);
        if (this.hp <= 0f) {
            die();
        }
    }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }
    public float getSpeed() { return speed; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getEatCooldown() { return eatCooldown; }
    public boolean isDead() { return dead; }
    public boolean isFrozen() { return frozen; }
    public boolean isHypnotized() { return hypnotized; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void setHp(int hp) { setHp((float) hp); }
    public void setSpeed(float speed) { this.speed = speed; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setLine(int line) {
        this.line = Math.max(0, Math.min(4, line));
        this.y = this.line * Tile.getHeight();
    }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public void setHypnotized(boolean hypnotized) { this.hypnotized = hypnotized; }
    public void setX(float x) { this.x = x;}
    public void setY(float y) { this.y = y;}
    public int getTileIndex() { return tileIndex; }
    public void setTileIndex(int tileIndex) { this.tileIndex = tileIndex; }
    public int getCost() { return cost; }
    public Tile getCurrentTile() { return this.currentTile; }
    public void setCost(int cost) { this.cost = cost; }
    public boolean isInWater(){
        return this.inWater;
    }
    public void setInWater(boolean inWater){ this.inWater = inWater;}

    // ====== ANIMATION SETTERS & GETTERS ======
    public void setPamPath(String path) { this.pamPath = path; }
    public String getPamPath() { return pamPath; }

    public void setIdle(String clip) { this.idleClip = clip; }
    public void setWalk(String clip) { this.walkClip = clip; }
    public void setEat(String clip) { this.eatClip = clip; }
    public void setDie(String clip) { this.dieClip = clip; }
    public void setFire(String clip) { this.fireClip = clip; }
    public void setExtra(String clip) { this.extraClip = clip; }

    public String getIdle() { return idleClip; }
    public String getWalk() { return walkClip; }
    public String getEat() { return eatClip; }
    public String getDie() { return dieClip; }
    public String getFire() { return fireClip; }
    public String getExtra() { return extraClip; }

    public ZombieState getCurrentState() { return currentState; }
    public float getStateTime() { return stateTime; }

    public void setState(ZombieState newState) {

        if (currentState == ZombieState.FIRING && isFiringRequested) {
            if (newState != ZombieState.DYING) {
                return;
            }
        }
        if (this.currentState != newState) {
            this.currentState = newState;
            this.stateTime = 0f;
        }
    }

    public void updateStateTime(float delta) {
        this.stateTime += delta;
    }

    public void updateAnimationState() {
        if (isDead()) {
            if (currentState != ZombieState.DYING) {
                setState(ZombieState.DYING);
            }
            return;
        }

        if (isFiringRequested) {
            if (currentState != ZombieState.FIRING) {
                setState(ZombieState.FIRING);
            }
            return;
        }
        if (isExtraRequested) {
            if (currentState != ZombieState.EXTRA) {
                setState(ZombieState.EXTRA);
            }
            return;
        }

        if (isEating()) {
            if (currentState != ZombieState.EATING) {
                setState(ZombieState.EATING);
            }
            return;
        }

        if (Math.abs(getSpeed()) > 0.1f) {
            if (currentState != ZombieState.WALKING) {
                setState(ZombieState.WALKING);
            }
        } else {
            if (currentState != ZombieState.IDLE) {
                setState(ZombieState.IDLE);
            }
        }
    }

    public void updateAnimation(float delta) {
        actionTimeRemaining = Math.max(0f, actionTimeRemaining - Math.max(0f, delta));
        if (actionTimeRemaining <= 0f) {
            isFiringRequested = false;
            isExtraRequested = false;
        }
        updateAnimationState();
        updateStateTime(delta);
    }

    public String getCurrentClipName() {
        switch (currentState) {
            case IDLE:
                return idleClip;

            case WALKING:
                return walkClip;

            case EATING:
                return eatClip;

            case DYING:
                return dieClip;

            case FIRING:
                return fireClip != null ? fireClip : idleClip;

            case EXTRA:
                return extraClip != null ? extraClip : idleClip;

            default:
                return idleClip;
        }
    }

    public boolean isEating() {
        return eatTimer >= eatCooldown * 0.3f && getTargetPlant() != null;
    }

    // ====== Visibility Management ======
    public Map<String, Boolean> getVisibilityMap() { return visibilityMap; }

    public void setVisibility(String key, boolean visible) {
        visibilityMap.put(key, visible);
    }

    public boolean isVisible(String key) {
        return visibilityMap.getOrDefault(key, false);
    }


    public List<Effect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public AllStarObserver getAllStarObserver() { return allStarObserver; }
    public NewspaperObserver getNewspaperObserver() { return newspaperObserver; }
    public PassThroughObserver getPassThroughObserver() { return passThroughObserver; }

    public void setCurrentTile(Tile currentTile) {
        this.currentTile = currentTile;
    }
}
