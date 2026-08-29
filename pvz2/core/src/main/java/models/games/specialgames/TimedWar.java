package models.games.specialgames;

import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;

public class TimedWar extends NormalGame implements SpecialGame {
    private float timeRemaining;

    public TimedWar(Chapters chapter, Level level) {
        super(chapter,level);
        timeRemaining = Math.max(90f, 150f - Math.max(0, level.getId()) * 2f);
    }

    @Override
    public ArrayList<PlantType> filterPlants() {
        return selection.getPlantsToChoose();
    }

    @Override
    public void attack() {

    }

    @Override
    public String playGame(float delta) {
        if (state != GameState.PLAYING) {
            return "";
        }

        String log = super.playGame(delta);
        if (!won) {
            timeRemaining -= Math.max(0f, delta);
            if (timeRemaining <= 0f) {
                timeRemaining = 0f;
                state = GameState.END;
                return (log == null ? "" : log) + "\nTime is up.";
            }
        }
        return log;
    }

    @Override
    public Result check_endGame() {
        if (!won && timeRemaining <= 0f) {
            state = GameState.END;
            return new Result(true, "Loss", null);
        }
        return super.check_endGame();
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }
}
