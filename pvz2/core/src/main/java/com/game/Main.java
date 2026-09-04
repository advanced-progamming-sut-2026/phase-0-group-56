package com.game;

import com.badlogic.gdx.Game;
import controllers.datacontroller.Data;
import models.App;
import models.utils.AudioManager;
import network.NetworkService;

/**
 * Main LibGDX application shared by all desktop platforms.
 */
public class Main extends Game {

    @Override
    public void create() {
        new App(this);

        AudioManager kanye = AudioManager.getInstance();
        kanye.loadAll();
        kanye.setMusicVolume(0.6f);
        kanye.setSFXVolume(0.8f);

        Data.loadPlantsFromJson();
        Data.loadLevelsFromJson();
        Data.setUp();
        NetworkService.ensureEmbedded();
        NetworkService.importLocalAccounts();
    }



    @Override
    public void pause() {
        Data.saveUser();
        super.pause();
    }

    @Override
    public void dispose() {
        Data.saveUser();
        NetworkService.stopEmbedded();
        super.dispose();
    }
}
