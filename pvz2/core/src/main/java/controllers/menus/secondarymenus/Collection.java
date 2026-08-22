package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.datacontroller.PlantData;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.entity.Zombie;
import models.entity.ZombieRegistry;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import view.HomeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read/write boundary for the Collection screen. */
public class Collection implements Menu {
    public static final int PURCHASE_COST = 2000;
    public static final int MAX_PLANT_LEVEL = 5;

    private static final Map<PlantType, String> PLANT_FAMILIES = buildPlantFamilies();
    private static final List<String> FAMILY_FILTERS = buildFamilyFilters();

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Collection menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Collection Menu ---";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Home Menu.";
    }

    public List<PlantType> getAllPlants() {
        ArrayList<PlantType> result = new ArrayList<>();
        for (PlantType type : PlantType.values()) {
            if (type != PlantType.MARIGOLD) {
                result.add(type);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Stable enum order; HashMap iteration order from ZombieRegistry is not used. */
    public List<ZombieRegistry.ZombieType> getAllZombies() {
        return List.of(ZombieRegistry.ZombieType.values());
    }

    public List<String> getPlantFamilies() {
        return FAMILY_FILTERS;
    }

    public String getPlantFamily(PlantType type) {
        return type == null ? "OTHER" : PLANT_FAMILIES.getOrDefault(type, "OTHER");
    }

    public boolean isPlantUnlocked(PlantType type) {
        User user = Data.getCurrentUser();
        return type != null && user != null && user.getUnlockedPlants().contains(type);
    }

    public boolean isZombieUnlocked(ZombieRegistry.ZombieType type) {
        User user = Data.getCurrentUser();
        return type != null && user != null && user.getZombieRegistry().isUnlocked(type);
    }

    public int getPlantLevel(PlantType type) {
        User user = Data.getCurrentUser();
        if (type == null || user == null || !isPlantUnlocked(type)) {
            return 1;
        }
        return Math.max(1, Math.min(MAX_PLANT_LEVEL, user.getLevels().getOrDefault(type, 1)));
    }

    public int getSeedCount(PlantType type) {
        User user = Data.getCurrentUser();
        return type == null || user == null ? 0 : user.getSpecificSeedCount(type.name());
    }

    public int getRequiredSeeds(PlantType type) {
        return 10 * getPlantLevel(type);
    }

    public int getRequiredCoins(PlantType type) {
        return 500 * getPlantLevel(type);
    }

    public boolean isAtMaximumLevel(PlantType type) {
        return isPlantUnlocked(type) && getPlantLevel(type) >= MAX_PLANT_LEVEL;
    }

    public boolean canUpgrade(PlantType type) {
        User user = Data.getCurrentUser();
        if (type == null || user == null || !isPlantUnlocked(type) || isAtMaximumLevel(type)) {
            return false;
        }
        return user.getCoins() >= getRequiredCoins(type)
            && getSeedCount(type) >= getRequiredSeeds(type);
    }

    public PlantData getPlantData(PlantType type) {
        return type == null || Data.getPlants() == null ? null : Data.getPlants().get(type);
    }

    public String upgradePlant(PlantType plantType) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }
        if (plantType == null) {
            return "Error: unknown plant.";
        }
        if (!isPlantUnlocked(plantType)) {
            return "Error: this plant is locked.";
        }

        int currentLevel = getPlantLevel(plantType);
        if (currentLevel >= MAX_PLANT_LEVEL) {
            return "Error: this plant is already at maximum level.";
        }

        int seedCost = getRequiredSeeds(plantType);
        int coinCost = getRequiredCoins(plantType);
        if (getSeedCount(plantType) < seedCost) {
            return "Error: not enough seed packets. Need " + seedCost + ".";
        }
        if (user.getCoins() < coinCost) {
            return "Error: not enough coins. Need " + coinCost + ".";
        }

        int nextLevel = currentLevel + 1;
        user.consumeSpecificSeeds(plantType.name(), seedCost);
        user.addCoins(-coinCost);
        user.getLevels().put(plantType, nextLevel);
        News.queueNewsForUser(
            user,
            plantType.name() + " upgraded to level " + nextLevel + "."
        );
        Data.saveUser();
        return plantType.name() + " upgraded to level " + nextLevel + ".";
    }

    public String buyPlant(PlantType type) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }
        if (type == null) {
            return "Error: unknown plant.";
        }
        if (type == PlantType.MARIGOLD) {
            return "Error: Marigold is a greenhouse plant and cannot be purchased here.";
        }
        if (isPlantUnlocked(type)) {
            return "Error: You already own this plant.";
        }
        if (user.getCoins() < PURCHASE_COST) {
            return "Error: Not enough coins. " + PURCHASE_COST + " coins are required.";
        }

        user.addCoins(-PURCHASE_COST);
        user.unlockPlant(type);
        News.queueNewsForUser(user, "New plant unlocked: " + type.name());
        Data.saveUser();
        return type.name() + " purchased successfully.";
    }

    public String buyPlant(String plantName) {
        return buyPlant(parsePlantType(plantName));
    }

    public ZombiePreview getZombiePreview(ZombieRegistry.ZombieType type) {
        if (type == null) {
            return null;
        }

        Zombie zombie = ZombieFactory.createZombie(factoryName(type));
        if (zombie == null) {
            return null;
        }

        return new ZombiePreview(
            zombie.getMaxHp(),
            zombie.getDamage(),
            Math.abs(zombie.getSpeed()),
            zombie.getCost(),
            zombie.hasArmor() ? zombie.getArmors().toString() : "NONE",
            zombie.getAbilities().isEmpty() ? "NONE" : zombie.getAbilities().toString()
        );
    }

    /** Kept for terminal compatibility. CollectionView uses getZombiePreview(). */
    public Zombie createZombiePreview(ZombieRegistry.ZombieType type) {
        return type == null ? null : ZombieFactory.createZombie(factoryName(type));
    }

    public String showunlockedPlant() {
        User user = Data.getCurrentUser();
        return user == null ? "Error: User not found." : user.getUnlockedPlants().toString();
    }

    public String showunlockedZombie() {
        User user = Data.getCurrentUser();
        return user == null
            ? "Error: User not found."
            : user.getZombieRegistry().getUnlockedZombies().toString();
    }

    public String showAllPlants() {
        return getAllPlants().toString();
    }

    public String showAllZombies() {
        return getAllZombies().toString();
    }

    public String showZombie(String zombieName) {
        return "Zombie: " + zombieName;
    }

    public String showPlant(String plantName) {
        return "Plant: " + plantName;
    }

    private PlantType parsePlantType(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }

        String normalized = plantName
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return PlantType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            String compact = normalized.replace("_", "");
            for (PlantType type : PlantType.values()) {
                if (type.name().replace("_", "").equals(compact)) {
                    return type;
                }
            }
            return null;
        }
    }

    private static String factoryName(ZombieRegistry.ZombieType type) {
        return switch (type) {
            case NORMAL -> "normal";
            case CONEHEAD -> "cone";
            case BUCKETHEAD -> "bucket";
            case BRICKHEAD -> "brick";
            case KNIGHT -> "knight";
            case IMP -> "imp";
            case GARGANTUAR -> "gargantuar";
            case ALLSTAR -> "allstar";
            case ARCADe -> "arcade";
            case PARASOL -> "parasol";
            case TURQUOISE -> "turquoise";
            case PROSPECTOR -> "prospector";
            case PIANIST -> "piano";
            case NEWSPAPER -> "newspaper";
            case BARREL_ROLLER -> "barrel";
            case RA -> "ra";
            case EXPLORER -> "explorer";
            case TOMB_RAISER -> "tombraiser";
            case DODO_RIDER -> "dodo";
            case HUNTER -> "hunter";
            case TROGLOBITE -> "troglobite";
            case FISHERMAN -> "fisherman";
            case SNORKEL -> "snorkel";
            case OCTOPUS -> "octopus";
            case JUGGLER -> "juggler";
            case WIZARD -> "wizard";
            case KING -> "king";
            case IMP_DRAGON -> "dragon_imp";
        };
    }

    private static Map<PlantType, String> buildPlantFamilies() {
        EnumMap<PlantType, String> result = new EnumMap<>(PlantType.class);
        registerRange(result, "SUN PRODUCER", PlantType.SUNFLOWER, PlantType.GOLD_BLOOM);
        registerRange(result, "SHOOTER", PlantType.PEASHOOTER, PlantType.PUFF_SHROOM);
        registerRange(result, "EXPLOSIVE", PlantType.POTATO_MINE, PlantType.TANGLE_KELP);
        registerRange(result, "CONTROL", PlantType.ICEBERG_LETTUCE, PlantType.GRAVE_BUSTER);
        registerRange(result, "LOBBER", PlantType.CABBAGE_PULT, PlantType.PEPPER_PULT);
        registerRange(result, "STRIKE THROUGH", PlantType.CACTUS, PlantType.FUM_SHROOM);
        registerRange(result, "MELEE", PlantType.BONK_CHOY, PlantType.KIWIBEAST);
        registerRange(result, "DEFENSIVE", PlantType.WALL_NUT, PlantType.SUN_BEAN);
        registerRange(result, "MODIFIER", PlantType.TORCHWOOD, PlantType.LILY_PAD);
        registerRange(result, "HOMING", PlantType.CAULIPOWER, PlantType.MAGNET_SHROOM);
        registerRange(result, "MINT", PlantType.ENLIGHTEN_MINT, PlantType.CATTAIL_MINT);
        return Collections.unmodifiableMap(result);
    }

    private static List<String> buildFamilyFilters() {
        ArrayList<String> result = new ArrayList<>();
        result.add("ANY");
        for (PlantType type : PlantType.values()) {
            if (type == PlantType.MARIGOLD) {
                continue;
            }
            String family = PLANT_FAMILIES.getOrDefault(type, "OTHER");
            if (!result.contains(family)) {
                result.add(family);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static void registerRange(
        EnumMap<PlantType, String> target,
        String family,
        PlantType first,
        PlantType last
    ) {
        PlantType[] values = PlantType.values();
        for (int index = first.ordinal(); index <= last.ordinal(); index++) {
            target.put(values[index], family);
        }
    }

    public static final class ZombiePreview {
        private final float health;
        private final float damage;
        private final float speed;
        private final int waveCost;
        private final String armor;
        private final String abilities;

        private ZombiePreview(
            float health,
            float damage,
            float speed,
            int waveCost,
            String armor,
            String abilities
        ) {
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.waveCost = waveCost;
            this.armor = armor;
            this.abilities = abilities;
        }

        public float getHealth() { return health; }
        public float getDamage() { return damage; }
        public float getSpeed() { return speed; }
        public int getWaveCost() { return waveCost; }
        public String getArmor() { return armor; }
        public String getAbilities() { return abilities; }
    }
}
