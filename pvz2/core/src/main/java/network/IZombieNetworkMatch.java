package network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic server-side simulation for the network I, Zombie minigame.
 * Clients never mutate this object; they submit role-checked actions instead.
 */
public final class IZombieNetworkMatch {
    public static final int ROW_COUNT = 5;
    public static final int COLUMN_COUNT = 9;
    public static final int FIRST_ZOMBIE_COLUMN = 5;
    public static final int MATCH_DURATION_SECONDS = 120;

    private static final List<Card> PLANT_CARDS = List.of(
        new Card("peashooter", 100, 300, 20f, 1.2f, 0f),
        new Card("wall_nut", 50, 1200, 0f, 0f, 0f),
        new Card("sunflower", 50, 200, 0f, 0f, 25f)
    );
    private static final List<Card> ZOMBIE_CARDS = List.of(
        new Card("normal", 50, 190, 0f, 0.55f, 10f),
        new Card("cone", 75, 300, 0f, 0.42f, 12f),
        new Card("imp", 50, 120, 0f, 0.72f, 8f)
    );

    private final String matchId;
    private final Map<Long, MutableUnit> plants = new LinkedHashMap<>();
    private final Map<Long, MutableUnit> zombies = new LinkedHashMap<>();
    private final Map<Long, MutableProjectile> projectiles = new LinkedHashMap<>();
    private final boolean[] eatenBrains = new boolean[ROW_COUNT];
    private long nextId = 1;
    private long revision;
    private float elapsed;
    private float plantIncomeTimer;
    private float zombieIncomeTimer;
    private int plantSun = 300;
    private int zombieSun = 150;
    private int plantScore;
    private int zombieScore;
    private boolean plantsJoined;
    private boolean zombiesJoined;
    private IZombieNetworkState.Phase phase = IZombieNetworkState.Phase.WAITING;
    private IZombieNetworkState.Role winner;

    public IZombieNetworkMatch(String matchId) {
        this.matchId = matchId == null ? "" : matchId;
    }

    private final List<GameMessage> chatHistory = new ArrayList<>();

    public static final int MAX_CHAT_HISTORY = 50;

    public static List<Card> plantCards() {
        return PLANT_CARDS;
    }

    public static List<Card> zombieCards() {
        return ZOMBIE_CARDS;
    }

    public static List<String> textReactions() {
        return List.of("NICE_MOVE", "HURRY_UP", "GOOD_GAME");
    }

    public static List<String> emojiReactions() {
        return List.of("😀", "😎", "🌻");
    }

    public static List<String> stickerReactions() {
        return List.of("SUN_DANCE", "ZOMBIE_DANCE", "BRAIN_POP");
    }

    public synchronized ActionResult join(IZombieNetworkState.Role role) {
        if (role == null) {
            return ActionResult.error("INVALID_ROLE", "A valid role is required.");
        }
        if (phase != IZombieNetworkState.Phase.WAITING) {
            return ActionResult.error("MATCH_STARTED", "The match has already started.");
        }
        if (role == IZombieNetworkState.Role.PLANTS) {
            if (plantsJoined) {
                return ActionResult.error("ROLE_TAKEN", "The plants role is already taken.");
            }
            plantsJoined = true;
        } else {
            if (zombiesJoined) {
                return ActionResult.error("ROLE_TAKEN", "The zombies role is already taken.");
            }
            zombiesJoined = true;
        }
        if (plantsJoined && zombiesJoined) {
            phase = IZombieNetworkState.Phase.PLAYING;
        }
        revision++;
        return ActionResult.ok("Joined the match.");
    }

