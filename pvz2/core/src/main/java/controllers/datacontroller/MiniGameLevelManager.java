package controllers.datacontroller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import models.games.minigames.MinigameLevel;
import models.factory.builder.PlantType;

import java.util.ArrayList;
import java.util.List;

public class MiniGameLevelManager {

    private static List<MinigameLevel> allLevels;

    @SuppressWarnings("unchecked")
    public static void loadLevelsFromFile(String fileName) {
        // ۱. پیدا کردن فایل از پوشه assets (مثلاً "minigames.json")
        FileHandle file = Gdx.files.internal(fileName);

        if (!file.exists()) {
            Gdx.app.error("MiniGameLevelManager", "❌ File not found in assets: " + fileName);
            allLevels = createFallbackLevels();
            return;
        }

        try {
            // ۲. ساخت شیء Json (مال خود LibGDX)
            Json json = new Json();

            // ۳. این خط باعث میشه اگه تو فایل جیسون فیلد اضافه‌ای بود که تو کلاس جاوات نبود، کرش نکنه
            json.setIgnoreUnknownFields(true);

            // ۴. خواندن مستقیم لیست از فایل جیسون! (خیلی تمیزتر از Gson)
            allLevels = json.fromJson(ArrayList.class, MinigameLevel.class, file);

            Gdx.app.log("MiniGameLevelManager", "✅ Minigame Levels loaded successfully!");

        } catch (Exception e) {
            Gdx.app.error("MiniGameLevelManager", "❌ Exception in reading files: " + e.getMessage());
            allLevels = createFallbackLevels();
        }
    }

    public static MinigameLevel getLevelById(int id) {
        if (allLevels == null || allLevels.isEmpty()) {
            allLevels = createFallbackLevels();
        }
        for (MinigameLevel level : allLevels) {
            if (level.getId() == id) return level;
        }
        return null;
    }

    private static List<MinigameLevel> createFallbackLevels() {
        List<MinigameLevel> result = new ArrayList<>();
        for (int id = 1; id <= 16; id++) {
            MinigameLevel level = new MinigameLevel();
            level.setId(id);
            level.setPlants(new ArrayList<>(List.of(
                PlantType.PEASHOOTER,
                PlantType.WALL_NUT,
                PlantType.CHOMPER,
                PlantType.SUNFLOWER,
                PlantType.POTATO_MINE
            )));
            level.setZombiesNames(new ArrayList<>(List.of("normal", "cone", "bucket")));
            result.add(level);
        }
        return result;
    }
}
