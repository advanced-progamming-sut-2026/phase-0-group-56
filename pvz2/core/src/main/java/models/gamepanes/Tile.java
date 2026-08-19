package models.gamepanes;

import com.badlogic.gdx.math.Rectangle;

public class Tile {
    private static float width = 82;
    private static float height = 97;
    private Rectangle bounds;
    float x , y;
    private TileType tileType;
    private boolean plantable = true;
    private boolean zombieSpawner = false;
    private boolean empty = true;
    private boolean water = false;
    private boolean block =  false;
    private float hp;
    private int line;
    private int col;
    public Tile(TileType tileType , int line , int col) {
        setTileType(tileType);
        this.plantable = tileType.isPlantable();
        this.zombieSpawner = tileType.isZombieSpawner();
        this.block = tileType.block;
        this.hp = tileType.hp;
        this.line = line;
        this.col = col;
        this.x = this.col * width;
        this.y = this.line * height;

        bounds = new Rectangle(x , y , width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public static float getHeight() {
        return height;
    }

    public static float getWidth() {
        return width;
    }

    public TileType getTileType() {
        return tileType;
    }

    public static void setWidth(float width) {
        Tile.width = width;
    }

    public static void setHeight(float height) {
        Tile.height = height;
    }

    public void setTileType(TileType tileType) {
        if (tileType == null) {
            return;
        }

        this.tileType = tileType;
        this.plantable = tileType.isPlantable();
        this.zombieSpawner = tileType.isZombieSpawner();
        this.block = tileType.block;
        this.hp = tileType.getHp();
    }

    public boolean isPlantable() {
        return plantable;
    }

    public void setPlantable(boolean plantable) {
        this.plantable = plantable;
    }

    public boolean isZombieSpawner() {
        return zombieSpawner;
    }

    public void setZombieSpawner(boolean zombieSpawner) {
        this.zombieSpawner = zombieSpawner;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public float getHp() {
        return hp;
    }

    public void setHp(float hp) {
        this.hp = hp;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public boolean isWater() {
        return water;
    }

    public void setWater(boolean water) {
        this.water = water;
    }


}
