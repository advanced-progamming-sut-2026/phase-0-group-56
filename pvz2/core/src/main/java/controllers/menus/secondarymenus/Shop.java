package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import view.HomeView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Shop implements Menu {
    public static final int POT_PRICE = 2000;
    public static final int MAX_POTS = 20;

    public static final int PLANT_FOOD_PRICE = 3;
    public static final int MAX_PLANT_FOODS = 3;

    public static final int RANDOM_SEED_PRICE = 1000;
    public static final int RANDOM_SEED_PACKETS = 5;

    public static final int SPECIFIC_SEED_PRICE = 5;
    public static final int SPECIFIC_SEED_PACKETS = 10;

    public static final int EXCHANGE_DIAMOND_COST = 5;
    public static final int EXCHANGE_COIN_REWARD = 500;

    public static final int DAILY_OFFER_PRICE = 1600;
    public static final int DAILY_OFFER_PACKETS = 10;

    private final Random random;

    public Shop() {
        this(new Random());
    }

    public Shop(Random random) {
        this.random = random == null
            ? new Random()
            : random;
    }

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Shop menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Shop Menu ---";
    }

    public String purchase(String itemName, int count) {
        return purchase(itemName, count, null);
    }

    public String purchase(
        String itemName,
        int count,
        String plantType
    ) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in first.";
        }

        if (count <= 0) {
            return "Error: purchase count must be positive.";
        }

        String normalizedItem = normalizeItemName(itemName);

        return switch (normalizedItem) {
            case "pot", "pots" ->
                buyPots(user, count);

            case "plantfood", "plantfoods" ->
                buyPlantFood(user, count);

            case "randomseed", "randomseeds",
                 "randomseedpacket", "randomseedpackets" ->
                buyRandomSeeds(user, count);

            case "specificseed", "specificseeds",
                 "specificseedpacket", "specificseedpackets" ->
                buySpecificSeeds(user, plantType, count);

            case "exchange", "currencyexchange" ->
                exchangeCurrency(user, count);

            case "daily", "dailyoffer" -> {
                if (count != 1) {
                    yield "Error: the daily offer can only be purchased once.";
                }

                yield buyDaily(user);
            }

            default -> "Error: Invalid item.";
        };
    }

    public boolean canPurchase(int cost, String currency) {
        User user = Data.getCurrentUser();

        if (user == null || cost < 0 || currency == null) {
            return false;
        }

        if ("coin".equalsIgnoreCase(currency)
            || "coins".equalsIgnoreCase(currency)) {
            return user.getCoins() >= cost;
        }

        if ("diamond".equalsIgnoreCase(currency)
            || "diamonds".equalsIgnoreCase(currency)
            || "gem".equalsIgnoreCase(currency)
            || "gems".equalsIgnoreCase(currency)) {
            return user.getDiamonds() >= cost;
        }

        return false;
    }

    public String setDailyOffer() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Daily offer unavailable until login.";
        }

        boolean changed = ensureDailyOffer(user);

        if (changed) {
            Data.saveUser();
        }

        String plantName = user.getDailyOfferPlant();

        if (plantName.isBlank()) {
            return "Daily offer unavailable: no unlocked plants.";
        }

        return DAILY_OFFER_PACKETS
            + "x "
            + plantName
            + " seed packets - "
            + DAILY_OFFER_PRICE
            + " Coins (20% off)"
            + (isDailyOfferPurchased()
            ? " - PURCHASED TODAY"
            : "");
    }

    public String getCurrentDailyPlant() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "";
        }

        boolean changed = ensureDailyOffer(user);

        if (changed) {
            Data.saveUser();
        }

        return user.getDailyOfferPlant();
    }

    public boolean isDailyOfferPurchased() {
        User user = Data.getCurrentUser();

        return user != null
            && today().equals(
            user.getLastDailyPurchaseDate()
        );
    }

    public String normalPurchase(String plantType) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in.";
        }

        return buySpecificSeeds(
            user,
            plantType,
            1
        );
    }

    public String randomPurchase() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in.";
        }

        return buyRandomSeeds(user, 1);
    }

    private String buyPots(User user, int count) {
        int remainingCapacity =
            Math.max(0, MAX_POTS - user.getUnlockedPots());

        if (count > remainingCapacity) {
            return "Error: Maximum of 20 pots reached.";
        }

        long cost = (long) POT_PRICE * count;

        if (!hasCoins(user, cost)) {
            return "Error: Not enough coins. Need " + cost + ".";
        }

        user.addCoins(-(int) cost);
        user.addUnlockedPots(count);
        Data.saveUser();

        return count + " pot(s) unlocked successfully.";
    }

    private String buyPlantFood(User user, int count) {
        int remainingCapacity =
            Math.max(0, MAX_PLANT_FOODS - user.getPlantFoods());

        if (count > remainingCapacity) {
            return "Error: Maximum of 3 Plant Foods can be stored.";
        }

        long cost = (long) PLANT_FOOD_PRICE * count;

        if (!hasDiamonds(user, cost)) {
            return "Error: Not enough diamonds. Need " + cost + ".";
        }

        user.addDiamonds(-(int) cost);
        user.addPlantFoods(count);
        Data.saveUser();

        return count + " Plant Food(s) purchased successfully.";
    }

    private String exchangeCurrency(User user, int count) {
        long diamondCost =
            (long) EXCHANGE_DIAMOND_COST * count;

        long coinReward =
            (long) EXCHANGE_COIN_REWARD * count;

        if (!hasDiamonds(user, diamondCost)) {
            return "Error: Not enough diamonds. Need "
                + diamondCost
                + ".";
        }

        if (coinReward > Integer.MAX_VALUE - (long) user.getCoins()) {
            return "Error: Coin wallet capacity exceeded.";
        }

        user.addDiamonds(-(int) diamondCost);
        user.addCoins((int) coinReward);
        Data.saveUser();

        return "Currency exchanged. Gained "
            + coinReward
            + " coins.";
    }

    private String buyRandomSeeds(User user, int count) {
        PlantType selectedPlant =
            chooseRandomUnlockedPlant(user);

        if (selectedPlant == null) {
            return "Error: No unlocked plant is available for seed packets.";
        }

        long cost = (long) RANDOM_SEED_PRICE * count;
        long packets = (long) RANDOM_SEED_PACKETS * count;

        if (!hasCoins(user, cost)) {
            return "Error: Not enough coins. Need " + cost + ".";
        }

        if (!canAddSeeds(user, selectedPlant, packets)) {
            return "Error: Seed packet inventory limit exceeded.";
        }

        user.addCoins(-(int) cost);
        user.addSpecificSeed(
            selectedPlant.name(),
            (int) packets
        );
        Data.saveUser();

        return "Bought "
            + packets
            + " "
            + selectedPlant.name()
            + " seed packet(s) for "
            + cost
            + " coins.";
    }

    private String buySpecificSeeds(
        User user,
        String plantName,
        int count
    ) {
        PlantType type = parsePlantType(plantName);

        if (type == null) {
            return "Error: Unknown plant.";
        }

        if (!user.getUnlockedPlants().contains(type)) {
            return "Error: Specific seed packets can only be bought for unlocked plants.";
        }

        long diamondCost =
            (long) SPECIFIC_SEED_PRICE * count;

        long packets =
            (long) SPECIFIC_SEED_PACKETS * count;

        if (!hasDiamonds(user, diamondCost)) {
            return "Error: Not enough diamonds. Need "
                + diamondCost
                + ".";
        }

        if (!canAddSeeds(user, type, packets)) {
            return "Error: Seed packet inventory limit exceeded.";
        }

        user.addDiamonds(-(int) diamondCost);
        user.addSpecificSeed(
            type.name(),
            (int) packets
        );
        Data.saveUser();

        return "Bought "
            + packets
            + " "
            + type.name()
            + " seed packet(s) for "
            + diamondCost
            + " diamonds.";
    }

    private String buyDaily(User user) {
        boolean offerChanged = ensureDailyOffer(user);
        String currentDate = today();

        if (currentDate.equals(user.getLastDailyPurchaseDate())) {
            saveOfferIfNeeded(offerChanged);
            return "Error: today's offer has already been purchased.";
        }

        PlantType type =
            parsePlantType(user.getDailyOfferPlant());

        if (type == null
            || !user.getUnlockedPlants().contains(type)) {
            saveOfferIfNeeded(offerChanged);
            return "Error: Daily offer is unavailable.";
        }

        if (!hasCoins(user, DAILY_OFFER_PRICE)) {
            saveOfferIfNeeded(offerChanged);
            return "Error: Not enough coins (1600 required).";
        }

        if (!canAddSeeds(user, type, DAILY_OFFER_PACKETS)) {
            saveOfferIfNeeded(offerChanged);
            return "Error: Seed packet inventory limit exceeded.";
        }

        user.addCoins(-DAILY_OFFER_PRICE);
        user.addSpecificSeed(
            type.name(),
            DAILY_OFFER_PACKETS
        );
        user.setLastDailyPurchaseDate(currentDate);
        Data.saveUser();

        return "Daily offer purchased: "
            + DAILY_OFFER_PACKETS
            + "x "
            + type.name()
            + " seeds.";
    }

    private boolean ensureDailyOffer(User user) {
        String currentDate = today();
        PlantType storedPlant =
            parsePlantType(user.getDailyOfferPlant());

        boolean storedOfferIsValid =
            currentDate.equals(user.getDailyOfferDate())
                && storedPlant != null
                && user.getUnlockedPlants().contains(storedPlant);

        if (storedOfferIsValid) {
            return false;
        }

        PlantType selectedPlant =
            chooseRandomUnlockedPlant(user);

        if (selectedPlant == null) {
            return false;
        }

        user.setDailyOfferDate(currentDate);
        user.setDailyOfferPlant(selectedPlant.name());

        return true;
    }

    private PlantType chooseRandomUnlockedPlant(User user) {
        List<PlantType> candidates = new ArrayList<>();

        for (PlantType type : user.getUnlockedPlants()) {
            if (type != null
                && type != PlantType.MARIGOLD
                && !candidates.contains(type)) {
                candidates.add(type);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(
            Comparator.comparing(PlantType::name)
        );

        return candidates.get(
            random.nextInt(candidates.size())
        );
    }

    private PlantType parsePlantType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String enumName = value
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return PlantType.valueOf(enumName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean canAddSeeds(
        User user,
        PlantType type,
        long packets
    ) {
        return packets >= 0
            && packets
            <= Integer.MAX_VALUE
            - (long) user.getSpecificSeedCount(
            type.name()
        );
    }

    private boolean hasCoins(User user, long amount) {
        return amount >= 0
            && amount <= user.getCoins();
    }

    private boolean hasDiamonds(User user, long amount) {
        return amount >= 0
            && amount <= user.getDiamonds();
    }

    private void saveOfferIfNeeded(boolean offerChanged) {
        if (offerChanged) {
            Data.saveUser();
        }
    }

    private String normalizeItemName(String value) {
        if (value == null) {
            return "";
        }

        return value
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "");
    }

    private String today() {
        return LocalDate.now().toString();
    }
}
