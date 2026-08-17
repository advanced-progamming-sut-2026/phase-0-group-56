package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import view.GreenHouseView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shop implements Menu {
    private static String currentDailyPlant = "PEASHOOTER";

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Shop menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new GreenHouseView());
        return "Returned to GreenHouse Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Shop Menu ---";
    }

    public String purchase(String itemName, int count) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in first.";
        }
        if (count <= 0) {
            return "Error: purchase count must be positive.";
        }

        return switch (itemName.toLowerCase()) {
            case "pot" -> buyPots(user, count);
            case "plantfood" -> buyPlantFood(user, count);
            case "exchange" -> exchangeCurrency(user, count);
            case "daily" -> buyDaily(user);
            default -> "Error: Invalid item.";
        };
    }

    private String buyPots(User user, int count) {
        int cost = 2000 * count;
        if (user.getUnlockedPots() + count > 20) {
            return "Error: Maximum of 20 pots reached.";
        }
        if (user.getCoins() < cost) {
            return "Error: Not enough coins.";
        }
        user.addCoins(-cost);
        user.addUnlockedPots(count);
        Data.saveUser();
        return count + " pot(s) unlocked successfully.";
    }

    private String buyPlantFood(User user, int count) {
        int cost = 3 * count;
        if (user.getPlantFoods() + count > 3) {
            return "Error: Maximum of 3 Plant Foods can be stored.";
        }
        if (user.getDiamonds() < cost) {
            return "Error: Not enough diamonds.";
        }
        user.addDiamonds(-cost);
        user.addPlantFoods(count);
        Data.saveUser();
        return count + " Plant Food(s) purchased successfully.";
    }

    private String exchangeCurrency(User user, int count) {
        int cost = 5 * count;
        if (user.getDiamonds() < cost) {
            return "Error: Not enough diamonds.";
        }
        user.addDiamonds(-cost);
        user.addCoins(500 * count);
        Data.saveUser();
        return "Currency exchanged. Gained " + (500 * count) + " coins.";
    }

    private String buyDaily(User user) {
        setDailyOffer();
        String today = LocalDate.now().toString();
        if (today.equals(user.getLastDailyPurchaseDate())) {
            return "Error: today's offer has already been purchased.";
        }
        if (user.getCoins() < 1600) {
            return "Error: Not enough coins (1600 required).";
        }
        user.addCoins(-1600);
        user.addSpecificSeed(currentDailyPlant, 10);
        user.setLastDailyPurchaseDate(today);
        Data.saveUser();
        return "Daily offer purchased: 10x " + currentDailyPlant + " seeds.";
    }

    public boolean canPurchase(int cost, String currency) {
        User user = Data.getCurrentUser();
        if (user == null || cost < 0) {
            return false;
        }
        if ("coin".equalsIgnoreCase(currency)) {
            return user.getCoins() >= cost;
        }
        if ("diamond".equalsIgnoreCase(currency)) {
            return user.getDiamonds() >= cost;
        }
        return false;
    }

    public String setDailyOffer() {
        String today = LocalDate.now().toString();
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Daily offer unavailable until login.";
        }

        if (!today.equals(user.getDailyOfferDate()) || user.getDailyOfferPlant().isBlank()) {
            List<PlantType> candidates = new ArrayList<>(user.getUnlockedPlants());
            if (candidates.isEmpty()) {
                candidates.add(PlantType.PEASHOOTER);
            }
            currentDailyPlant = candidates.get(new Random().nextInt(candidates.size())).name();
            user.setDailyOfferDate(today);
            user.setDailyOfferPlant(currentDailyPlant);
            Data.saveUser();
        } else {
            currentDailyPlant = user.getDailyOfferPlant();
        }
        return "10x " + currentDailyPlant + " seed packets - 1600 Coins (20% off)";
    }

    public String getCurrentDailyPlant() {
        setDailyOffer();
        return currentDailyPlant;
    }

    public String normalPurchase(String plantType) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }
        PlantType type;
        try {
            type = PlantType.valueOf(plantType.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return "Error: Unknown plant.";
        }
        if (!user.getUnlockedPlants().contains(type)) {
            return "Error: Specific seed packets can only be bought for unlocked plants.";
        }
        if (user.getDiamonds() < 5) {
            return "Error: Not enough diamonds (5 required).";
        }
        user.addDiamonds(-5);
        user.addSpecificSeed(type.name(), 10);
        Data.saveUser();
        return "Bought 10 " + type.name() + " seeds for 5 diamonds.";
    }

    public String randomPurchase() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: Please log in.";
        }
        if (user.getCoins() < 1000) {
            return "Error: Not enough coins (1000 required).";
        }
        user.addCoins(-1000);
        user.addRandomSeeds(5);
        Data.saveUser();
        return "Bought 5 random seed packets for 1000 coins.";
    }
}
