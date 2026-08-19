package models.gamepanes;

import models.entity.Zombie;
import java.util.*;

public class GridController {

    private final List<GridItem> gridItems = new ArrayList<>();

    public void addGridItem(GridItem item) {
        gridItems.add(item);
    }

    public void removeGridItem(GridItem item) {
        gridItems.remove(item);
    }

    public GridItem getGridItem(int row, int col) {
        for (GridItem item : gridItems) {
            if (item.getRow() == row && item.getCol() == col) {
                return item;
            }
        }
        return null;
    }

    public List<GridItem> getGridItems() {
        return gridItems;
    }

    public void checkAndAttachZombies(List<Zombie> zombies) {
        for (GridItem item : gridItems) {
            if (item.getOwner() != null || !item.isPushable()) continue;

            for (Zombie z : zombies) {
                if (!z.isAlive()) continue;

                if (z.getLine() == item.getRow() && z.getTileIndex() == item.getCol()) {

                    boolean canAttach = false;
                    if (item.getType().equals("arcade") || z.getType().toLowerCase().contains("arcade")) {
                        canAttach = true;
                    } else if (item.getType().equals("ice") || z.getType().toLowerCase().contains("troglobite")) {
                        canAttach = true;
                    } else if (item.getType().equals("barrel") || z.getType().toLowerCase().contains("barrel")) {
                        canAttach = true;
                    }

                    if (canAttach) {
                        item.setOwner(z);
                        break;
                    }
                }
            }
        }
    }

    public void updateItems() {
        for (GridItem item : gridItems) {
            if (item.getOwner() != null && item.getOwner().isAlive()) {
                item.setX(item.getOwner().getX());
                item.setY(item.getOwner().getY());
                item.setRow(item.getOwner().getLine());
                item.setCol(item.getOwner().getTileIndex() + 1);
            }
            else if (item.getOwner() != null && !item.getOwner().isAlive()) {
                // killed zombie , no zombie
                item.setOwner(null);
            }
        }
    }

    public void pushItem(GridItem item, int newCol) {
        if (newCol >= 9) {
            removeGridItem(item);
            return;
        }
        item.setCol(newCol);
    }

    public void clear() {
        gridItems.clear();
    }
}
