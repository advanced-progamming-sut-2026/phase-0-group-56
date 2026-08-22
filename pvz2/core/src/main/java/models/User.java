package models;

import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.utils.CredentialHasher;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class User implements Serializable, QuestObserver {
    private static final long serialVersionUID = 1L;

    private String name;
    private String nickname;
    private String email;
    private String passwordHash;
    private String gender;

    private int securityQuestionNumber;
    private String securityAnswer;

    private Chapters chapter;
    private Level level;
    private int levelId;

    private int coins = 0;
    private int diamonds = 0;
    private int highestScore = 0;
    private int gamesPlayed = 0;
    private int levelsPassed = 0;
    private int difficultyLevel = 3;
    private int vaseBreaker = 1;
    private int wallNutBowling = 1;
    private int IZombie = 1;
    private boolean isStayLoggedIn = false;

    private int gameSpeed = 1;
    private boolean showGrid = false;
    private boolean debugMode = false;

    private int minigamesWon = 0;
    private int dailyQuestsCompleted = 0;
    private int otherQuestsCompleted = 0;

    private int unlockedPots = 5;
    private int plantFoods = 0;
    private int randomSeeds = 0;
    private String lastDailyPurchaseDate = "";
    private String dailyOfferDate = "";
    private String dailyOfferPlant = "";
    private HashMap<String, Integer> specificSeeds;

    private ArrayList<PlantType> unlockedPlants;
    private HashMap<PlantType, Integer> levels;
    private ArrayList<String> unlockedPlantsNames;
    private ArrayList<String> unreadNews;
    private ArrayList<String> readNews;
    private ArrayList<PlantType> boostList;

    private ArrayList<models.Quest> activeQuests;
    private String questDailyDate = "";
    private int questCatalogVersion;
    private ZombieRegistry zombieRegistry;
    private GreenHouse greenHouse;

    public User(String name, String passwordHash, String nickname, String email, String gender) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.specificSeeds = new HashMap<>();
        this.unlockedPlantsNames = new ArrayList<>();
        this.unreadNews = new ArrayList<>();
        this.readNews = new ArrayList<>();
        this.boostList = new ArrayList<>();
        this.levelId = 1;
        this.chapter = Chapters.AncientEgypt;
        this.zombieRegistry = new ZombieRegistry();
        this.levels = new HashMap<>();

        this.unlockedPlants = new ArrayList<>(Arrays.asList(
            PlantType.PEASHOOTER,
            PlantType.SNOW_PEA,
            PlantType.REPEATER,
            PlantType.CHOMPER,
            PlantType.WALL_NUT
        ));

        for (PlantType plant : this.unlockedPlants) {
            this.levels.put(plant, 1);
            this.unlockedPlantsNames.add(plant.name());
        }

        this.greenHouse = new GreenHouse(this);
        this.activeQuests = models.QuestCatalog.createDefaultQuests(this);
        this.questDailyDate = LocalDate.now().toString();
        this.questCatalogVersion = models.QuestCatalog.CURRENT_VERSION;
    }

    @Override
    public void updateQuestProgress(String action, int amount) {
        QuestProgress.add(action, amount);
    }

    public ZombieRegistry getZombieRegistry() {
        if (zombieRegistry == null) {
            zombieRegistry = new ZombieRegistry();
        }
        return zombieRegistry;
    }

    public ArrayList<models.Quest> getActiveQuests() {
        if (activeQuests == null) {
            activeQuests = new ArrayList<>();
        }
        return activeQuests;
    }

    public void resetQuestsForTesting() {
        activeQuests = models.QuestCatalog.createDefaultQuests(this);
        questDailyDate = LocalDate.now().toString();
        questCatalogVersion = models.QuestCatalog.CURRENT_VERSION;
    }

    ArrayList<models.Quest> getStoredActiveQuests() {
        return activeQuests;
    }

    void setStoredActiveQuests(ArrayList<models.Quest> quests) {
        activeQuests = quests == null
            ? new ArrayList<>()
            : quests;
    }

    public String getQuestDailyDate() {
        return questDailyDate == null
            ? ""
            : questDailyDate;
    }

    public void setQuestDailyDate(String date) {
        questDailyDate = date == null
            ? ""
            : date;
    }

    public int getQuestCatalogVersion() {
        return questCatalogVersion;
    }

    public void setQuestCatalogVersion(int version) {
        questCatalogVersion = Math.max(0, version);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setSecurityQuestion(int questionNumber, String answer) {
        this.securityQuestionNumber = questionNumber;
        this.securityAnswer = CredentialHasher.hash(normalizeSecurityAnswer(answer));
    }

    public int getSecurityQuestionNumber() {
        return securityQuestionNumber;
    }

    public boolean checkSecurityAnswer(String answer) {
        String normalizedAnswer = normalizeSecurityAnswer(answer);
        boolean matches = CredentialHasher.matches(normalizedAnswer, securityAnswer);

        if (matches && !CredentialHasher.isSha256Hash(securityAnswer)) {
            securityAnswer = CredentialHasher.hash(normalizedAnswer);
        }

        return matches;
    }

    private String normalizeSecurityAnswer(String answer) {
        return answer == null ? "" : answer.trim();
    }

    public int getVaseBreaker() {
        return vaseBreaker;
    }

    public void setVaseBreaker(int vaseBreaker) {
        this.vaseBreaker = Math.min(3, Math.max(1, vaseBreaker));
    }

    public int getWallNutBowling() {
        return wallNutBowling;
    }

    public void setWallNutBowling(int wallNutBowling) {
        this.wallNutBowling = Math.min(3, Math.max(1, wallNutBowling));
    }

    public int getIZombie() {
        return IZombie;
    }

    public void setIZombie(int IZombie) {
        this.IZombie = Math.max(1, IZombie);
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins = Math.max(0, coins + amount);
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addDiamonds(int amount) {
        diamonds = Math.max(0, diamonds + amount);
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int score) {
        if (score > highestScore) {
            highestScore = score;
        }
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void incrementGamesPlayed() {
        gamesPlayed++;
    }

    public int getLevelsPassed() {
        return levelsPassed;
    }

    public void incrementLevelsPassed() {
        setLevelsPassed(levelsPassed + 1);
    }

    public int getDifficultyLevel() {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            difficultyLevel = 3;
        }
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = Math.max(1, Math.min(5, difficultyLevel));
    }

    public int getGameSpeed() {
        if (gameSpeed < 1 || gameSpeed > 3) {
            gameSpeed = 1;
        }
        return gameSpeed;
    }

    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = Math.max(1, Math.min(3, gameSpeed));
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isStayLoggedIn() {
        return isStayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        isStayLoggedIn = stayLoggedIn;
    }

    public int getUnlockedPots() {
        return unlockedPots;
    }

    public void addUnlockedPots(int amount) {
        unlockedPots = Math.max(0, Math.min(20, unlockedPots + amount));
        syncPotUnlocks();
    }

    private void syncPotUnlocks() {
        GreenHouse house = getGreenHouse();
        int unlocked = 0;
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                Pot pot = house.getPotByPosition(x, y);
                if (pot != null) {
                    boolean open = unlocked < unlockedPots;
                    pot.unlock(open);
                    unlocked++;
                }
            }
        }
    }

    public int getPlantFoods() {
        return plantFoods;
    }

    public void addPlantFoods(int amount) {
        plantFoods = Math.max(0, Math.min(3, plantFoods + amount));
    }

    public void setPlantFoods(int plantFoods) {
        this.plantFoods = Math.max(0, Math.min(3, plantFoods));
    }

    public int getRandomSeeds() {
        return randomSeeds;
    }

    public void addRandomSeeds(int amount) {
        randomSeeds = Math.max(0, randomSeeds + amount);
    }

    public String getLastDailyPurchaseDate() {
        return lastDailyPurchaseDate == null ? "" : lastDailyPurchaseDate;
    }

    public void setLastDailyPurchaseDate(String date) {
        lastDailyPurchaseDate = date == null ? "" : date;
    }

    public String getDailyOfferDate() {
        return dailyOfferDate == null ? "" : dailyOfferDate;
    }

    public void setDailyOfferDate(String dailyOfferDate) {
        this.dailyOfferDate = dailyOfferDate == null ? "" : dailyOfferDate;
    }

    public String getDailyOfferPlant() {
        return dailyOfferPlant == null ? "" : dailyOfferPlant;
    }

    public void setDailyOfferPlant(String dailyOfferPlant) {
        this.dailyOfferPlant = dailyOfferPlant == null ? "" : dailyOfferPlant;
    }

    public void setChapter(Chapters chapter) {
        this.chapter = chapter;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Chapters getChapter() {
        if (chapter == null) {
            chapter = Chapters.AncientEgypt;
        }
        return chapter;
    }

    public int getLevelId() {
        return levelId <= 0 ? 1 : levelId;
    }

    public void setLevelId(int levelId) {
        this.levelId = Math.max(1, levelId);
    }

    public void addSpecificSeed(String plantType, int amount) {
        if (specificSeeds == null) {
            specificSeeds = new HashMap<>();
        }
        String key = plantType == null ? "" : plantType.toUpperCase();
        specificSeeds.put(key, Math.max(0, specificSeeds.getOrDefault(key, 0) + amount));
    }

    public int getSpecificSeedCount(String plantType) {
        if (specificSeeds == null || plantType == null) {
            return 0;
        }
        return specificSeeds.getOrDefault(plantType.toUpperCase(), 0);
    }

    public void consumeSpecificSeeds(String plantType, int amount) {
        addSpecificSeed(plantType, -Math.max(0, amount));
    }

    public void addToBoostList(PlantType seedling) {
        if (seedling == null) {
            return;
        }
        if (boostList == null) {
            boostList = new ArrayList<>();
        }
        if (!boostList.contains(seedling)) {
            boostList.add(seedling);
        }
    }

    public ArrayList<String> getUnlockedPlantsNames() {
        if (unlockedPlantsNames == null) {
            unlockedPlantsNames = new ArrayList<>();
        }
        for (PlantType type : getUnlockedPlants()) {
            if (!unlockedPlantsNames.contains(type.name())) {
                unlockedPlantsNames.add(type.name());
            }
        }
        return unlockedPlantsNames;
    }

    public ArrayList<String> getUnreadNews() {
        if (unreadNews == null) {
            unreadNews = new ArrayList<>();
        }
        return unreadNews;
    }

    public ArrayList<String> getReadNews() {
        if (readNews == null) {
            readNews = new ArrayList<>();
        }
        return readNews;
    }

    public ArrayList<PlantType> getBoostList() {
        if (boostList == null) {
            boostList = new ArrayList<>();
        }
        return boostList;
    }

    public ArrayList<PlantType> getUnlockedPlants() {
        if (unlockedPlants == null) {
            unlockedPlants = new ArrayList<>();
        }
        return unlockedPlants;
    }

    public void setUnlockedPlants(ArrayList<PlantType> unlockedPlants) {
        this.unlockedPlants = unlockedPlants == null ? new ArrayList<>() : unlockedPlants;
    }

    public void unlockPlant(PlantType plantType) {
        if (plantType == null || plantType == PlantType.MARIGOLD) {
            return;
        }
        if (!getUnlockedPlants().contains(plantType)) {
            getUnlockedPlants().add(plantType);
        }
        if (!getUnlockedPlantsNames().contains(plantType.name())) {
            getUnlockedPlantsNames().add(plantType.name());
        }
        getLevels().putIfAbsent(plantType, 1);
    }

    public HashMap<PlantType, Integer> getLevels() {
        if (levels == null) {
            levels = new HashMap<>();
        }
        for (PlantType type : getUnlockedPlants()) {
            levels.putIfAbsent(type, 1);
        }
        return levels;
    }

    public void setLevels(HashMap<PlantType, Integer> levels) {
        this.levels = levels == null ? new HashMap<>() : levels;
    }

    public void setLevelsPassed(int levelsPassed) {
        this.levelsPassed = Math.max(0, Math.min(16, levelsPassed));
    }

    public GreenHouse getGreenHouse() {
        if (greenHouse == null) {
            greenHouse = new GreenHouse(this);
        }
        syncPotUnlocksWithoutRecursion();
        return greenHouse;
    }

    private void syncPotUnlocksWithoutRecursion() {
        if (greenHouse == null) {
            return;
        }
        int index = 0;
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                Pot pot = greenHouse.getPotByPosition(x, y);
                if (pot != null) {
                    pot.unlock(index < unlockedPots);
                }
                index++;
            }
        }
    }

    public int getMinigamesWon() {
        return minigamesWon;
    }

    public void incrementMinigamesWon() {
        minigamesWon++;
    }

    public int getDailyQuestsCompleted() {
        return dailyQuestsCompleted;
    }

    public void incrementDailyQuestsCompleted() {
        dailyQuestsCompleted++;
    }

    public int getOtherQuestsCompleted() {
        return otherQuestsCompleted;
    }

    public void incrementOtherQuestsCompleted() {
        otherQuestsCompleted++;
    }

    public String getLastProgressText() {
        return getChapter().name() + " - Level " + getLevelId();
    }

    public void updateProgress() {
        // Existing gameplay code may call this hook. Progress is represented by
        // chapter + levelId + levelsPassed and therefore needs no extra work here.
    }
}
