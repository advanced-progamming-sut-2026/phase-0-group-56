package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.datacontroller.PlantData;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.entity.PlantCategory;
import models.entity.ZombieRegistry;
import models.entity.Zombie;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import view.HomeView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Collection implements Menu {
    private static final int PURCHASE_COST = 2000;

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
        return result;
    }

    public List<ZombieRegistry.ZombieType> getAllZombies() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        return user.getZombieRegistry().getAllZombieTypes();
    }

    /**
     * Returns only the entries that the player has actually discovered.
     * Keeping this separate from {@link #getAllZombies()} preserves the
     * terminal command/API that lists every supported type while allowing the
     * Collection screen to hide undiscovered zombies completely.
     */
    public List<ZombieRegistry.ZombieType> getUnlockedZombies() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return List.of();
        }
        List<ZombieRegistry.ZombieType> result =
            new ArrayList<>(user.getZombieRegistry().getUnlockedZombies());
        result.sort(java.util.Comparator.comparingInt(ZombieRegistry.ZombieType::ordinal));
        return result;
    }

    /**
     * Reads the family/type stored in plants.json. Older save/data files may
     * not have a category, so a small deterministic fallback keeps filtering
     * usable instead of silently treating every plant as uncategorised.
     */
    public String getPlantCategory(PlantType type) {

        if (type == null) {
            return "UNKNOWN";
        }

        PlantCategory category = type.getCategory();

        if (category != null) {
            return category.name();
        }

        return "UNKNOWN";
    }

    private static String displayCategory(String raw) {
        if (raw == null) {
            return "UNKNOWN";
        }
        String display = raw.trim().replace('_', ' ').replaceAll("\\s+", " ");
        if (display.equalsIgnoreCase("Strike through")) {
            return "Strike Through";
        }
        if (display.equalsIgnoreCase("Wall nut")) {
            return "Wall Nut";
        }
        return display;
    }

    /** All distinct families in display order, suitable for a SelectBox. */
    public List<String> getPlantCategories() {
        Set<String> categories = new LinkedHashSet<>();
        for (PlantType type : getAllPlants()) {
            categories.add(getPlantCategory(type));
        }
        return new ArrayList<>(categories);
    }

    public String getZombieDescription(ZombieRegistry.ZombieType type) {
        if (type == null) {
            return "Unknown zombie.";
        }
        return switch (type) {
            case NORMAL -> "The standard zombie: slow, persistent, and always moving forward.";
            case CONEHEAD -> "A cone protects its head and gives this zombie extra durability.";
            case BUCKETHEAD -> "A bucket-headed zombie with heavy protection.";
            case BRICKHEAD -> "A reinforced Egyptian zombie carrying the strongest basic armor.";
            case KNIGHT -> "A heavily armoured knight that is difficult to stop.";
            case IMP -> "A small, quick zombie that can slip through a defence.";
            case GARGANTUAR -> "A giant zombie with enormous health and devastating attacks.";
            case ALLSTAR -> "A fast modern zombie that can charge through the lawn.";
            case ARCADe -> "An arcade zombie that pushes and interacts with arcade machinery.";
            case PARASOL -> "A parasol protects this zombie from many incoming projectiles.";
            case TURQUOISE -> "A turquoise crystal-skull zombie with powerful special abilities.";
            case PROSPECTOR -> "A prospector that can cross the lawn in the opposite direction.";
            case PIANIST -> "A piano-playing zombie whose music makes it a dangerous support unit.";
            case NEWSPAPER -> "Breaking its newspaper sends this zombie into a much faster rage.";
            case BARREL_ROLLER -> "A pirate zombie that rolls a barrel and releases imps.";
            case RA -> "Ra steals sun while advancing through the lawn.";
            case EXPLORER -> "An explorer carrying a torch that can ignite and destroy plants.";
            case TOMB_RAISER -> "A tomb raiser summons graves to reinforce the attack.";
            case DODO_RIDER -> "A dodo rider can fly over obstacles and bypass normal routes.";
            case HUNTER -> "An ice-age hunter throws freezing projectiles from a distance.";
            case TROGLOBITE -> "A troglobite pushes ice and changes the shape of the battlefield.";
            case FISHERMAN -> "A fisherman pulls plants away from their defensive positions.";
            case SNORKEL -> "A snorkel zombie hides in water while approaching the house.";
            case OCTOPUS -> "An octopus zombie attacks from range with disabling projectiles.";
            case JUGGLER -> "A juggler deflects incoming projectiles while advancing.";
            case WIZARD -> "A wizard can transform plants into harmless cats.";
            case KING -> "A dark king grants armour to other zombies.";
            case IMP_DRAGON -> "A dragon imp breathes fire and is resistant to ordinary tactics.";
        };
    }

    private static String fallbackPlantCategory(PlantType type) {
        return switch (type) {
            case SUNFLOWER, TWIN_SUNFLOWER, SUN_SHROOM, PRIMAL_SUNFLOWER,
                 GOLD_BLOOM, ENLIGHTEN_MINT, MARIGOLD -> "Sun Producer";
            case PEASHOOTER, REPEATER, THREEPEATER, SNOW_PEA, ROTOBAGA,
                 PEA_POD, SPLIT_PEA, CITRON, BOWLING_BULB, FIRE_PEASHOOTER,
                 STARFRUIT, GOO_PEASHOOTER, MEGA_GATLING_PEA, SEA_SHROOM,
                 PUFF_SHROOM, APPEASE_MINT -> "Shooter";
            case CAULIPOWER, ELECTRIC_BLUEBERRY, MAGNET_SHROOM, CAT_TAIL,
                 CATTAIL_MINT -> "Homing";
            case CACTUS, FUM_SHROOM, PIERCE_MINT -> "Strike Through";
            case CABBAGE_PULT, KERNEL_PULT, MELON_PULT, WINTER_MELON,
                 PEPPER_PULT, ARMA_MINT -> "Lobber";
            case POTATO_MINE, PRIMAL_POTATO_MINE, CHERRY_BOMB, SQUASH,
                 GRAPESHOT, JALAPENO, DOOM_SHROOM, TANGLE_KELP,
                 ICEBERG_LETTUCE, ICE_SHROOM, HOT_POTATO, GRAVE_BUSTER,
                 BOMBARD_MINT -> "Explosive";
            case BONK_CHOY, PHAT_BEET, CHOMPER, WASABI_WHIP, KIWIBEAST,
                 ENFORCE_MINT -> "Melee";
            case WALL_NUT, TALL_NUT, ENDURIAN, GARLIC, SWEET_POTATO,
                 EXPLODE_O_NUT, PUMPKIN, SUN_BEAN, REINFORCE_MINT -> "Wall Nut";
            case TORCHWOOD, HYPNO_SHROOM, IMITATER, LILY_PAD,
                 ENCHANT_MINT -> "Modifier";
        };
    }


    public Zombie createZombiePreview(ZombieRegistry.ZombieType type) {
        if (type == null) {
            return null;
        }
        String factoryName = switch (type) {
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
        return ZombieFactory.createZombie(factoryName);
    }

    public boolean isPlantUnlocked(PlantType type) {
        User user = Data.getCurrentUser();
        return user != null && user.getUnlockedPlants().contains(type);
    }

    public boolean isZombieUnlocked(ZombieRegistry.ZombieType type) {
        User user = Data.getCurrentUser();
        return user != null && user.getZombieRegistry().isUnlocked(type);
    }

    public int getPlantLevel(PlantType type) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return 1;
        }
        return user.getLevels().getOrDefault(type, 1);
    }

    public int getSeedCount(PlantType type) {
        User user = Data.getCurrentUser();
        return user == null ? 0 : user.getSpecificSeedCount(type.name());
    }

    public int getRequiredSeeds(PlantType type) {
        return 10 * Math.max(1, getPlantLevel(type));
    }

    public int getRequiredCoins(PlantType type) {
        return 500 * Math.max(1, getPlantLevel(type));
    }

    public boolean canUpgrade(PlantType type) {
        User user = Data.getCurrentUser();
        if (user == null || !isPlantUnlocked(type)) {
            return false;
        }
        int level = getPlantLevel(type);
        return level < 5
            && user.getCoins() >= getRequiredCoins(type)
            && getSeedCount(type) >= getRequiredSeeds(type);
    }

    public PlantData getPlantData(PlantType type) {
        return Data.getPlants() == null ? null : Data.getPlants().get(type);
    }

    public String upgradePlant(PlantType plantType) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }
        if (!isPlantUnlocked(plantType)) {
            return "Error: this plant is locked.";
        }
        int level = getPlantLevel(plantType);
        if (level >= 5) {
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

        user.consumeSpecificSeeds(plantType.name(), seedCost);
        user.addCoins(-coinCost);
        user.getLevels().put(plantType, level + 1);
        News.pushNewsToUser(user, plantType.name() + " upgraded to level " + (level + 1) + ".");
        Data.saveUser();
        return plantType.name() + " upgraded to level " + (level + 1) + ".";
    }

    public String buyPlant(String plantName) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }

        PlantType type;
        try {
            type = PlantType.valueOf(plantName.toUpperCase());
        } catch (IllegalArgumentException exception) {
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
        News.pushNewsToUser(user, "New plant unlocked: " + type.name());
        Data.saveUser();
        return type.name() + " purchased successfully.";
    }

    public String showunlockedPlant() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        return user.getUnlockedPlants().toString();
    }

    public String showunlockedZombie() {
        User user = Data.getCurrentUser();
        return user == null ? "Error: User not found." : user.getZombieRegistry().getUnlockedZombies().toString();
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
}
