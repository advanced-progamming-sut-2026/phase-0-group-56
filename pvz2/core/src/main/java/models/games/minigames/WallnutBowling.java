package models.games.minigames;

import models.App;
import models.Constants;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.entity.Zombie;
import models.gamepanes.Field;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Random;

public class WallnutBowling extends NormalGame {
    public  WallnutBowling(MinigameLevel level) {
                field = new Field().initField(Chapters.AncientEgypt
                        , App.getCurrentuser().getWallNutBowling());
        Level level1 = new Level(App.getCurrentuser().getWallNutBowling()
        , Chapters.AncientEgypt , "normal" , 4 , 1000);
    }

    ArrayList<String> belt = new ArrayList<>();
    ArrayList<BowlingNut>  nuts = new ArrayList<>();


    @Override
    public Result check_endGame() {
        for (Zombie z : zombies) {
            if(z.getTileIndex() < Constants.WALLNUT_LIMIT_LINE) return new Result(true,"Loss",null);
        }
        if(won) return  new Result(true,"Won",null);
        return new  Result(false,null,null);
    }


    @Override
    public String playGame(float delta) {
        for (BowlingNut x : nuts){
            x.go(delta, this);
        }
       Result result = attack(delta);
        return result.message();
    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        if(x >= Constants.WALLNUT_LIMIT_LINE) return false;
        if(plantName.equals("Wallnut") && belt.contains(plantName)){
            nuts.add(makeNut(false , x ,y));
        }
        else if(plantName.equals("Explod'O nut") && belt.contains(plantName)){
            nuts.add(makeNut(true , x ,y));
        }
        else return false;
        return true;
    }

    private BowlingNut makeNut(boolean explosive , int x , int y) {
        BowlingNut bowling = new BowlingNut(1000 , explosive);
        Random rand = new Random();
        boolean up = rand.nextBoolean();
        bowling.setVelocityX(Constants.BOWLING_WALLNUT_VELOCITY);
        bowling.setVelocityY(Constants.BOWLING_WALLNUT_VELOCITY * 0.4f * (up ? 1 : -1));
        bowling.setTileIndex(x);
        bowling.setLine(y);
        nuts.add(bowling);
        return  bowling;
    }

    public ArrayList<BowlingNut> getNuts() {
        return nuts;
    }

    @Override
    protected Result attack(float delta) {
        if(currentWave.isFinished()){
            if(waveID == waves.size() - 1) return null;
            previousWave = currentWave;
            currentWave = waves.get(waveID);
            zombies.addAll(currentWave.getZombies());
            waveID += 1;
            return new Result(true,"new Wave",null);
        }
        return  new  Result(false,null,null);
    }
}