    public synchronized ActionResult placePlant(
        IZombieNetworkState.Role role, String type, int column, int row
    ) {
        if (!canAct(role, IZombieNetworkState.Phase.PLAYING)) {
            return ActionResult.error("NOT_ALLOWED", "Only the plants player can place plants now.");
        }
        if (column < 0 || column >= FIRST_ZOMBIE_COLUMN || row < 0 || row >= ROW_COUNT) {
            return ActionResult.error("INVALID_TILE", "Plants must be placed on the left side of the lawn.");
        }
        Card card = card(PLANT_CARDS, type);
        if (card == null) {
            return ActionResult.error("UNKNOWN_PLANT", "That plant is not available in this match.");
        }
        if (plantSun < card.cost()) {
            return ActionResult.error("NOT_ENOUGH_SUN", "Not enough plant sun.");
        }
        for (MutableUnit plant : plants.values()) {
            if (plant.row == row && Math.abs(plant.x - (column + 0.5f)) < 0.01f) {
                return ActionResult.error("OCCUPIED", "That tile is occupied.");
            }
        }
        long id = nextId++;
        plants.put(id, new MutableUnit(id, card.type(), row, column + 0.5f, card.hp()));
        plantSun -= card.cost();
        revision++;
        return ActionResult.ok("Plant placed.");
    }

    public synchronized ActionResult placeZombie(
        IZombieNetworkState.Role role, String type, int column, int row
    ) {
        if (!canAct(role, IZombieNetworkState.Phase.PLAYING)) {
            return ActionResult.error("NOT_ALLOWED", "Only the zombies player can deploy zombies now.");
        }
        if (column < FIRST_ZOMBIE_COLUMN || column >= COLUMN_COUNT
            || row < 0 || row >= ROW_COUNT) {
            return ActionResult.error("INVALID_TILE", "Zombies must be deployed right of the red line.");
        }
        Card card = card(ZOMBIE_CARDS, type);
        if (card == null) {
            return ActionResult.error("UNKNOWN_ZOMBIE", "That zombie is not available in this match.");
        }
        if (zombieSun < card.cost()) {
            return ActionResult.error("NOT_ENOUGH_SUN", "Not enough zombie sun.");
        }
        for (MutableUnit zombie : zombies.values()) {
            if (zombie.row == row && Math.abs(zombie.x - (column + 0.5f)) < 0.01f) {
                return ActionResult.error("OCCUPIED", "That tile is occupied.");
            }
        }
        long id = nextId++;
        zombies.put(id, new MutableUnit(id, card.type(), row, column + 0.5f, card.hp()));
        zombieSun -= card.cost();
        revision++;
        return ActionResult.ok("Zombie deployed.");
    }

    public synchronized void tick(float delta) {
        if (phase != IZombieNetworkState.Phase.PLAYING) {
            return;
        }
        float safeDelta = Math.max(0f, Math.min(delta, 0.25f));
        elapsed += safeDelta;
        plantIncomeTimer += safeDelta;
        zombieIncomeTimer += safeDelta;
        while (plantIncomeTimer >= 6f) {
            plantSun += 25;
            plantIncomeTimer -= 6f;
        }
        while (zombieIncomeTimer >= 8f) {
            zombieSun += 25;
            zombieIncomeTimer -= 8f;
        }
        updatePlants(safeDelta);
        updateProjectiles(safeDelta);
        updateZombies(safeDelta);
        evaluateEnd();
        revision++;
    }

    public synchronized IZombieNetworkState snapshot() {
        List<IZombieNetworkState.Unit> plantCopy = new ArrayList<>();
        for (MutableUnit unit : plants.values()) {
            plantCopy.add(unit.snapshot());
        }
        List<IZombieNetworkState.Unit> zombieCopy = new ArrayList<>();
        for (MutableUnit unit : zombies.values()) {
            zombieCopy.add(unit.snapshot());
        }
        List<IZombieNetworkState.Projectile> projectileCopy = new ArrayList<>();
        for (MutableProjectile projectile : projectiles.values()) {
            projectileCopy.add(projectile.snapshot());
        }
        return new IZombieNetworkState(matchId, revision, phase, winner,
            Math.max(0L, (long) ((MATCH_DURATION_SECONDS - elapsed) * 1000f)),
            plantSun, zombieSun, countBrains(), plantScore, zombieScore,
            eatenBrains, plantCopy, zombieCopy, projectileCopy);
    }

