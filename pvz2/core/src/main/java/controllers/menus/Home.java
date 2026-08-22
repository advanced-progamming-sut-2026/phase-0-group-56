package controllers.menus;

import com.badlogic.gdx.Gdx;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import view.CollectionView;
import view.GreenHouseView;
import view.HomeView;
import view.LeaderBoardView;
import view.LogInView;
import view.NewsView;
import view.PlayView;
import view.ProfileView;
import view.SettingsView;
import view.ShopView;
import view.TravelLogView;
import view.WalletView;

public class Home implements Menu {

    @Override
    public String ChangeMenu(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            return "Error: menu name cannot be empty.";
        }

        String normalizedName =
            menuName.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();

        switch (normalizedName) {
            case "play":
            case "play menu":
            case "adventure":
            case "adventure menu":
                App.setScreen(new PlayView());
                return "Changed menu successfully to Play menu.";

            case "collection":
            case "collection menu":
                App.setScreen(new CollectionView());
                return "Changed menu successfully to Collection menu.";

            case "news":
            case "news menu":
                App.setScreen(new NewsView());
                return "Changed menu successfully to News menu.";

            case "profile":
            case "profile menu":
                App.setScreen(new ProfileView());
                return "Changed menu successfully to Profile menu.";

            case "setting":
            case "setting menu":
            case "settings":
            case "settings menu":
                App.setScreen(new SettingsView());
                return "Changed menu successfully to Settings menu.";

            case "leaderboard":
            case "leaderboard menu":
                App.setScreen(new LeaderBoardView());
                return "Changed menu successfully to Leaderboard menu.";

            case "quest":
            case "quest menu":
            case "quests":
            case "quests menu":
            case "travel log":
            case "travel log menu":
                App.setScreen(new TravelLogView());
                return "Changed menu successfully to Travel Log menu.";

            case "greenhouse":
            case "greenhouse menu":
            case "green house":
            case "green house menu":
                App.setScreen(new GreenHouseView());
                return "Changed menu successfully to Greenhouse menu.";

            case "shop":
            case "shop menu":
                App.setScreen(new ShopView());
                return "Changed menu successfully to Shop menu.";

            case "wallet":
            case "wallet menu":
                App.setScreen(new WalletView());
                return "Changed menu successfully to Wallet menu.";

            case "home":
            case "home menu":
            case "main":
            case "main menu":
                App.setScreen(new HomeView());
                return "You are already in the Main menu.";

            case "login":
            case "login menu":
                App.setScreen(new LogInView());
                return "Changed menu successfully to Login menu.";

            default:
                return "Error: the selected menu is not available from the Main menu.";
        }
    }

    @Override
    public String ShowCurrentMenu() {
        return """
            --- Main Menu ---
            1. Adventure
            2. Collection
            3. News
            4. Profile
            5. Settings
            6. Leaderboard
            7. Travel Log / Quests
            8. Greenhouse
            9. Shop
            10. Wallet
            11. Log Out
            12. Exit Game
            """.trim();
    }

    @Override
    public String exitMenu() {
        Data.saveUser();

        if (Gdx.app != null) {
            Gdx.app.exit();
        }

        return "Game closed successfully.";
    }

    public String LogOut() {
        User user = Data.getCurrentUser();

        if (user == null) {
            App.setScreen(new LogInView());
            return "No user is currently logged in.";
        }

        user.setStayLoggedIn(false);
        Data.saveUser();
        Data.setCurrentUser(null);

        App.setScreen(new LogInView());

        return "Logged out successfully.";
    }
}
