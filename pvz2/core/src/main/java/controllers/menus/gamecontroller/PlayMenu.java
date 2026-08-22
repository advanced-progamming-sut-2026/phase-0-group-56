package controllers.menus.gamecontroller;

import controllers.datacontroller.Data;
import controllers.datacontroller.LevelProgressService;
import controllers.menus.Menu;
import models.App;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import view.*;
import view.gameview.GameView;

import java.util.ArrayList;

public class PlayMenu implements Menu {
    private Chapters currentChapter;

    public PlayMenu() {
        if (App.getCurrentuser() != null) {
            LevelProgressService.normalizeUserProgress(App.getCurrentuser());
            currentChapter = App.getCurrentuser().getChapter();
        } else {
            currentChapter = Chapters.AncientEgypt;
        }
    }

    @Override
    public String ChangeMenu(String menuName) {
        switch (menuName) {
            case "Collection menu":
                App.setScreen(new CollectionView());
                return "Changed successfully to Collection menu";
            case "Greenhouse menu":
                App.setScreen(new GreenHouseView());
                return "Changed successfully to Greenhouse menu";
            case "Quests menu":
                App.setScreen(new TravelLogView());
                return "Changed successfully to Quests menu";
            case "Leaderboard menu":
                App.setScreen(new LeaderBoardView());
                return "Changed successfully to Leaderboard menu";
            case "Wallet menu":
                App.setScreen(new WalletView());
                return "Changed successfully to Wallet menu";
            default:
                return "Invalid menu name";
        }
    }

    public String changeChapter(Chapters chapter) {
        if (chapter == null) {
            return "Invalid chapter";
        }

        if (App.getCurrentuser() != null) {
            LevelProgressService.normalizeUserProgress(App.getCurrentuser());
        }
        currentChapter = chapter;
        StringBuilder output = new StringBuilder();

        ArrayList<Level> chapterLevels = Data.getAllLevels().get(chapter);

        if (chapterLevels == null || chapterLevels.isEmpty()) {
            return "Welcome to " + chapter.name() + "\nNo levels available for this chapter yet.";
        }

        for (Level level : chapterLevels) {
            int levelId = level.getId();
            boolean unlocked = LevelProgressService.isLevelUnlocked(
                App.getCurrentuser(),
                level
            );

            output.append("═════════════════════════ LEVEL ").append(levelId).append(" : ").append(unlocked ? "Unlocked" : "Locked");

            if (levelId == App.getCurrentuser().getLevelId() && chapter == App.getCurrentuser().getChapter()) {
                output.append(" (You are here now)");
            }
            output.append(" ═════════════════════════\n");
        }

        return "Welcome to " + chapter.name() + "\n" + output.toString();
    }

    public String play(int levelId) {
        ArrayList<Level> chapterLevels = Data.getAllLevels().get(currentChapter);

        if (chapterLevels == null || chapterLevels.isEmpty()) {
            return "Error: No levels found for " + currentChapter.name();
        }

        Level toPlay = null;
        for (Level l : chapterLevels) {
            if (l.getId() == levelId) {
                toPlay = l;
                break;
            }
        }

        if (toPlay == null) {
            return "Error: Level " + levelId + " does not exist in " + currentChapter.name() + ".";
        }

        boolean isUnlocked = LevelProgressService.isLevelUnlocked(
            App.getCurrentuser(),
            toPlay
        );

        if (!isUnlocked) {
            return "Error: You haven't unlocked Level " + levelId + " yet!";
        }

        App.setScreen(new GameView(currentChapter, toPlay));

        return "Loading Level " + levelId + " from " + currentChapter.name() + "...\nGame on , baby.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return Menu.super.exitMenu();
    }
}
