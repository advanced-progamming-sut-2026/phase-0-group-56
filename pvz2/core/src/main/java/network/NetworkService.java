package network;

import controllers.datacontroller.Data;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Application-wide networking runtime shared by menu controllers. */
public final class NetworkService {
    private static NetworkServer embeddedServer;
    private static NetworkClient client;
    private static final CopyOnWriteArrayList<Consumer<NetworkEvent>> listeners = new CopyOnWriteArrayList<>();

    private NetworkService() {
    }

    /** Starts a local server for the desktop game and connects its client. */
    public static synchronized NetworkClient ensureEmbedded() {
        if (client != null && client.isConnected()) {
            return client;
        }
        if (embeddedServer == null || !embeddedServer.isRunning()) {
            embeddedServer = new NetworkServer();
            try {
                embeddedServer.start();
            } catch (IOException exception) {
                embeddedServer.close();
                embeddedServer = null;
                return connect("127.0.0.1", NetworkServer.DEFAULT_PORT).success()
                    ? client : null;
            }
        }
        client = new NetworkClient("127.0.0.1", NetworkServer.DEFAULT_PORT);
        attachListeners(client);
        NetworkResponse response = client.connect();
        if (!response.success()) {
            client.close();
            client = null;
        }
        return client;
    }

    public static synchronized void importLocalAccounts() {
        if (embeddedServer == null) {
            return;
        }
        for (models.User user : Data.getAllUsers()) {
            embeddedServer.importUser(user);
        }
    }

    public static synchronized NetworkResponse connect(String host, int port) {
        // Reconnecting to the endpoint we already use would discard the
        // server-side authenticated session. Keep the existing socket alive.
        if (client != null && client.isConnectedTo(host, port)) {
            return new NetworkResponse(true, "ALREADY_CONNECTED",
                "Already connected.", new String[0]);
        }
        disconnectClient();
        client = new NetworkClient(host, port);
        attachListeners(client);
        NetworkResponse response = client.connect();
        if (!response.success()) {
            client.close();
            client = null;
        }
        return response;
    }

    public static synchronized NetworkResponse host(int port) {
        stopEmbedded();
        embeddedServer = new NetworkServer(port);
        try {
            embeddedServer.start();
        } catch (IOException exception) {
            embeddedServer.close();
            embeddedServer = null;
            return NetworkResponse.error("HOST_FAILED", "Could not start server: " + exception.getMessage());
        }
        return connect("127.0.0.1", embeddedServer.getPort());
    }

    public static synchronized NetworkClient getClient() {
        return client;
    }

    public static synchronized boolean isConnected() {
        return client != null && client.isConnected();
    }

    public static synchronized void disconnectClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    public static void addEventListener(Consumer<NetworkEvent> listener) {
        if (listener != null) {
            listeners.add(listener);
            NetworkClient active = client;
            if (active != null) {
                active.addEventListener(listener);
            }
        }
    }

    public static void removeEventListener(Consumer<NetworkEvent> listener) {
        listeners.remove(listener);
        NetworkClient active = client;
        if (active != null) {
            active.removeEventListener(listener);
        }
    }

    private static void attachListeners(NetworkClient active) {
        for (Consumer<NetworkEvent> listener : listeners) {
            active.addEventListener(listener);
        }
    }

    public static synchronized void stopEmbedded() {
        disconnectClient();
        if (embeddedServer != null) {
            embeddedServer.close();
            embeddedServer = null;
        }
    }
}
