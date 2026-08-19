package models.games;

import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.factory.builder.PlantType;
import models.utils.Result;

public interface Game {

    public void initGame(Chapters chapter , Level level);
    public boolean startGame(String input);
    public String playGame(float delta);
    public void updatePlants(float delta);
    public void updateZombies(float delta);
    public void updateScene(float delta);
    public boolean plant(String plantName , int x , int y);
    public String pluck(int x , int y);
    public Result check_endGame();
    public void endGame();
    public default String boost(PlantType type) {
        return  "boosetd";
    }

}
