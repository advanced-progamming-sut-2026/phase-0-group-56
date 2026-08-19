package controllers.datacontroller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import models.gameadventure.*;
import models.gameadventure.levels.*;
import view.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Data {
    private static final String USERS_FILE = "users_data.dat";
    private static ArrayList<User> allUsers = new ArrayList<>();
    private static User currentUser = null;
    private static User tempUser = null;
    private static HashMap<PlantType, PlantData> plants = new HashMap<>();
    private static HashMap<Chapters, ArrayList<Level>> allLevels = new HashMap<>();

    public static void saveUser() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(allUsers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void deserializeUser() {
        File userFile = new File(USERS_FILE);

        if (!userFile.exists()) {
            allUsers = new ArrayList<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile))) {
            allUsers = (ArrayList<User>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Warning: Could not load user data or file is corrupted. Starting fresh.");
            allUsers = new ArrayList<>();
        }
    }

    public static void setUp() {
        User testUser = new User("LeBron", "passhash", "LeBron", "LeBron", "LeBron");
        currentUser = testUser;
        App.setScreen(new PlayView());

        /*
        if (allUsers.isEmpty()) {
            App.setScreen(new SignUpView());
        } else {
            for (User user : allUsers) {
                if (user.isStayLoggedIn()) {
                    currentUser = user;
                    App.setScreen(new HomeView());
                    return;
                }
            }
        }
        App.setScreen(new SignUpView());
        */
    }

    public static void addUser(User user) {
        allUsers.add(user);
        saveUser();
    }

    public static boolean isUsernameExists(String username) {
        for (User user : allUsers) {
            if (user.getName().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public static User getUserByUsername(String username) {
        for (User user : allUsers) {
            if (user.getName().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Loads plants.json using PlantData's custom LibGDX Json reader.
     *
     * PlantData handles the legacy tag format and the description/effect mismatch.
     * This method only has to normalize the human-readable plant name to PlantType.
     */
    public static void loadPlantsFromJson() {
        FileHandle file = Gdx.files.internal("plants.json");

        if (!file.exists()) {
            Gdx.app.error("Data", "plants.json not found in assets folder!");
            plants = new HashMap<>();
            return;
        }

        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            ArrayList<PlantData> plantsList =
                json.fromJson(ArrayList.class, PlantData.class, file);

            HashMap<PlantType, PlantData> loadedPlants = new HashMap<>();

            for (PlantData plant : plantsList) {
                if (plant == null || plant.getName() == null || plant.getName().isBlank()) {
                    Gdx.app.error("Data", "Skipping plant with missing name in plants.json");
                    continue;
                }

                try {
                    PlantType type = parsePlantType(plant.getName());
                    loadedPlants.put(type, plant);
                } catch (IllegalArgumentException e) {
                    Gdx.app.error(
                        "Data",
                        "Unknown PlantType for plants.json name: " + plant.getName(),
                        e
                    );
                }
            }

            plants = loadedPlants;
            Gdx.app.log("Data", "Plants loaded successfully: " + plants.size());

        } catch (Exception e) {
            plants = new HashMap<>();
            Gdx.app.error("Data", "Error reading plants.json", e);
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
            Gdx.app.error("Data", "levels.json not found in assets folder!");
            return;
        }

        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            ArrayList<Level> levelsList = json.fromJson(ArrayList.class, Level.class, file);

            allLevels = new HashMap<>();
            for (Level level : levelsList) {
                Chapters chapter = level.getChapters();
                allLevels.putIfAbsent(chapter, new ArrayList<>());
                allLevels.get(chapter).add(level);
            }

            Gdx.app.log(
                "Data",
                "Levels loaded successfully! Total chapter: " + allLevels.size()
            );

        } catch (Exception e) {
            Gdx.app.error("Data", "Error reading levels.json", e);
        }
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
        Data.plants = plants;
    }

    public void saveGame() {
    }

    public void deserializeGame() {
    }
}
