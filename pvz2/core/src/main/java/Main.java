import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import controllers.datacontroller.*;

import models.*;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;


public class Main{
    private static String COMMAND = "java \"-Dpvz.assets=.\\Assets\" -jar browser.jar";

    public static void main(String[] args) {

        Data.deserializeUser();
        Data.setUp();
        Data.loadPlantsFromJson();
        Data.loadLevelsFromJson();
        while (App.getScreen() != null) {

        }
    }
}
