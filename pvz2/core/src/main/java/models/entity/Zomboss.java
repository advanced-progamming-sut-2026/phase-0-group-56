package models.entity;

import models.games.BaseGame;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pvz.libpvz.pam.PamPlayer;
import java.util.*;

public class Zomboss extends Entity {

    private String pamPath;
    private float stateTime = 0f;

    private String idleClip = "idle";
    private String introClip = "intro";
    private String stunClip = "stun";
    private String dieClip = "die";
    private String talkClip = "die_talk";
    private String exitClip = "die_exit";
    private String attack1Clip = null;
    private String attack2Clip = null;
    private String attack3Clip = null;

    private String currentClip = null;

    // ====== STATE ======
    public enum BossState {
        INTRO, IDLE, ATTACKING, STUNNED, DYING, TALKING, EXITING, DEAD
    }
    private BossState state = BossState.IDLE;
    private BossState nextState = null;

    // ====== STATE TIMER ======
    private float stateTimer = 0f;
    private float stateDuration = 0f;
    private boolean stateTiming = false;

    // ====== STUN TRACKING ======
    private int stunThreshold1;
    private int stunThreshold2;
    private int lastStunState = 0;

    // ====== DEATH SEQUENCE ======
    private boolean deathSequenceStarted = false;

    // ====== ATTACK ======
    private float attackTimer = 0f;
    private float attackCooldown = 5f;
    private Random random = new Random();

    // ====== CONSTRUCTOR ======
    public Zomboss(float x, float y, int line, String pamPath) {
        super();
        this.x = x;
        this.y = y;
        this.line = line;  // lowest line
        this.tileIndex = 6; // lowest col
        this.hp = 3000;
        this.pamPath = pamPath;
        this.maxHp = hp;
        this.width = 100;
        this.height = 150;
        this.isAlive = true;

        this.stunThreshold1 = 2000;
        this.stunThreshold2 = 1000;
    }

    // ====== TIMER METHODS ======
    private void startStateTimer(float duration) {
        stateTimer = 0f;
        stateDuration = duration;
        stateTiming = true;
    }

    private void startStateTimer(PamPlayer player, String clipName) {
        if (clipName == null || pamPath == null) {
            startStateTimer(2.0f); // fallback
            return;
        }
        float duration = player.clipDurationSeconds(pamPath, clipName);
        startStateTimer(duration > 0 ? duration : 2.0f);
    }

    private boolean isStateTimerComplete() {
        return stateTiming && stateTimer >= stateDuration;
    }

    private void resetStateTimer() {
        stateTiming = false;
        stateTimer = 0f;
        stateDuration = 0f;
    }

    // ====== STATE MANAGEMENT ======
    private void setState(BossState newState, PamPlayer player) {
        this.state = newState;
        this.stateTime = 0f;
        this.currentClip = getClipForState(newState);

        if (newState == BossState.DEAD) {
            resetStateTimer();
            return;
        }
        startStateTimer(player, currentClip);
    }

    private void setState(BossState newState, float duration) {
        this.state = newState;
        this.stateTime = 0f;
        this.currentClip = getClipForState(newState);
        startStateTimer(duration);
    }

    private String getClipForState(BossState state) {
        switch(state) {
            case INTRO: return introClip;
            case IDLE: return idleClip;
            case ATTACKING: return currentClip;
            case STUNNED: return stunClip;
            case DYING: return dieClip;
            case TALKING: return talkClip;
            case EXITING: return exitClip;
            default: return idleClip;
        }
    }

    // ====== UPDATE ======
    public void update(float delta, BaseGame game, PamPlayer player) {
        if (state == BossState.DEAD || !isAlive) return;

        stateTime += delta;

        if (stateTiming) {
            stateTimer += delta;
            if (isStateTimerComplete()) {
                onStateTimerComplete(game, player);
                return;
            }
        }

        if (state == BossState.IDLE && !stateTiming) {
            updateIdleState(game);
        }
    }

    // ====== ON STATE TIMER COMPLETE ======
    private void onStateTimerComplete(BaseGame game, PamPlayer player) {
        resetStateTimer();

        switch(state) {
            case INTRO:
                goToIdle(player);
                break;

            case STUNNED:
                goToIdle(player);
                break;

            case ATTACKING:
                goToIdle(player);
                break;

            case DYING:
                goToTalk(player);
                break;

            case TALKING:
                goToExit(player);
                break;

            case EXITING:
                goToDead();
                break;

            default:
                break;
        }
    }

    // ====== IDLE STATE ======
    private void updateIdleState(BaseGame game) {

        if (checkAndApplyStun()) return;

        attackTimer += Gdx.graphics.getDeltaTime();
        if (attackTimer >= attackCooldown) {
            performAttack(game);
            attackTimer = 0f;
        }
    }

    private void goToIdle(PamPlayer player) {
        state = BossState.IDLE;
        stateTime = 0f;
        currentClip = idleClip;
        resetStateTimer();
    }

