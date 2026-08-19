package com.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.datacontroller.Data;
import models.App;
import view.PlayView;
import view.gameview.GameView;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private SpriteBatch batch;
    private Texture image;

    @Override
    public void create() {
        System.out.println("Kobe Bryant");
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        Data.loadPlantsFromJson();
        Data.loadLevelsFromJson();
        new App(this);
        Data.setUp();

    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
