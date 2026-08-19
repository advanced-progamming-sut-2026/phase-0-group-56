package models;

import models.entity.Entity;

public class Constants {
    public final static int PROTECTED_SEEDS_COUNT = 3;
        public final static float GRAVITY = 300;
    public final static float LOBBER_BULLET_VELOCITY_X = 100;
    public final static float BULLET_VELOCITY_X = 120;
    public final static float MAGICAL_BULLET_VELOCITY = 120;
    public final static float TALL_WALL_NUT_HEIGHT = 250;
    public final static int DEAD_LINE_TILE_INDEX = 3;
    public final static int LAP_Count = 5;
    public final static int PLANTS_COUNT_IN_A_GAME = 8;
    public final static int PLANT_WHAT_YOU_GET_STARTING_SUN_COUNT = 800;
    public final static int DISASTER_ZOMBIES_BASE_COUNT = 3;
    public final static float TORNADO_VELOCITY = 40;
    public final static float ENDURIAN_ARMOR_DAMAGE = 80f;
    public final static float WATER_SURFACE_CHANGE_TIME = 30f;
    public final static float SUN_DROPPING_VELOCITY = 70f;
    public final static float CHILL_TIME = 5f;
    public final static float POISON_BASE_DAMAGE = 10f;
    public final static float HOMING_VELOCITY = 250f;
    public final static int WALLNUT_LIMIT_LINE = 3;
    public final static float BOWLING_WALLNUT_VELOCITY = 200f;
    public final static float MOANER_SPEED = 300f;

    public static boolean overlap(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null) {
            return false;
        }

        return (entity1.getX() < entity2.getX() + entity2.getWidth()) &&
                (entity1.getX() + entity1.getWidth() > entity2.getX()) &&
                (entity1.getY() < entity2.getY() + entity2.getHeight()) &&
                (entity1.getY() + entity1.getHeight() > entity2.getY());
    }

}
