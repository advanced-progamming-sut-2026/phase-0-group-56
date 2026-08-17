package models;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.game.Main;
import controllers.datacontroller.Data;
import controllers.menus.Menu;
import pvz.skin.PvzSkin;
import view.MenuView;

import java.util.Scanner;

public class App {
    public static Scanner input = new Scanner(System.in);

    private static Menu currentmenu;
    private static Main main;
    public static Skin skin;

    public App(Main main) {
        App.main = main;
        skin = PvzSkin.get();
    }

    public static void setScreen(MenuView screen) {
        if (screen instanceof Screen) {
            setScreen((Screen) screen);
        }
    }

    public static void setScreen(Screen screen) {
        if (main == null || screen == null) {
            return;
        }
        if (skin == null) {
            skin = PvzSkin.get();
        }
        main.setScreen(screen);
    }

    public static Scanner getInput() {
        return input;
    }

    public static void setInput(Scanner scanner) {
        if (scanner != null) {
            input = scanner;
        }
    }

    public static Menu getCurrentmenu() {
        return currentmenu;
    }

    public static void setCurrentmenu(Menu currentmenu) {
        App.currentmenu = currentmenu;
    }

    public static void setCurrentuser(User currentuser) {
        Data.setCurrentUser(currentuser);
    }

    public static User getCurrentuser() {
        return Data.getCurrentUser();
    }

    public static Screen getScreen() {
        return main == null ? null : main.getScreen();
    }
}
