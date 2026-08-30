package models.games.minigames;

import network.IZombieNetworkMatch;
import network.IZombieNetworkState;

/** Local two-player variant using the exact same rules as network matches. */
public final class CouchIZombieGame {
    private final IZombieNetworkMatch match = new IZombieNetworkMatch("couch");

    public CouchIZombieGame() {
        match.join(IZombieNetworkState.Role.PLANTS);
        match.join(IZombieNetworkState.Role.ZOMBIES);
    }

    public IZombieNetworkMatch.ActionResult placePlant(String type, int column, int row) {
        return match.placePlant(IZombieNetworkState.Role.PLANTS, type, column, row);
    }

    public IZombieNetworkMatch.ActionResult placeZombie(String type, int column, int row) {
        return match.placeZombie(IZombieNetworkState.Role.ZOMBIES, type, column, row);
    }

    public void tick(float delta) {
        match.tick(delta);
    }

    public IZombieNetworkState snapshot() {
        return match.snapshot();
    }

    public IZombieNetworkMatch.ActionResult react(
        IZombieNetworkState.Role role, String category, String value
    ) {
        return match.react(role, category, value);
    }
}