    public synchronized ActionResult react(
        IZombieNetworkState.Role role, String category, String value
    ) {
        if (!canAct(role, IZombieNetworkState.Phase.PLAYING)) {
            return ActionResult.error("NOT_ALLOWED", "Reactions are available while the match is running.");
        }
        if (!validReaction(category, value)) {
            return ActionResult.error("INVALID_REACTION", "That reaction is not available.");
        }
        return ActionResult.ok("Reaction sent.");
    }

    public synchronized ActionResult leave(IZombieNetworkState.Role role) {
        if (role == IZombieNetworkState.Role.PLANTS) {
            plantsJoined = false;
        } else if (role == IZombieNetworkState.Role.ZOMBIES) {
            zombiesJoined = false;
        }
        if (phase == IZombieNetworkState.Phase.PLAYING) {
            phase = IZombieNetworkState.Phase.ABORTED;
        }
        revision++;
        return ActionResult.ok("Left the match.");
    }

    private void updatePlants(float delta) {
        for (MutableUnit plant : plants.values()) {
            plant.cooldown -= delta;
            if ("sunflower".equals(plant.type) && plant.cooldown <= 0f) {
                plantSun += 25;
                plant.cooldown = 8f;
            }
            if (!"peashooter".equals(plant.type) || plant.cooldown > 0f) {
                continue;
            }
            MutableUnit target = nearestZombie(plant.row, plant.x);
            if (target != null) {
                long id = nextId++;
                projectiles.put(id, new MutableProjectile(id, plant.row, plant.x + 0.35f, 20f));
                plant.cooldown = 1.2f;
            }
        }
    }

    private void updateProjectiles(float delta) {
        Iterator<MutableProjectile> iterator = projectiles.values().iterator();
        while (iterator.hasNext()) {
            MutableProjectile projectile = iterator.next();
            projectile.x += 4.2f * delta;
            MutableUnit target = null;
            for (MutableUnit zombie : zombies.values()) {
                if (zombie.row == projectile.row
                    && Math.abs(zombie.x - projectile.x) < 0.28f) {
                    target = zombie;
                    break;
                }
            }
            if (target != null) {
                target.hp -= projectile.damage;
                if (target.hp <= 0f) {
                    zombies.remove(target.id);
                    plantScore += 50;
                }
                iterator.remove();
            } else if (projectile.x > COLUMN_COUNT + 1f) {
                iterator.remove();
            }
        }
    }

    private void updateZombies(float delta) {
        for (MutableUnit zombie : zombies.values()) {
            Card card = card(ZOMBIE_CARDS, zombie.type);
            MutableUnit target = nearestPlant(zombie.row, zombie.x);
            if (target != null && zombie.x - target.x < 0.75f) {
                zombie.cooldown -= delta;
                if (zombie.cooldown <= 0f) {
                    target.hp -= card == null ? 10f : card.damage();
                    zombie.cooldown = "imp".equals(zombie.type) ? 0.55f : 0.85f;
                    if (target.hp <= 0f) {
                        plants.remove(target.id);
                        zombieScore += 25;
                    }
                }
            } else {
                zombie.x -= card == null ? 0.5f * delta : card.speed() * delta;
            }
        }
        Iterator<MutableUnit> iterator = zombies.values().iterator();
        while (iterator.hasNext()) {
            MutableUnit zombie = iterator.next();
            if (zombie.x <= 0.1f) {
                eatenBrains[zombie.row] = true;
                zombieScore += 100;
                iterator.remove();
            }
        }
    }

    private void evaluateEnd() {
        if (countBrains() == ROW_COUNT) {
            winner = IZombieNetworkState.Role.ZOMBIES;
            phase = IZombieNetworkState.Phase.ZOMBIES_WON;
            return;
        }
        if (elapsed >= MATCH_DURATION_SECONDS) {
            winner = IZombieNetworkState.Role.PLANTS;
            phase = IZombieNetworkState.Phase.PLANTS_WON;
        }
    }

