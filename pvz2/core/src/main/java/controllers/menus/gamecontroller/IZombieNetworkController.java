package controllers.menus.gamecontroller;

import network.IZombieNetworkState;
import network.NetworkClient;
import network.NetworkEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** MVC controller for a server-authoritative network I, Zombie match. */
public final class IZombieNetworkController implements AutoCloseable {
    private final NetworkClient client;
    private final String matchId;
    private final IZombieNetworkState.Role role;
    private final ConcurrentLinkedQueue<Reaction> reactions = new ConcurrentLinkedQueue<>();
    private final Consumer<NetworkEvent> listener = this::onEvent;
    private volatile IZombieNetworkState state;
    private volatile String status = "Connecting to match...";
    private boolean closed;
    private boolean scoreSubmitted;

    public IZombieNetworkController(
        NetworkClient client, String matchId, IZombieNetworkState.Role role
    ) {
        if (client == null || matchId == null || matchId.isBlank() || role == null) {
            throw new IllegalArgumentException("A client, match id, and role are required.");
        }
        this.client = client;
        this.matchId = matchId;
        this.role = role;
        client.addEventListener(listener);
    }

    public String join() {
        network.NetworkResponse response = client.joinGame(matchId);
        status = response.message();
        return response.message();
    }

    public String placePlant(String type, int column, int row) {
        return client.placePlant(matchId, type, column, row).message();
    }

    public String placeZombie(String type, int column, int row) {
        return client.placeZombie(matchId, type, column, row).message();
    }

    public String sendReaction(String category, String value) {
        return client.sendReaction(matchId, category, value).message();
    }

    public String submitScore() {
        if (scoreSubmitted) {
            return "Score already saved on the server.";
        }
        IZombieNetworkState current = state;
        if (current == null || current.phase() == IZombieNetworkState.Phase.PLAYING
            || current.phase() == IZombieNetworkState.Phase.WAITING) {
            return "The match has not ended yet.";
        }
        int score = role == IZombieNetworkState.Role.PLANTS
            ? current.plantScore() : current.zombieScore();
        network.NetworkResponse response = client.submitScore(matchId, score);
        if (response.success()) {
            scoreSubmitted = true;
        }
        return response.message();
    }

    public IZombieNetworkState getState() {
        return state;
    }

    public IZombieNetworkState.Role getRole() {
        return role;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getStatus() {
        return status;
    }

    public boolean isScoreSubmitted() {
        return scoreSubmitted;
    }

    public Reaction pollReaction() {
        return reactions.poll();
    }

    public void leave() {
        if (closed) {
            return;
        }
        client.leaveGame();
    }

    private void onEvent(NetworkEvent event) {
        if (event == null) {
            return;
        }
        if ("GAME_STATE".equals(event.type())) {
            IZombieNetworkState decoded = NetworkClient.stateFrom(event);
            if (decoded != null && matchId.equals(decoded.matchId())) {
                state = decoded;
                status = decoded.phase().name();
            }
        } else if ("REACTION".equals(event.type()) && event.data().length >= 3) {
            reactions.offer(new Reaction(event.data()[0], event.data()[1], event.data()[2]));
        } else if ("MATCH_ABORTED".equals(event.type())) {
            status = event.data().length == 0 ? "Match aborted." : event.data()[0];
        } else if ("GAME_OVER".equals(event.type())) {
            status = event.data().length == 0 ? "Match finished." : "Winner: " + event.data()[0];
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            client.removeEventListener(listener);
        }
    }

    public record Reaction(String sender, String category, String value) {
    }
}
