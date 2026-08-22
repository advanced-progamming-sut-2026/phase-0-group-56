package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import view.HomeView;
import view.ShopView;

public class GreenHouseController implements Menu {
    @Override
    public String ChangeMenu(String menuName) {
        if (menuName != null
            && (
            menuName.equalsIgnoreCase("Shop")
                || menuName.equalsIgnoreCase("Shop menu")
        )) {

            App.setScreen(new ShopView());
            return "Changed menu successfully to Shop menu.";
        }

        return "Invalid menu transition from GreenHouse menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- GreenHouse Menu ---";
    }

    public String showgreenhouse() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: User not logged in.";
        }

        return user.getGreenHouse().showAll();
    }

    public String plant(int x, int y) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: User not logged in.";
        }

        return saveOnSuccess(
            user.getGreenHouse()
                .plantPot(x, y)
        );
    }

    public String forceGrow(int x, int y) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: User not logged in.";
        }

        return saveOnSuccess(
            user.getGreenHouse()
                .growNow(x, y)
        );
    }

    /*
     * Legacy overload retained for terminal and older View compatibility.
     * The trusted cost is always calculated by Pot from the current clock.
     */
    public String forceGrow(
        int x,
        int y,
        int ignoredRemainingHours
    ) {
        return forceGrow(x, y);
    }

    public String collect(int x, int y) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: User not logged in.";
        }

        return saveOnSuccess(
            user.getGreenHouse()
                .collectPot(x, y)
        );
    }

    /*
     * Legacy overload retained because the old terminal command supplied this
     * flag. Pot itself determines the actual seedling type.
     */
    public String collect(
        int x,
        int y,
        boolean ignoredIsMarigold
    ) {
        return collect(x, y);
    }

    private String saveOnSuccess(String result) {
        if (result != null
            && !result.startsWith("Error:")) {
            Data.saveUser();
        }

        return result == null
            ? "Error: Greenhouse operation failed."
            : result;
    }
}
