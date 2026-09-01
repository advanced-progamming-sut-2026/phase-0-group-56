package models.entity;
import com.badlogic.gdx.math.Rectangle;
import models.factory.builder.PlantType;

import models.Constants;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.games.BaseGame;

import java.util.ArrayList;
import java.util.Arrays;


public class Projectile implements Cloneable {
    /** Point on a zombie's hit box that a lobber aims for (the head). */
    public static final float ZOMBIE_HEAD_AIM_FRACTION = 0.82f;

    private ProjectileType type;
    private PlantType sourcePlantType;
    private float velocityX;
    private float velocityY;
    private float width = 50;
    private float height = 50;
    private float destinationX;
    private float destinationY;
    private float damage;
    private float aoEDamage;
    private float x;
    private float y;
    private float pierce = 1;
    private boolean grounded = true;
    private boolean ignoresObstacles;
    private boolean active;
    private float poisonDamage = Constants.POISON_BASE_DAMAGE;
    private final ArrayList<ProjectileType> bowling = new ArrayList<>(Arrays.asList(ProjectileType.ONION_1,
        ProjectileType.ONION_2 , ProjectileType.ONION_3 , ProjectileType.Explosive_Onion));


    /// ------------BOOLEANS------------
    public enum Tag{MAGICAL,ICE,FIRE,POISON,HOMING,AoE}
    ArrayList<Tag> tags = new  ArrayList<>();
    private boolean proved = false;
    /// for homing plantsInField of course!
    private Zombie toLockIn;
    /** Optional lobber target: collision is resolved only after the arc lands. */
    private Zombie impactTarget;
    public void setTags(ArrayList<PlantTags> tags) {
        this.tags.clear();
        if (tags == null) {
            return;
        }

        if (tags.contains(PlantTags.Fire)) {
            this.tags.add(Tag.FIRE);
        }
        if (tags.contains(PlantTags.POISON)) {
            this.tags.add(Tag.POISON);
        }
        if (tags.contains(PlantTags.Ice)) {
            this.tags.add(Tag.ICE);
        }
        if (tags.contains(PlantTags.MAGICAL)) {
            this.tags.add(Tag.MAGICAL);
        }
        if (tags.contains(PlantTags.AoE)) {
            this.tags.add(Tag.AoE);
        }
    }


