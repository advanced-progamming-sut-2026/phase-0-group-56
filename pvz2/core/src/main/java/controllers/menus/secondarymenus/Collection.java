package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.datacontroller.PlantData;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.entity.ZombieRegistry;
import models.entity.Zombie;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import view.HomeView;

import java.util.ArrayList;
import java.util.List;

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
