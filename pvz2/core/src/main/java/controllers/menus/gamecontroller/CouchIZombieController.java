package controllers.menus.gamecontroller;

import models.games.minigames.CouchIZombieGame;
import network.IZombieNetworkMatch;
import network.IZombieNetworkState;

/** Controller for simultaneous mouse/keyboard Couch Play input. */
public final class CouchIZombieController {
    private final CouchIZombieGame game = new CouchIZombieGame();

    public String placePlant(String type, int column, int row) {
        return message(game.placePlant(type, column, row));
    }

    public String placeZombie(String type, int column, int row) {
        return message(game.placeZombie(type, column, row));
    }

    public String react(IZombieNetworkState.Role role, String category, String value) {
        return message(game.react(role, category, value));
    }

    public void playGame(float delta) {
        game.tick(delta);
    }

    public CouchIZombieGame getGame() {
        return game;
    }

    private static String message(IZombieNetworkMatch.ActionResult result) {
        return result == null ? "Action failed." : result.message();
    }
}
