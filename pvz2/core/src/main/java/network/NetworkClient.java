package network;

import models.utils.CredentialHasher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

/** TCP client used by menus and future multiplayer game controllers. */
public final class NetworkClient implements Closeable {
    private final String host;
    private final int port;
    private final BlockingQueue<NetworkResponse> responses = new LinkedBlockingQueue<>();
    private final CopyOnWriteArrayList<Consumer<NetworkEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Object requestLock = new Object();
    private volatile Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean running;

    public NetworkClient(String host, int port) {
        this.host = host == null || host.isBlank() ? "127.0.0.1" : host;
        this.port = port;
    }

    public synchronized NetworkResponse connect() {
        if (isConnected()) {
            return new NetworkResponse(true, "ALREADY_CONNECTED", "Already connected.", new String[0]);
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 2500);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            running = true;
            startReader();
            return request("HELLO");
        } catch (IOException exception) {
            close();
            return NetworkResponse.error("CONNECTION_FAILED", "Could not connect to server: " + exception.getMessage());
        }
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /** Returns true when this live client already targets the requested endpoint. */
    public boolean isConnectedTo(String candidateHost, int candidatePort) {
        if (!isConnected() || port != candidatePort) {
            return false;
        }
        String normalizedHost = candidateHost == null || candidateHost.isBlank()
            ? "127.0.0.1"
            : candidateHost.trim();
        return host.equalsIgnoreCase(normalizedHost)
            || (isLoopbackHost(host) && isLoopbackHost(normalizedHost));
    }

    private static boolean isLoopbackHost(String value) {
        return "127.0.0.1".equalsIgnoreCase(value)
            || "localhost".equalsIgnoreCase(value)
            || "::1".equalsIgnoreCase(value);
    }

    public NetworkResponse register(String username, String password, String nickname,
                                    String email, String gender, int question, String answer) {
        return request("REGISTER", username, password, nickname, email, gender,
            Integer.toString(question), answer);
    }

    public NetworkResponse login(String username, String password) {
        return request("LOGIN", username, password);
    }

    public NetworkResponse logout() {
        return request("LOGOUT");
    }

    public NetworkResponse findRandomOpponent() {
        return request("RANDOM_MATCH");
    }

    public NetworkResponse challenge(String username) {
        return request("CHALLENGE", username);
    }

    public NetworkResponse respondToChallenge(String requestId, boolean accept) {
        return request("RESPOND", requestId, accept ? "ACCEPT" : "REJECT");
    }

    public NetworkResponse cancelMatchmaking() {
        return request("CANCEL_MATCH");
    }

    public NetworkResponse joinGame(String matchId) {
        return request("GAME_JOIN", matchId);
    }

    public NetworkResponse placePlant(String matchId, String type, int column, int row) {
        return request("GAME_PLANT", matchId, type, Integer.toString(column), Integer.toString(row));
    }

    public NetworkResponse placeZombie(String matchId, String type, int column, int row) {
        return request("GAME_ZOMBIE", matchId, type, Integer.toString(column), Integer.toString(row));
    }

    public NetworkResponse sendMessage(String matchId, String receiver, String type, String contentId, String soundId) {
        return request("GAME_MESSAGE", matchId, receiver, type, contentId, soundId);
    }

    public NetworkResponse leaveGame() {
        return request("GAME_LEAVE");
    }

    public NetworkResponse submitScore(String matchId, int score) {
        return request("SCORE_SUBMIT", matchId, Integer.toString(score));
    }

    public NetworkResponse requestLeaderboard() {
        return request("LEADERBOARD");
    }

    public static IZombieNetworkState stateFrom(NetworkEvent event) {
        if (event == null || !"GAME_STATE".equals(event.type())) {
            return null;
        }
        return IZombieNetworkStateCodec.decode(event.data());
    }

