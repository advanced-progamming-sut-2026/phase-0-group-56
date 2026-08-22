package controllers.menus.gamecontroller;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import models.games.minigames.Beghouled;
import models.utils.Result;

/** Controller for Beghouled input, selection state, upgrades, and score saving. */
public final class BeghouledController implements Controller {
    private final Beghouled game;
    private int selectedColumn = -1;
    private int selectedRow = -1;
    private boolean resultSaved;

    public BeghouledController(int levelNumber) {
        game = new Beghouled(levelNumber);
    }

    @Override
    public String playGame(float delta) {
        String log = game.playGame(delta);
        Result end = game.check_endGame();
        if (end.success() && !resultSaved) {
            resultSaved = true;
            saveScore();
        }
        return log == null ? "" : log;
    }

    public SelectionResult selectTile(int column, int row) {
        if (game.isLost()) {
            clearSelection();
            return new SelectionResult(false, "The game is over.");
        }
        if (game.getPlantAt(row, column) == null) {
            clearSelection();
            return new SelectionResult(false, "There is no plant on that tile.");
        }
        if (!hasSelection()) {
            setSelection(column, row);
            return new SelectionResult(false, "Plant selected. Choose an adjacent plant.");
        }
        if (selectedColumn == column && selectedRow == row) {
            clearSelection();
            return new SelectionResult(false, "Selection cancelled.");
        }
        if (!isAdjacent(column, row)) {
            setSelection(column, row);
            return new SelectionResult(false, "New plant selected. Choose an adjacent plant.");
        }

        Beghouled.SwapResult result = game.swap(
            selectedColumn,
            selectedRow,
            column,
            row
        );
        clearSelection();
        return new SelectionResult(result == Beghouled.SwapResult.MATCHED, swapMessage(result));
    }

    public String upgrade(PlantType from) {
        Beghouled.UpgradeResult result = game.upgrade(from);
        return switch (result) {
            case UPGRADED -> pretty(from) + " upgraded.";
            case NOT_ENOUGH_SUN -> "Not enough sun for that upgrade.";
            case NOT_AVAILABLE -> "That upgrade is not available on this board.";
        };
    }

    private String swapMessage(Beghouled.SwapResult result) {
        return switch (result) {
            case MATCHED -> "Match! Sun and score awarded.";
            case NO_MATCH -> "That swap does not create a match.";
            case NOT_ADJACENT -> "Choose an adjacent plant.";
            case EMPTY_TILE -> "Both tiles must contain plants.";
            case OUT_OF_BOUNDS -> "That tile is outside the lawn.";
            case GAME_OVER -> "The game is over.";
        };
    }

    private boolean isAdjacent(int column, int row) {
        return Math.abs(selectedColumn - column) + Math.abs(selectedRow - row) == 1;
    }

    private void setSelection(int column, int row) {
        selectedColumn = column;
        selectedRow = row;
    }

    public void clearSelection() {
        selectedColumn = -1;
        selectedRow = -1;
    }

    public boolean hasSelection() {
        return selectedColumn >= 0 && selectedRow >= 0;
    }

    public int getSelectedColumn() {
        return selectedColumn;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public Beghouled getGame() {
        return game;
    }

    private void saveScore() {
        User user = App.getCurrentuser();
        if (user == null) {
            return;
        }
        user.setHighestScore(game.getScore());
        user.incrementGamesPlayed();
        Data.saveUser();
    }

    private static String pretty(PlantType type) {
        return type.name().toLowerCase().replace('_', ' ');
    }

    @Override
    public String GameStart(String input) {
        return "Beghouled is already running.";
    }

    public record SelectionResult(boolean matched, String message) {
    }
}