    private int countBrains() {
        int count = 0;
        for (boolean eaten : eatenBrains) {
            if (eaten) {
                count++;
            }
        }
        return count;
    }

    private MutableUnit nearestZombie(int row, float x) {
        MutableUnit result = null;
        for (MutableUnit zombie : zombies.values()) {
            if (zombie.row != row || zombie.x < x) {
                continue;
            }
            if (result == null || zombie.x < result.x) {
                result = zombie;
            }
        }
        return result;
    }

    private MutableUnit nearestPlant(int row, float x) {
        MutableUnit result = null;
        for (MutableUnit plant : plants.values()) {
            if (plant.row != row || plant.x > x) {
                continue;
            }
            if (result == null || plant.x > result.x) {
                result = plant;
            }
        }
        return result;
    }

    private boolean canAct(IZombieNetworkState.Role role, IZombieNetworkState.Phase required) {
        return role != null && phase == required
            && ((role == IZombieNetworkState.Role.PLANTS && plantsJoined)
                || (role == IZombieNetworkState.Role.ZOMBIES && zombiesJoined));
    }

    private static Card card(List<Card> cards, String type) {
        if (type == null) {
            return null;
        }
        for (Card card : cards) {
            if (card.type().equalsIgnoreCase(type.trim())) {
                return card;
            }
        }
        return null;
    }

    private static boolean validReaction(String category, String value) {
        if (category == null || value == null) {
            return false;
        }
        String normalized = category.trim().toUpperCase(java.util.Locale.ROOT);
        List<String> allowed = switch (normalized) {
            case "TEXT" -> textReactions();
            case "EMOJI" -> emojiReactions();
            case "STICKER" -> stickerReactions();
            default -> List.of();
        };
        return allowed.contains(value.trim());
    }

    public record Card(String type, int cost, int hp, float damage, float speed, float income) {
        public Card {
            if (type == null || type.isBlank() || cost <= 0 || hp <= 0) {
                throw new IllegalArgumentException("Invalid I, Zombie card.");
            }
        }
    }

    public record ActionResult(boolean success, String code, String message) {
        public static ActionResult ok(String message) {
            return new ActionResult(true, "OK", message);
        }

        public static ActionResult error(String code, String message) {
            return new ActionResult(false, code, message);
        }
    }

    public synchronized ActionResult sendMessage(
        IZombieNetworkState.Role role,
        String receiver,
        String type,
        String contentId,
        String soundId
    ) {
        if (!canAct(role, IZombieNetworkState.Phase.PLAYING)) {
            return ActionResult.error("NOT_ALLOWED", "Messages are available while the match is running.");
        }

        String sender = role == IZombieNetworkState.Role.PLANTS ? "PLANTS" : "ZOMBIES";

        GameMessage msg = new GameMessage(
            sender,
            receiver,
            MessageType.valueOf(type),
            contentId,
            soundId
        );

        chatHistory.add(msg);
        if (chatHistory.size() > MAX_CHAT_HISTORY) {
            chatHistory.remove(0);
        }

        revision++;
        return ActionResult.ok("Message sent.");
    }

    private static final class MutableUnit {
        private final long id;
        private final String type;
        private final int row;
        private float x;
        private float hp;
        private final float maxHp;
        private float cooldown;

        private MutableUnit(long id, String type, int row, float x, float hp) {
            this.id = id;
            this.type = type;
            this.row = row;
            this.x = x;
            this.hp = hp;
            this.maxHp = hp;
        }

        private IZombieNetworkState.Unit snapshot() {
            return new IZombieNetworkState.Unit(id, type, row, x, row, hp, maxHp);
        }
    }

    private static final class MutableProjectile {
        private final long id;
        private final int row;
        private float x;
        private final float damage;

        private MutableProjectile(long id, int row, float x, float damage) {
            this.id = id;
            this.row = row;
            this.x = x;
            this.damage = damage;
        }

        private IZombieNetworkState.Projectile snapshot() {
            return new IZombieNetworkState.Projectile(id, row, x, row, damage);
        }
    }
}
