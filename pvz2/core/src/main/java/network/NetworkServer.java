package network;

import models.utils.CredentialHasher;
import models.utils.AccountValidator;
import models.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Authoritative TCP server for accounts and pre-game matchmaking. */
public final class NetworkServer implements Closeable {
    public static final int DEFAULT_PORT = 47856;
    private static final String DEFAULT_FILE = ".pvz2-group-56/server-accounts.properties";

    private final int port;
    private final Path storageFile;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Session> online = new ConcurrentHashMap<>();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final ArrayDeque<Session> randomQueue = new ArrayDeque<>();
    private final Object queueLock = new Object();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public NetworkServer() {
        this(DEFAULT_PORT, Path.of(System.getProperty("user.home"), DEFAULT_FILE));
    }

    public NetworkServer(int port) {
        this(port, Path.of(System.getProperty("java.io.tmpdir"), "pvz2-server-accounts.properties"));
    }

    public NetworkServer(int port, Path storageFile) {
        this.port = port;
        this.storageFile = storageFile;
        loadAccounts();
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "pvz-network-server-acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    /** Seeds a legacy local account without exposing its password to the client. */
    public void importUser(User user) {
        if (user == null || user.getName() == null || user.getPasswordHash() == null) {
            return;
        }
        accounts.putIfAbsent(user.getName().toLowerCase(Locale.ROOT),
            new Account(user.getName(), user.getPasswordHash(), user.getNickname(),
                user.getEmail(), user.getGender(), 1, CredentialHasher.hash("")));
        saveAccounts();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread worker = new Thread(() -> serve(socket), "pvz-network-server-client");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException exception) {
                if (running) {
                    // Keep accepting after a transient socket error.
                }
            }
        }
    }

    private void serve(Socket socket) {
        Session session = new Session(socket);
        try (socket; BufferedReader input = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            session.output = output;
            String line;
            while (running && (line = input.readLine()) != null) {
                handle(session, line);
            }
        } catch (IOException ignored) {
            // Disconnects are normal when a client closes the game.
        } finally {
            disconnect(session);
        }
    }

    private void handle(Session session, String line) {
        String[] parts = Protocol.split(line);
        if (parts.length == 0) {
            return;
        }
        String command = parts[0].toUpperCase(Locale.ROOT);
        String[] values = decodeValues(parts);
        switch (command) {
            case "HELLO" -> response(session, true, "OK", "Connected.");
            case "REGISTER" -> register(session, values);
            case "LOGIN" -> login(session, values);
            case "LOGOUT" -> logout(session);
            case "RANDOM_MATCH" -> randomMatch(session);
            case "CHALLENGE" -> challenge(session, values);
            case "RESPOND" -> respond(session, values);
            case "CANCEL_MATCH" -> cancelMatch(session);
            default -> response(session, false, "UNKNOWN_COMMAND", "Unknown network command.");
        }
    }

    private String[] decodeValues(String[] parts) {
        String[] values = new String[Math.max(0, parts.length - 1)];
        for (int index = 1; index < parts.length; index++) {
            values[index - 1] = Protocol.decode(parts[index]);
        }
        return values;
    }

    private void register(Session session, String[] values) {
        if (values.length < 7 || values[0].isBlank() || values[1].isBlank()) {
            response(session, false, "INVALID_ACCOUNT", "All registration fields are required.");
            return;
        }
        String username = values[0].trim();
        String key = username.toLowerCase(Locale.ROOT);
        if (!AccountValidator.isValidUsername(username)
            || !AccountValidator.isValidNickname(values[2])
            || !AccountValidator.isValidEmail(values[3])
            || !AccountValidator.isValidGender(values[4])) {
            response(session, false, "INVALID_ACCOUNT", "Registration data is invalid.");
            return;
        }
        String passwordError = AccountValidator.getPasswordError(values[1]);
        if (passwordError != null) {
            response(session, false, "INVALID_ACCOUNT", passwordError);
            return;
        }
        if (accounts.containsKey(key)) {
            response(session, false, "USERNAME_TAKEN", "Username is already taken.");
            return;
        }
        int question;
        try {
            question = Integer.parseInt(values[5]);
        } catch (NumberFormatException exception) {
            response(session, false, "INVALID_ACCOUNT", "Security question is invalid.");
            return;
        }
        if (question < 1 || question > 5 || values[6].isBlank()) {
            response(session, false, "INVALID_ACCOUNT", "Security question or answer is invalid.");
            return;
        }
        Account account = new Account(username, CredentialHasher.hash(values[1]), values[2], values[3],
            values[4], question, CredentialHasher.hash(values[6]));
        if (accounts.putIfAbsent(key, account) != null) {
            response(session, false, "USERNAME_TAKEN", "Username is already taken.");
            return;
        }
        saveAccounts();
        responseWithAccount(session, "REGISTERED", "Account created successfully.", account);
    }

    private void login(Session session, String[] values) {
        if (values.length < 2) {
            response(session, false, "INVALID_LOGIN", "Username and password are required.");
            return;
        }
        Account account = accounts.get(values[0].trim().toLowerCase(Locale.ROOT));
        if (account == null) {
            response(session, false, "UNKNOWN_USER", "Username does not exist.");
            return;
        }
        if (!CredentialHasher.matches(values[1], account.passwordHash)) {
            response(session, false, "BAD_PASSWORD", "Incorrect password.");
            return;
        }
        Session previous = online.putIfAbsent(account.key(), session);
        if (previous != null && previous != session) {
            response(session, false, "ALREADY_ONLINE", "This account is already connected.");
            return;
        }
        session.username = account.username;
        responseWithAccount(session, "LOGGED_IN", "Logged in successfully.", account);
    }

    private void logout(Session session) {
        disconnectUser(session);
        response(session, true, "OK", "Logged out successfully.");
    }

    private void randomMatch(Session session) {
        if (!loggedIn(session)) {
            response(session, false, "NOT_LOGGED_IN", "Log in before matchmaking.");
            return;
        }
        synchronized (queueLock) {
            removeFromQueue(session);
            Session opponent = randomQueue.pollFirst();
            if (opponent == null || !online.containsKey(opponent.key())) {
                randomQueue.addLast(session);
                response(session, true, "WAITING", "Waiting for a random opponent.");
                return;
            }
            createMatch(session, opponent);
            response(session, true, "MATCHED", "Opponent found.");
        }
    }

    private void challenge(Session session, String[] values) {
        if (!loggedIn(session) || values.length == 0 || values[0].isBlank()) {
            response(session, false, "INVALID_CHALLENGE", "A logged-in opponent is required.");
            return;
        }
        Session target = online.get(values[0].trim().toLowerCase(Locale.ROOT));
        if (target == null) {
            response(session, false, "OPPONENT_UNAVAILABLE", "Opponent is offline or does not exist.");
            return;
        }
        if (target == session) {
            response(session, false, "INVALID_CHALLENGE", "You cannot challenge yourself.");
            return;
        }
        synchronized (queueLock) {
            removeFromQueue(session);
            removeFromQueue(target);
        }
        String requestId = UUID.randomUUID().toString();
        challenges.put(requestId, new Challenge(session, target));
        event(target, "GAME_REQUEST", session.username, requestId);
        response(session, true, "REQUEST_SENT", "Game request sent.", requestId);
    }

    private void respond(Session session, String[] values) {
        if (!loggedIn(session) || values.length < 2) {
            response(session, false, "INVALID_RESPONSE", "Request id and decision are required.");
            return;
        }
        Challenge challenge = challenges.remove(values[0]);
        if (challenge == null || challenge.target != session) {
            response(session, false, "REQUEST_EXPIRED", "Game request is no longer available.");
            return;
        }
        if (!online.containsKey(challenge.source.key())) {
            response(session, false, "REQUEST_EXPIRED", "The requesting player went offline.");
            return;
        }
        boolean accepted = "ACCEPT".equalsIgnoreCase(values[1]);
        event(challenge.source, "GAME_REQUEST_RESULT", session.username, Boolean.toString(accepted));
        if (accepted) {
            createMatch(challenge.source, challenge.target);
        }
        response(session, true, accepted ? "REQUEST_ACCEPTED" : "REQUEST_REJECTED",
            accepted ? "Game request accepted." : "Game request rejected.");
    }

    private void createMatch(Session first, Session second) {
        String matchId = UUID.randomUUID().toString();
        event(first, "MATCH_FOUND", second.username, matchId, "PLANTS");
        event(second, "MATCH_FOUND", first.username, matchId, "ZOMBIES");
    }

    private void cancelMatch(Session session) {
        synchronized (queueLock) {
            removeFromQueue(session);
        }
        response(session, true, "MATCH_CANCELLED", "Matchmaking cancelled.");
    }

    private void removeFromQueue(Session session) {
        randomQueue.removeIf(item -> item == session);
    }

    private boolean loggedIn(Session session) {
        return session.username != null && online.get(session.key()) == session;
    }

    private void disconnect(Session session) {
        synchronized (queueLock) {
            removeFromQueue(session);
        }
        disconnectUser(session);
    }

    private void disconnectUser(Session session) {
        if (session.username != null) {
            online.remove(session.key(), session);
            session.username = null;
        }
    }

    private void response(Session session, boolean success, String code, String message, String... data) {
        StringBuilder line = new StringBuilder(Protocol.RESPONSE)
            .append('|').append(Protocol.encode(success ? "OK" : code))
            .append('|').append(Protocol.encode(message));
        for (String value : data) {
            line.append('|').append(Protocol.encode(value));
        }
        session.send(line.toString());
    }

    private void responseWithAccount(Session session, String code, String message, Account account) {
        response(session, true, code, message, account.username, account.nickname, account.email,
            account.gender, Integer.toString(account.highestScore));
    }

    private void event(Session session, String type, String... values) {
        StringBuilder line = new StringBuilder(Protocol.EVENT)
            .append('|').append(Protocol.encode(type));
        for (String value : values) {
            line.append('|').append(Protocol.encode(value));
        }
        session.send(line.toString());
    }

    private void loadAccounts() {
        if (!Files.exists(storageFile)) {
            return;
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(storageFile)) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String[] fields = properties.getProperty(key, "").split("\\t", -1);
                if (fields.length >= 7) {
                    accounts.put(key, new Account(Protocol.decode(fields[0]), Protocol.decode(fields[1]),
                        Protocol.decode(fields[2]), Protocol.decode(fields[3]), Protocol.decode(fields[4]),
                        Integer.parseInt(Protocol.decode(fields[5])), Protocol.decode(fields[6])));
                }
            }
        } catch (Exception ignored) {
            accounts.clear();
        }
    }

    private synchronized void saveAccounts() {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Properties properties = new Properties();
            for (Account account : accounts.values()) {
                String value = String.join("\t", Protocol.encode(account.username),
                    Protocol.encode(account.passwordHash), Protocol.encode(account.nickname),
                    Protocol.encode(account.email), Protocol.encode(account.gender),
                    Protocol.encode(Integer.toString(account.question)), Protocol.encode(account.answerHash));
                properties.setProperty(account.key(), value);
            }
            Path temporary = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
            try (var output = Files.newOutputStream(temporary)) {
                properties.store(output, "PVZ network accounts");
            }
            Files.move(temporary, storageFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // A temporary persistence failure must not crash the server.
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // Closing an already closed socket is harmless.
        }
        online.clear();
        synchronized (queueLock) {
            randomQueue.clear();
        }
    }

    private static final class Session {
        private final Socket socket;
        private BufferedWriter output;
        private String username;

        private Session(Socket socket) {
            this.socket = socket;
        }

        private String key() {
            return username == null ? "" : username.toLowerCase(Locale.ROOT);
        }

        private synchronized void send(String line) {
            if (output == null) {
                return;
            }
            try {
                output.write(line);
                output.newLine();
                output.flush();
            } catch (IOException ignored) {
                try {
                    socket.close();
                } catch (IOException ignoredClose) {
                    // Already closed.
                }
            }
        }
    }

    private record Challenge(Session source, Session target) {
    }

    private record Account(String username, String passwordHash, String nickname, String email,
                           String gender, int question, String answerHash, int highestScore) {
        private Account(String username, String passwordHash, String nickname, String email,
                        String gender, int question, String answerHash) {
            this(username, passwordHash, nickname, email, gender, question, answerHash, 0);
        }

        private String key() {
            return username.toLowerCase(Locale.ROOT);
        }
    }
}
