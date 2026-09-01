package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;

public class Wallet implements Menu {
    @Override
    public String ChangeMenu(String menuName) { return "Invalid menu transition from this menu."; }

    @Override
    public String exitMenu() {
        App.setScreen(new view.HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Wallet Menu ---\n" + showCoinWallet() + "\n" + showGemWallet();
    }

    public String showCoinWallet() {
        User user = Data.getCurrentUser();
        return user != null ? "Coin Wallet: " + user.getCoins() + " Coins" : "Error: Please log in to view your wallet.";
    }

    public String showGemWallet() {
        User user = Data.getCurrentUser();
        return user != null ? "Gem Wallet: " + user.getDiamonds() + " Diamonds" : "Error: Please log in to view your wallet.";
    }

    public String cheatAdd(int amount, String type) {
        User user = Data.getCurrentUser();
        if (user == null) return "Error: Please log in.";
        if (type.equalsIgnoreCase("coin") || type.equalsIgnoreCase("coins")) {
            user.addCoins(amount);
            Data.saveUser();
            return "Cheat activated: Added " + amount + " coins successfully.";
        } else if (type.equalsIgnoreCase("diamond") || type.equalsIgnoreCase("diamonds")) {
            user.addDiamonds(amount);
            Data.saveUser();
            return "Cheat activated: Added " + amount + " diamonds successfully.";
        }
        return "Error: Invalid cheat type. Use 'coin' or 'diamond'.";
    }
}