    // ====== STUN ======
    private boolean checkAndApplyStun() {
        int currentHp = (int)hp;

        if (lastStunState == 0 && currentHp <= stunThreshold1 && currentHp > stunThreshold2) {
            lastStunState = 1;
            goToStun();
            return true;
        }

        if (lastStunState == 1 && currentHp <= stunThreshold2 && currentHp > 0) {
            lastStunState = 2;
            goToStun();
            return true;
        }

        return false;
    }

    private void goToStun() {
        setState(BossState.STUNNED, stunClip, 1.5f); // مدت زمان Stun
    }

    // ====== ATTACK ======
    private void performAttack(BaseGame game) {
        List<String> availableAttacks = new ArrayList<>();
        if (attack1Clip != null) availableAttacks.add(attack1Clip);
        if (attack2Clip != null) availableAttacks.add(attack2Clip);
        if (attack3Clip != null) availableAttacks.add(attack3Clip);

        if (availableAttacks.isEmpty()) return;

        String chosen = availableAttacks.get(random.nextInt(availableAttacks.size()));
        currentClip = chosen;
        setState(BossState.ATTACKING, chosen, 1.0f);

        executeAttack(chosen, game);
    }

    protected void executeAttack(String clipName, BaseGame game) {
        // در کلاس‌های فرزند پیاده‌سازی می‌شود
    }

    // ====== DEATH SEQUENCE ======
    private void goToDie(PamPlayer player) {
        setState(BossState.DYING, dieClip, player);
    }

    private void goToTalk(PamPlayer player) {
        setState(BossState.TALKING, talkClip, player);
    }

    private void goToExit(PamPlayer player) {
        setState(BossState.EXITING, exitClip, player);
    }

    private void goToDead() {
        state = BossState.DEAD;
        isAlive = false;
        resetStateTimer();
    }

    // ====== TAKE DAMAGE ======
    public void takeDamage(float damage) {
        if (state == BossState.DEAD || !isAlive) return;

        float newHp = Math.max(0, this.hp - damage);
        this.hp = newHp;

        if (this.hp <= 0 && !deathSequenceStarted) {
            deathSequenceStarted = true;
            // نیاز به PamPlayer داریم، ولی اینجا نداریم
            // باید از caller دریافت کنیم
        }
    }

    public void startDeathSequence(PamPlayer player) {
        if (deathSequenceStarted) return;
        deathSequenceStarted = true;
        goToDie(player);
    }

    // ====== SETTER WITH DURATION ======
    private void setState(BossState newState, String clipName, PamPlayer player) {
        this.state = newState;
        this.stateTime = 0f;
        this.currentClip = clipName;
        startStateTimer(player, clipName);
    }

    private void setState(BossState newState, String clipName, float duration) {
        this.state = newState;
        this.stateTime = 0f;
        this.currentClip = clipName;
        startStateTimer(duration);
    }

    // ====== RENDER ======
    public void render(SpriteBatch batch, PamPlayer player) {
        if (state == BossState.DEAD) return;

        String clipName = getCurrentClipName();
        if (clipName == null || pamPath == null) return;

        float scale = 1.0f;
        player.draw(batch, pamPath, clipName, stateTime, x, y, scale, scale, false, visibilityMap);
    }

    public String getCurrentClipName() {
        return currentClip != null ? currentClip : idleClip;
    }

    // ====== SETTERS ======
    public void setPamPath(String path) { this.pamPath = path; }
    public void setIntroClip(String clip) { this.introClip = clip; }
    public void setStunClip(String clip) { this.stunClip = clip; }
    public void setDieClip(String clip) { this.dieClip = clip; }
    public void setTalkClip(String clip) { this.talkClip = clip; }
    public void setExitClip(String clip) { this.exitClip = clip; }
    public void setAttackClips(String attack1, String attack2, String attack3) {
        this.attack1Clip = attack1;
        this.attack2Clip = attack2;
        this.attack3Clip = attack3;
    }
    public void setAttackCooldown(float cooldown) { this.attackCooldown = cooldown; }
    public void setMaxHp(float maxHp) {
        this.maxHp = maxHp;
        this.stunThreshold1 = (int)(maxHp / 1000f) * 666;
        this.stunThreshold2 = (int)(maxHp / 1000f) * 333;
    }
    public void setState(BossState state) { this.state = state; }

    // ====== GETTERS ======
    public BossState getState() { return state; }
    public boolean isBossAlive() { return isAlive && state != BossState.DEAD; }
    public float getMaxHp() { return maxHp; }
    public String getPamPath() { return pamPath; }
    public Map<String, Boolean> getVisibilityMap() { return visibilityMap; }

    // ====== COLLISION ======
    public boolean occupiesCell(int row, int col) {
        return (row == this.line || row == this.line + 1) &&
            (col >= this.tileIndex && col <= this.tileIndex + 2);
    }
}
