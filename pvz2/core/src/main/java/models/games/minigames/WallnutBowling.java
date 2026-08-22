package models.games.minigames;

import models.App;
import models.Constants;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.entity.Zombie;
import models.gamepanes.Field;
import models.gamepanes.Wave;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Random;

public class WallnutBowling extends NormalGame {
    public  WallnutBowling(MinigameLevel level) {
        int safeLevel = Math.max(1, Math.min(16, level == null ? 1 : level.getId()));
        field = new Field().initField(Chapters.AncientEgypt, safeLevel);
        Level level1 = new Level(
            safeLevel,
            Chapters.AncientEgypt,
            "normal",
            2,
            100
        );
        level1.setAllowedZombies(new ArrayList<>(java.util.List.of("normal")));
        initGame(Chapters.AncientEgypt, level1);
        belt.add("Wallnut");
        belt.add("Explod'O nut");
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
        return result == null ? "No wave transition." : result.message();
    }

    @Override
    public String plant(String plantName, int x, int y) {
        if(x >= Constants.WALLNUT_LIMIT_LINE) return "You can't plant after limit line!";
        if(plantName.equals("Wallnut") && belt.contains(plantName)){
            nuts.add(makeNut(false , x ,y));
        }
        else if(plantName.equals("Explod'O nut") && belt.contains(plantName)){
            nuts.add(makeNut(true , x ,y));
        }
        else return "Come on man open your eyes.";
        return "Suiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii , bowling on It's wayyyyy";
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
        if (currentWave == null) {
            if (waves.isEmpty()) {
                won = true;
                return new Result(true, "Won", null);
            }
            currentWave = waves.get(waveID);
            zombies.addAll(currentWave.getZombies());
            waveID += 1;
            return new Result(true, "Wave started", null);
        }
        if(currentWave.isFinished()){
            if(waveID >= waves.size()) {
                won = true;
                return new Result(true, "Won", null);
            }
            previousWave = currentWave;
            currentWave = waves.get(waveID);
            zombies.addAll(currentWave.getZombies());
            waveID += 1;
            return new Result(true,"new Wave",null);
        }
        return  new  Result(false,null,null);
    }
}