    public Projectile(float x, float y , float velocityX , float velocityY, int line) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.line = line;
    }

    public Projectile(float x, float y , float velocityX , ProjectileType type , float damage
        , int line) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.type = type;
        this.damage = damage;
        this.line = line;
    }

    public Projectile(float x, float y , ProjectileType projectileType, int line) {
        this.x = x;
        this.y = y;
        this.type = projectileType;
        this.velocityX = Constants.BULLET_VELOCITY_X;
        damage = 20;
        this.line = line;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Projectile(){

    }

    public void run(float delta, BaseGame game) {
        if (pierce <= 0) {
            dispose(game);
            return;
        }

        // Keep the previous position so a fast projectile cannot tunnel through
        // a zombie between two simulation ticks.  This is especially noticeable
        // when a zombie reaches a plant and the projectile starts very close to
        // its hit box.
        float previousX = x;
        float previousY = y;
        updateLocation(delta, game);

        if (impactTarget != null) {
            if (impactTarget.isDead() || impactTarget.getHp() <= 0f) {
                dispose(game);
                return;
            }
            if (!grounded) {
                return;
            }
            // Lobbers land on the target's head, so a moving zombie cannot make
            // the projectile pass through its old predicted position.
            x = impactTarget.getX() + impactTarget.getWidth() * 0.5f - width * 0.5f;
            y = zombieHeadY(impactTarget) - height * 0.5f;
            hitZombie(impactTarget);
            return;
        }

        // Airborne lobber shots travel over tiles/plants; their obstacle-ignore
        // flag remains set after landing so the impact is not consumed by the
        // landing tile before collision resolution.
        if (!ignoresObstacles && !tags.contains(Tag.MAGICAL) && toLockIn == null && grounded) {
            block(game);
        }

        if (bowling.contains(this.type)) {
            bowling(game.getField());
        }

        checkHit(game, previousX, previousY, delta);
    }

    private void checkHit(BaseGame game, float previousX, float previousY, float delta){
        if (toLockIn != null) {

            if (!toLockIn.isDead() && toLockIn.getHp() > 0f
                && (overlaps(toLockIn)
                || sweptOverlaps(toLockIn, previousX, previousY, delta))) {
                hitZombie(toLockIn);
            }
            return;
        }

        Zombie target = null;
        float targetDistance = Float.MAX_VALUE;
        float targetHitTime = Float.MAX_VALUE;
        for (Zombie z : game.getZombies()) {
            if (z == null || z.isDead() || z.getHp() <= 0 || z.getLine() != this.line) {
                continue;
            }

            // Choose the first hit along the travelled segment.  Looking only
            // at the projectile's final position (or at the globally nearest
            // zombie) lets a fast shot tunnel through the front zombie and hit
            // one behind it, or miss both when the target moved between ticks.
            float hitTime = sweptCollisionTime(z, previousX, previousY, delta);
            if (hitTime >= 0f) {
                float distance = Math.abs((x + width * 0.5f)
                    - (z.getX() + z.getWidth() * 0.5f));
                if (target == null
                    || hitTime < targetHitTime - 0.0001f
                    || (Math.abs(hitTime - targetHitTime) <= 0.0001f
                        && distance < targetDistance)) {
                    target = z;
                    targetDistance = distance;
                    targetHitTime = hitTime;
                }
            }
        }

        if(target != null){
            hitZombie(target);
            if(tags.contains(Tag.AoE)){
                damageOnArea(1 , game);
            }
        }

    }

    /**
     * Returns the first normalized time at which this projectile's hitbox
     * intersects the zombie, or {@code -1} when the whole segment misses.
     * The slab test is continuous, so it remains reliable at high game speeds
     * and at row/plant boundaries.
     */
    private float sweptCollisionTime(
        Zombie zombie, float previousX, float previousY, float delta
    ) {
        if (zombie == null) {
            return -1f;
        }

        float startX = previousX + width * 0.5f;
        float startY = previousY + height * 0.5f;
        float endX = x + width * 0.5f;
        float endY = y + height * 0.5f;
        float dx = endX - startX;
        float dy = endY - startY;

        // Expand the zombie rectangle by the projectile's half extents and
        // cast the projectile centre through that rectangle.
        float zombieMotionPadding = Math.abs(zombie.getVelocityX())
            * Math.max(0f, delta);
        float minX = zombie.getX() - width * 0.5f - zombieMotionPadding;
        float maxX = zombie.getX() + zombie.getWidth()
            + width * 0.5f + zombieMotionPadding;
        float minY = zombie.getY() - height * 0.5f;
        float maxY = zombie.getY() + zombie.getHeight() + height * 0.5f;

        if (startX >= minX && startX <= maxX
            && startY >= minY && startY <= maxY) {
            return 0f;
        }

        return sweptCollisionTimeScalar(startX, startY, dx, dy, minX, maxX, minY, maxY);
    }

    private float sweptCollisionTimeScalar(
        float startX, float startY, float dx, float dy,
        float minX, float maxX, float minY, float maxY
    ) {
        float enter = 0f;
        float exit = 1f;

        if (Math.abs(dx) < 0.00001f) {
            if (startX < minX || startX > maxX) return -1f;
        } else {
            float tx1 = (minX - startX) / dx;
            float tx2 = (maxX - startX) / dx;
            float near = Math.min(tx1, tx2);
            float far = Math.max(tx1, tx2);
            enter = Math.max(enter, near);
            exit = Math.min(exit, far);
            if (enter > exit) return -1f;
        }

        if (Math.abs(dy) < 0.00001f) {
            if (startY < minY || startY > maxY) return -1f;
        } else {
            float ty1 = (minY - startY) / dy;
            float ty2 = (maxY - startY) / dy;
            float near = Math.min(ty1, ty2);
            float far = Math.max(ty1, ty2);
            enter = Math.max(enter, near);
            exit = Math.min(exit, far);
            if (enter > exit) return -1f;
        }

        return enter >= 0f && enter <= 1f ? enter : -1f;
    }

    private boolean sweptOverlaps(
        Zombie zombie, float previousX, float previousY, float delta
    ) {
        return sweptCollisionTime(zombie, previousX, previousY, delta) >= 0f;
    }

    private static float zombieHeadY(Zombie zombie) {
        return zombie.getY() + zombie.getHeight() * ZOMBIE_HEAD_AIM_FRACTION;
    }

    private void hitZombie(Zombie z){
        this.pierce -= 1;
        if (z.isEncasedInIce()) {
            z.breakIce();
        }
        z.notifyBulletObservers(this);

        z.takeDamage((int) this.damage);
        if (this.getTags().contains(Tag.ICE)) {
            z.addEffect(new Effect(EffectType.FROZEN, 3.0f));
        }
        if (this.getTags().contains(Tag.POISON)) {
            z.addEffect(new Effect(EffectType.POISONED, 5.0f));
        }
        // Any hit cracks a Frozen Caves ice shell; fire additionally clears
        // dynamite-freeze state used by the Prospector.
        z.setFrozen(false);
        if (this.getTags().contains(Tag.FIRE)) {
            z.setDynamiteFrozen(false);
        }

    }


    private void damageOnArea(int radius , BaseGame  game){
        for (Zombie z : game.getZombies()) {
            float dx = Math.abs(z.getX() - this.x);
            float dy = Math.abs(z.getY() - this.y);
            if(dx <= Tile.getWidth() * radius && dy <= Tile.getHeight() * radius){
                z.setHurt(true);
                z.takeDamage((int) this.damage);
            }
        }
    }
    private void dispose(BaseGame game) {
        game.getBullets().remove(this);
    }

    private void block(BaseGame game){
        Rectangle bounds = new Rectangle(x , y , width, height);
        for (int i = 0; i < 5; i++) {
            // A straight shot belongs to one lane. Checking every row made a
            // projectile at a row boundary consume its pierce on the adjacent
            // lane's tile before it could reach the zombie in front of it.
            if (i != line) {
                continue;
            }
            for (Tile tile : game.getField().getTiles().get(i)){
                if(bounds.overlaps(tile.getBounds())) {
                    if(tile.getTileType() == TileType.FROZEN && tile.getHp() > 0){
                        tile.setHp(tile.getHp() - this.damage);
                        if (tile.getHp() <= 0) {
                            tile.setTileType(TileType.CAVE_TILE);
                        }
                        setPierce(pierce - 1);
                    }
                }
            }
        }

        for (Plant p : game.getPlantsInField()){
            if (p == null || p.getLine() != line) {
                continue;
            }
            Rectangle plantBounds = new Rectangle(p.getX(), p.getY(), p.getWidth(), p.getHeight());
            if (!bounds.overlaps(plantBounds)) {
                continue;
            }
            if(p.isFrozen()){
                if(this.tags.contains(Tag.FIRE)){
                    p.setFreezeHp(0);
                }
                else {
                    p.setFreezeHp(p.freezeHp -  this.damage);
                }
            }
        }
        Zombie z ;

    }

    public int line;
    private void updateLocation(float delta, BaseGame game){
        if(toLockIn != null){
            setDest();
        }
        this.x += velocityX * delta;
        this.y += velocityY * delta;
        if (!grounded) {
            // Scene2D/world coordinates grow upward; gravity pulls the arc
            // toward decreasing Y after the upward launch.
            this.velocityY -= Constants.GRAVITY * delta;
        }

        if (impactTarget != null) {
            // y is the projectile hitbox's lower-left corner.  Subtract half
            // its height so the rendered centre lands on the zombie's head.
            float landingY = zombieHeadY(impactTarget) - height * 0.5f;
            if (!grounded && velocityY < 0f && this.y <= landingY) {
                this.y = landingY;
                this.velocityY = 0f;
                grounded = true;
            }
            return;
        }

        float groundY = line * Tile.getHeight() + 30f;
        if (!grounded && this.y <= groundY && velocityY < 0f) {
            this.y = groundY;
            this.velocityY = 0f;
            grounded = true;

            if (tags.contains(Tag.AoE)) {
                AoE(game);
            }
        }

    }
    private void setDest(){
        float dy = toLockIn.getY() - y;
        float dx = toLockIn.getX() - x;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        velocityX = Constants.HOMING_VELOCITY * (dx / d);
        velocityY = Constants.HOMING_VELOCITY * (dy / d);
    }

    private void bowling(Field field){
        if(this.y + this.height >= field.getHeight()){
            velocityY *= -1;
        }
        else if(this.y <= 0){
            velocityY *= -1;
        }
    }

    private void AoE(BaseGame game){
        for (Zombie z : game.getZombies()) {
            float dx = Math.abs(z.getX() - this.x);
            float dy = Math.abs(z.getY() - this.y);
            if(dx <= Tile.getWidth() * 1 && dy <= Tile.getHeight() * 1){
                z.takeDamage((int) this.aoEDamage);
                if (this.getTags().contains(Tag.ICE)) {
                    z.addEffect(new Effect(EffectType.FROZEN, 3.0f));
                }
                if (this.getTags().contains(Tag.POISON)) {
                    z.addEffect(new Effect(EffectType.POISONED, 5.0f));
                }
            }
        }
    }


    public float getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

    public float getDestinationX() {
        return destinationX;
    }

    public void setDestinationX(float destinationX) {
        this.destinationX = destinationX;
    }

    public float getDestinationY() {
        return destinationY;
    }

    public void setDestinationY(float destinationY) {
        this.destinationY = destinationY;
    }

    public float getDamage() {
        return damage;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isProved() {
        return proved;
    }

    public void setProved(boolean proved) {
        this.proved = proved;
    }

    public Zombie getToLockIn() {
        return toLockIn;
    }

    public void setToLockIn(Zombie toLockIn) {
        this.toLockIn = toLockIn;
    }

    public Zombie getImpactTarget() {
        return impactTarget;
    }

    public void setImpactTarget(Zombie impactTarget) {
        this.impactTarget = impactTarget;
    }

    public ProjectileType getType() {
        return type;
    }

    public void setType(ProjectileType type) {
        this.type = type;
    }

    public ArrayList<Tag> getTags() {
        return tags;
    }
    public PlantType getSourcePlantType() {
        return sourcePlantType;
    }

    public void setSourcePlantType(PlantType sourcePlantType) {
        this.sourcePlantType = sourcePlantType;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Projectile clone = (Projectile) super.clone();

        if (this.tags != null) {
            clone.tags = new ArrayList<>(this.tags);
        } else {
            clone.tags = new ArrayList<>();
        }

        clone.width = this.width;
        clone.height = this.height;
        clone.aoEDamage = this.aoEDamage;
        clone.pierce = this.pierce;
        clone.grounded = this.grounded;
        clone.ignoresObstacles = this.ignoresObstacles;
        clone.active = this.active;
        clone.poisonDamage = this.poisonDamage;
        clone.toLockIn = this.toLockIn;
        clone.impactTarget = this.impactTarget;

        return clone;
    }
    public boolean overlaps(Tile tile) {
        if (tile == null) {
            return false;
        }

        return (this.x < tile.getX() + Tile.getWidth()) &&
            (this.x + this.width > tile.getX()) &&
            (this.y < tile.getY() + Tile.getHeight()) &&
            (this.y + this.height > tile.getY());
    }

    public boolean overlaps(Zombie zombie){
        if (zombie == null) {
            return false;
        }

        return (this.getX() < zombie.getX() + zombie.getWidth()) &&
            (this.getX() + width > zombie.getX()) &&
            (this.getY() < zombie.getY() + zombie.getHeight()) &&
            (this.getY() + height > zombie.getY());

    }

    public void setPierce(float pierce) {
        this.pierce = pierce;
    }

    public boolean isActive(){
        return this.active;
    }
    public void setActive(boolean active){
        this.active = active;
    }

    public float getPierce() {
        return pierce;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setIgnoresObstacles(boolean ignoresObstacles) {
        this.ignoresObstacles = ignoresObstacles;
    }

    public boolean isIgnoresObstacles() {
        return ignoresObstacles;
    }
}
