package controllers.menus.gamecontroller;

import models.App;
import models.games.minigames.VaseBraker;
import models.utils.Result;
import view.TravelLogView;

public class VaseBreakerController implements Controller{
    VaseBraker game;
    @Override
    public String playGame(float delta) {
        game.playGame(delta);
        Result end = game.check_endGame();
        if(end.success()){
            if(end.message().equals("Won")){
                App.getCurrentuser().setVaseBreaker(App.getCurrentuser().getVaseBreaker() + 1);
            }
            end();
        }
        return "Vases Remained = " + game.getVases().size();
    }
    private void end(){
        App.setScreen(new TravelLogView());
    }

    @Override
    public String GameStart(String input) {
        return "";
    }

    public boolean plant(String plantNamt , int x , int y){
        return game.plant(plantNamt, x, y);
    }

    public String showVases(){
        return null;
    }

}
