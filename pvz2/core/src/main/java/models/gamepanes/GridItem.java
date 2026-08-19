package models.gamepanes;

import models.entity.Zombie;

public class GridItem {

    private final String type;
    private int row;
    private int col;
    private float x;
    private float y;
    private int hp;
    private final int maxHp;
    private final boolean isPushable;
    private final boolean isDestructible;
    private Zombie owner;

    public GridItem(String type, int row, int col, int hp, boolean isPushable, boolean isDestructible) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.hp = hp;
        this.maxHp = hp;
        this.isPushable = isPushable;
        this.isDestructible = isDestructible;
        this.owner = null;
        this.x = col * Tile.getWidth();
        this.y = row * Tile.getHeight();
    }

    public void takeDamage(int damage) {
        if (!isDestructible) return;
        hp -= damage;
        if (hp < 0) hp = 0;
    }

    public boolean isDestroyed() {
        return isDestructible && hp <= 0;
    }

    // ====== GETTERS & SETTERS ======
    public String getType() { return type; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isPushable() { return isPushable; }
    public boolean isDestructible() { return isDestructible; }
    public Zombie getOwner() { return owner; }

    public void setOwner(Zombie owner) {
        this.owner = owner;
        if (owner != null) {
            this.row = owner.getLine();
            this.col = owner.getTileIndex() + 1;
            this.x = owner.getX();
            this.y = owner.getY();
        }
    }

    public void setRow(int row) { this.row = row; this.y = row * Tile.getHeight(); }
    public void setCol(int col) { this.col = col; this.x = col * Tile.getWidth(); }
    public void setX(float x) { this.x = x; this.col = (int)(x / Tile.getWidth()); }
    public void setY(float y) { this.y = y; this.row = (int)(y / Tile.getHeight()); }
    public void setPosition(int row, int col) { setRow(row); setCol(col); }
}
