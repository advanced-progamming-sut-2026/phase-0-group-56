package models.gameadventure;

import models.Constants;
import models.entity.Plant;
import models.entity.PlantTags;
import models.gamepanes.Field;
import models.games.BaseGame;

import java.util.Random;

public class Water implements  ChapterSpecialEvent
{
    private static final int MIN_SURFACE_COLUMN = 4;
    private static final int MAX_SURFACE_COLUMN = 7;
    public Water(BaseGame game) {

    }
    float waterSurfaceChange = 10f;
    Random rand = new Random();
    @Override
    public void run(BaseGame game, float delta) {
        if(waterSurfaceChange <= 0){
            int oldSurface = game.getField().getWaterCurrentSurface();
            int newSurface = MIN_SURFACE_COLUMN
                + rand.nextInt(MAX_SURFACE_COLUMN - MIN_SURFACE_COLUMN + 1);
            game.getField().setWaterCurrentSurface(newSurface);
            fixTiles(game.getField(), oldSurface, newSurface);
            WaterEffect(game);
            waterSurfaceChange = rand.nextFloat(Constants.WATER_SURFACE_CHANGE_TIME);
        }else{
            waterSurfaceChange -= delta;
        }


    }

    private void fixTiles(Field field, int oldSurface, int newSurface) {
        if (newSurface < oldSurface) {
            for (int i = newSurface; i < oldSurface; i++) {
                for (int j = 0; j < 5; j++) {
                    field.getTiles().get(j).get(i).setWater(true);
                }
            }
        } else if (newSurface > oldSurface) {
            for (int i = oldSurface; i < newSurface; i++) {
                for (int j = 0; j < 5; j++) {
                    field.getTiles().get(j).get(i).setWater(false);
                }
            }
        }
    }

    private void WaterEffect(BaseGame game) {
        for (Plant x : game.getPlantsInField()){
            if(x.getTileIndex() >= game.getField().getWaterCurrentSurface()){
                boolean waterPlant = x.getTags() != null
                    && x.getTags().contains(PlantTags.WATER);
                if(!x.onLilyPad && !waterPlant){
                    // Mark the plant for the normal removal path.  Calling
                    // dispose() alone only triggers skills; it does not make
                    // the drowned plant leave the field.
                    x.setHP(0f);
                    x.dispose(game);
                }
            }
        }
    }


}
