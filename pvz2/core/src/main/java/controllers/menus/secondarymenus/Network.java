package controllers.menus.secondarymenus;

import controllers.menus.Menu;
import models.App;
import network.NetworkClient;
import network.NetworkEvent;
import network.NetworkService;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** Controller for the network menu and pre-game matchmaking actions. */
public class Network implements Menu, AutoCloseable {
    private final ConcurrentLinkedQueue<NetworkEvent> pendingEvents =
        new ConcurrentLinkedQueue<>();
    private final Consumer<NetworkEvent> eventListener = pendingEvents::offer;
    private boolean closed;

    public Network() {
        NetworkService.addEventListener(eventListener);
    }

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Network menu.";
    }

    @Override
    public String exitMenu() {
        close();
        App.setScreen(new view.HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Network Menu ---\n"
            + "connect <ip> <port> | host <port> | find opponent | "
            + "challenge <username> | respond <request-id> <accept|reject>";
    }

    public String connectToServer(String ip, int port) {
        if (ip == null || ip.isBlank()) {
            return "Error: server address cannot be empty.";
        }
        if (!validPort(port)) {
            return "Error: port must be between 1 and 65535.";
        }
        try {
            return NetworkService.connect(ip.trim(), port).message();
        } catch (RuntimeException exception) {
            return "Error: could not connect to server.";
        }
    }

    public String hostServer(int port) {
        if (!validPort(port)) {
            return "Error: port must be between 1 and 65535.";
        }
        try {
            return NetworkService.host(port).message();
        } catch (RuntimeException exception) {
            return "Error: could not start server.";
        }
    }

    public String findRandomOpponent() {
        NetworkClient client = NetworkService.getClient();
        return client == null
            ? "Error: connect to a server first."
            : client.findRandomOpponent().message();
    }

    public String challenge(String username) {
        if (username == null || username.isBlank()) {
            return "Error: opponent username cannot be empty.";
        }
        NetworkClient client = NetworkService.getClient();
        return client == null
            ? "Error: connect to a server first."
            : client.challenge(username.trim()).message();
    }

    public String respondToChallenge(String requestId, boolean accept) {
        if (requestId == null || requestId.isBlank()) {
            return "Error: request id cannot be empty.";
        }
        NetworkClient client = NetworkService.getClient();
        return client == null
            ? "Error: connect to a server first."
            : client.respondToChallenge(requestId.trim(), accept).message();
    }

    public String cancelMatchmaking() {
        NetworkClient client = NetworkService.getClient();
        return client == null
            ? "Error: connect to a server first."
            : client.cancelMatchmaking().message();
    }

    public NetworkEvent pollEvent() {
        return pendingEvents.poll();
    }

    private boolean validPort(int port) {
        return port >= 1 && port <= 65535;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            NetworkService.removeEventListener(eventListener);
        }
    }
}
