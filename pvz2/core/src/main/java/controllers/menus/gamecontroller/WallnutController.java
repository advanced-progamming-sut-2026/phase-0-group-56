package controllers.menus.gamecontroller;

import controllers.datacontroller.MiniGameLevelManager;
import controllers.datacontroller.Data;
import models.App;
import models.games.minigames.MinigameLevel;
import models.games.minigames.WallnutBowling;
import models.utils.Result;
import view.TravelLogView;

public class WallnutController implements Controller{
    public WallnutController(int level){
        this.level = MiniGameLevelManager.getLevelById(level);
        game = new WallnutBowling(this.level);
    }
    MinigameLevel level;
    WallnutBowling game ;


    @Override
    public String playGame(float delta) {
        game.playGame(delta);
        Result end = game.check_endGame();
        if(end.success()){
            if(end.message().equals("Won")){
                App.getCurrentuser().setWallNutBowling(App.getCurrentuser().getWallNutBowling() + 1);
                Data.saveUser();
            }
            endGame();
        }
        return "My brother , My captain , My King!\n    - Boromir to Aragorn";
    }

    private void endGame(){
        App.setScreen(new TravelLogView());
    }
    public String plant(String name , int x , int y) {
        try {
            return game.plant(name ,  x , y);
        }catch (Exception e){
            return "Name of the plant is not correct.";
        }
    }

    @Override
    public String GameStart(String input) {
        return "";
    }
}
