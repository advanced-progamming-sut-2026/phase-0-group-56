package controllers.datacontroller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import view.HomeView;
import view.LogInView;
import view.SignUpView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Data {
    private static final String SAVE_DIRECTORY_NAME = ".pvz2-group-56";
    private static final String USERS_FILE_NAME = "users_data.dat";
    private static final String USERS_BACKUP_FILE_NAME = "users_data.bak";
    private static final String USERS_TEMP_FILE_NAME = "users_data.tmp";
    private static final String LEGACY_USERS_FILE_NAME = "users_data.dat";

    private static ArrayList<User> allUsers = new ArrayList<>();
    private static User currentUser;
    private static User tempUser;

    private static HashMap<PlantType, PlantData> plants = new HashMap<>();
    private static HashMap<Chapters, ArrayList<Level>> allLevels = new HashMap<>();

    private Data() {
    }

    public static synchronized void saveUser() {
        Path saveDirectory = getSaveDirectory();
        Path usersFile = saveDirectory.resolve(USERS_FILE_NAME);
        Path backupFile = saveDirectory.resolve(USERS_BACKUP_FILE_NAME);
        Path temporaryFile = saveDirectory.resolve(USERS_TEMP_FILE_NAME);

        try {
            Files.createDirectories(saveDirectory);
            writeUsersToFile(temporaryFile);

            if (Files.exists(usersFile)) {
                Files.copy(
                    usersFile,
                    backupFile,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }

            replaceFile(temporaryFile, usersFile);
            logInfo("User data saved successfully. Users: " + allUsers.size());
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile);
            logError("Could not save user data.", exception);
        }
    }

    public static synchronized void deserializeUser() {
        allUsers = new ArrayList<>();

        Path saveDirectory = getSaveDirectory();
        Path usersFile = saveDirectory.resolve(USERS_FILE_NAME);
        Path backupFile = saveDirectory.resolve(USERS_BACKUP_FILE_NAME);
        Path legacyFile = Path.of(LEGACY_USERS_FILE_NAME);

        if (loadUsersFromFile(usersFile)) {
            return;
        }

        if (loadUsersFromFile(backupFile)) {
            restorePrimaryFile(backupFile, usersFile);
            return;
        }

        if (loadUsersFromFile(legacyFile)) {
            logInfo("Legacy user data was found and loaded.");
            saveUser();
            return;
        }

        allUsers = new ArrayList<>();
        logInfo("No saved users were found. Starting with an empty user list.");
    }

    public static void setUp() {
        deserializeUser();
        currentUser = null;
        tempUser = null;

        for (User user : allUsers) {
            if (user != null && user.isStayLoggedIn()) {
                currentUser = user;
                App.setScreen(new HomeView());
                logInfo("Automatically logged in user: " + user.getName());
                return;
            }
        }

        if (allUsers.isEmpty()) {
            App.setScreen(new SignUpView());
        } else {
            App.setScreen(new LogInView());
        }
    }

    public static void addUser(User user) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return;
        }

        if (isUsernameExists(user.getName())) {
            return;
        }

        allUsers.add(user);
        saveUser();
    }

    public static boolean isUsernameExists(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        for (User user : allUsers) {
            if (user != null
                && user.getName() != null
                && user.getName().equalsIgnoreCase(username)) {
                return true;
            }
        }

        return false;
    }

    public static User getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        for (User user : allUsers) {
            if (user != null
                && user.getName() != null
                && user.getName().equalsIgnoreCase(username)) {
                return user;
            }
        }

        return null;
    }

    public static void loadPlantsFromJson() {
        FileHandle file = Gdx.files.internal("plants.json");

        if (!file.exists()) {
            Gdx.app.error("Data", "plants.json not found in assets folder.");
            plants = new HashMap<>();
            return;
        }

        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            ArrayList<PlantData> plantsList =
                json.fromJson(ArrayList.class, PlantData.class, file);

            HashMap<PlantType, PlantData> loadedPlants = new HashMap<>();

            if (plantsList != null) {
                for (PlantData plant : plantsList) {
                    addLoadedPlant(loadedPlants, plant);
                }
            }

            plants = loadedPlants;
            Gdx.app.log("Data", "Plants loaded successfully: " + plants.size());
        } catch (Exception exception) {
            plants = new HashMap<>();
            Gdx.app.error("Data", "Error reading plants.json.", exception);
        }
    }

    private static void addLoadedPlant(
        HashMap<PlantType, PlantData> loadedPlants,
        PlantData plant
    ) {
        if (plant == null || plant.getName() == null || plant.getName().isBlank()) {
            Gdx.app.error("Data", "Skipping plant with a missing name.");
            return;
        }

        try {
            PlantType type = parsePlantType(plant.getName());
            loadedPlants.put(type, plant);
        } catch (IllegalArgumentException exception) {
            Gdx.app.error(
                "Data",
                "Unknown PlantType in plants.json: " + plant.getName(),
                exception
            );
        }
    }

    private static PlantType parsePlantType(String jsonName) {
        String enumName = jsonName
            .trim()
            .replace('-', '_')
            .replace(' ', '_')
            .replaceAll("_+", "_")
            .toUpperCase(Locale.ROOT);

        return PlantType.valueOf(enumName);
    }

    public static void loadLevelsFromJson() {
        FileHandle file = Gdx.files.internal("levels.json");

        if (!file.exists()) {
            Gdx.app.error("Data", "levels.json not found in assets folder.");
            allLevels = new HashMap<>();
            return;
        }

        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            ArrayList<Level> levelsList =
                json.fromJson(ArrayList.class, Level.class, file);

            HashMap<Chapters, ArrayList<Level>> loadedLevels = new HashMap<>();

            if (levelsList != null) {
                for (Level level : levelsList) {
                    addLoadedLevel(loadedLevels, level);
                }
            }

            allLevels = loadedLevels;

            Gdx.app.log(
                "Data",
                "Levels loaded successfully. Chapters: " + allLevels.size()
            );
        } catch (Exception exception) {
            allLevels = new HashMap<>();
            Gdx.app.error("Data", "Error reading levels.json.", exception);
        }
    }

    private static void addLoadedLevel(
        HashMap<Chapters, ArrayList<Level>> loadedLevels,
        Level level
    ) {
        if (level == null || level.getChapters() == null) {
            Gdx.app.error("Data", "Skipping a level with no chapter.");
            return;
        }

        Chapters chapter = level.getChapters();
        loadedLevels.putIfAbsent(chapter, new ArrayList<>());
        loadedLevels.get(chapter).add(level);
    }

    public static HashMap<Chapters, ArrayList<Level>> getAllLevels() {
        return allLevels;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setTempUser(User user) {
        tempUser = user;
    }

    public static User getTempUser() {
        return tempUser;
    }

    public static ArrayList<User> getAllUsers() {
        return allUsers;
    }

    public static HashMap<PlantType, PlantData> getPlants() {
        return plants;
    }

    public static void setPlants(HashMap<PlantType, PlantData> plants) {
        if (plants == null) {
            Data.plants = new HashMap<>();
        } else {
            Data.plants = plants;
        }
    }

    public void saveGame() {
        saveUser();
    }

    public void deserializeGame() {
        deserializeUser();
    }

    private static Path getSaveDirectory() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, SAVE_DIRECTORY_NAME);
    }

    private static void writeUsersToFile(Path path) throws IOException {
        try (
            FileOutputStream fileOutput = new FileOutputStream(path.toFile());
            BufferedOutputStream bufferedOutput =
                new BufferedOutputStream(fileOutput);
            ObjectOutputStream objectOutput =
                new ObjectOutputStream(bufferedOutput)
        ) {
            objectOutput.writeObject(allUsers);
            objectOutput.flush();
            bufferedOutput.flush();
            fileOutput.getFD().sync();
        }
    }

    private static boolean loadUsersFromFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        try (
            FileInputStream fileInput = new FileInputStream(path.toFile());
            BufferedInputStream bufferedInput =
                new BufferedInputStream(fileInput);
            ObjectInputStream objectInput =
                new ObjectInputStream(bufferedInput)
        ) {
            Object savedObject = objectInput.readObject();

            if (!(savedObject instanceof ArrayList<?> savedUsers)) {
                throw new IOException("The save file does not contain a user list.");
            }

            ArrayList<User> loadedUsers = convertToUsers(savedUsers);
            allUsers = loadedUsers;

            logInfo(
                "User data loaded from " + path.toAbsolutePath()
                    + ". Users: " + allUsers.size()
            );

            return true;
        } catch (IOException | ClassNotFoundException exception) {
            logError("Could not load user data from " + path.toAbsolutePath(), exception);
            return false;
        }
    }

    private static ArrayList<User> convertToUsers(
        ArrayList<?> savedUsers
    ) throws IOException {
        ArrayList<User> loadedUsers = new ArrayList<>();

        for (Object savedUser : savedUsers) {
            if (!(savedUser instanceof User user)) {
                throw new IOException("The save file contains an invalid user entry.");
            }

            loadedUsers.add(user);
        }

        return loadedUsers;
    }

    private static void replaceFile(
        Path temporaryFile,
        Path usersFile
    ) throws IOException {
        try {
            Files.move(
                temporaryFile,
                usersFile,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                temporaryFile,
                usersFile,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static void restorePrimaryFile(
        Path backupFile,
        Path usersFile
    ) {
        try {
            Files.createDirectories(usersFile.getParent());

            Files.copy(
                backupFile,
                usersFile,
                StandardCopyOption.REPLACE_EXISTING
            );

            logInfo("The primary user file was restored from its backup.");
        } catch (IOException exception) {
            logError("Could not restore the primary user file.", exception);
        }
    }

    private static void deleteTemporaryFile(Path temporaryFile) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            logError("Could not delete the temporary save file.", exception);
        }
    }

    private static void logInfo(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("Data", message);
        } else {
            System.out.println("[Data] " + message);
        }
    }

    private static void logError(
        String message,
        Throwable throwable
    ) {
        if (Gdx.app != null) {
            Gdx.app.error("Data", message, throwable);
        } else {
            System.err.println("[Data] " + message);
            throwable.printStackTrace();
        }
    }
}