    public static List<LeaderboardEntry> leaderboardFrom(NetworkResponse response) {
        if (response == null || !response.success() || response.data().length == 0) {
            return List.of();
        }
        String[] values = response.data();
        try {
            int count = Integer.parseInt(values[0]);
            List<LeaderboardEntry> result = new ArrayList<>();
            for (int index = 0; index < count && 1 + index * 3 + 2 < values.length; index++) {
                int offset = 1 + index * 3;
                result.add(new LeaderboardEntry(values[offset], values[offset + 1],
                    Integer.parseInt(values[offset + 2])));
            }
            return List.copyOf(result);
        } catch (NumberFormatException exception) {
            return List.of();
        }
    }

    public void addEventListener(Consumer<NetworkEvent> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeEventListener(Consumer<NetworkEvent> listener) {
        listeners.remove(listener);
    }

    private NetworkResponse request(String command, String... values) {
        if (!isConnected()) {
            return NetworkResponse.error("NOT_CONNECTED", "Client is not connected to a server.");
        }
        synchronized (requestLock) {
            try {
                StringBuilder line = new StringBuilder(command);
                for (String value : values) {
                    line.append('|').append(Protocol.encode(value));
                }
                writer.write(line.toString());
                writer.newLine();
                writer.flush();
                NetworkResponse response = responses.take();
                return response;
            } catch (IOException exception) {
                close();
                return NetworkResponse.error("CONNECTION_LOST", "Connection to server was lost.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return NetworkResponse.error("INTERRUPTED", "Request was interrupted.");
            }
        }
    }

    private void startReader() {
        Socket activeSocket = socket;
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    dispatch(line);
                }
            } catch (IOException ignored) {
                // The waiting request receives a connection error when the socket closes.
            } finally {
                if (socket == activeSocket) {
                    running = false;
                    responses.offer(NetworkResponse.error("CONNECTION_LOST", "Connection to server was lost."));
                }
            }
        }, "pvz-network-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void dispatch(String line) {
        String[] parts = Protocol.split(line);
        if (parts.length == 0) {
            return;
        }
        if (Protocol.RESPONSE.equals(parts[0])) {
            String code = parts.length > 1 ? Protocol.decode(parts[1]) : "UNKNOWN";
            String message = parts.length > 2 ? Protocol.decode(parts[2]) : "";
            String[] data = new String[Math.max(0, parts.length - 3)];
            for (int index = 3; index < parts.length; index++) {
                data[index - 3] = Protocol.decode(parts[index]);
            }
            responses.offer(new NetworkResponse(isSuccessCode(code), code, message, data));
        } else if (Protocol.EVENT.equals(parts[0]) && parts.length > 1) {
            String[] data = new String[Math.max(0, parts.length - 2)];
            for (int index = 2; index < parts.length; index++) {
                data[index - 2] = Protocol.decode(parts[index]);
            }
            NetworkEvent event = new NetworkEvent(Protocol.decode(parts[1]), data);
            for (Consumer<NetworkEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (RuntimeException ignored) {
                    // A UI listener must not terminate the network reader thread.
                }
            }
        }
    }

    private boolean isSuccessCode(String code) {
        return "OK".equals(code)
            || "REGISTERED".equals(code)
            || "LOGGED_IN".equals(code)
            || "WAITING".equals(code)
            || "MATCHED".equals(code)
            || "REQUEST_SENT".equals(code)
            || "REQUEST_ACCEPTED".equals(code)
            || "REQUEST_REJECTED".equals(code)
            || "MATCH_CANCELLED".equals(code)
            || "GAME_JOINED".equals(code)
            || "ACTION_ACCEPTED".equals(code)
            || "GAME_LEFT".equals(code)
            || "SCORE_UPDATED".equals(code)
            || "LEADERBOARD".equals(code);
    }

    public static AccountSnapshot accountFrom(NetworkResponse response) {
        if (response == null || !response.success() || response.data().length < 5) {
            return null;
        }
        try {
            return new AccountSnapshot(response.data()[0], response.data()[1], response.data()[2],
                response.data()[3], Integer.parseInt(response.data()[4]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String hashForCompatibility(String password) {
        return CredentialHasher.hash(password);
    }

    @Override
    public synchronized void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // Closing an already closed socket is harmless.
        } finally {
            socket = null;
            reader = null;
            writer = null;
            responses.clear();
        }
    }
}
