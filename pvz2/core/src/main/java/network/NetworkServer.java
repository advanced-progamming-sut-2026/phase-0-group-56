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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Authoritative TCP server for accounts and pre-game matchmaking. */
public final class NetworkServer implements Closeable {
    public static final int DEFAULT_PORT = 47856;
    private static final String DEFAULT_FILE = ".pvz2-group-56/server-accounts.properties";

    private final int port;
    private final Path storageFile;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Session> online = new ConcurrentHashMap<>();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, MatchRuntime> matches = new ConcurrentHashMap<>();
    private final ArrayDeque<Session> randomQueue = new ArrayDeque<>();
    private final Object queueLock = new Object();
    private final ScheduledExecutorService gameTicker =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "pvz-network-match-ticker");
            thread.setDaemon(true);
            return thread;
        });
    private volatile boolean running;
    private ServerSocket serverSocket;
    private boolean tickerStarted;

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
        if (!tickerStarted) {
            tickerStarted = true;
            gameTicker.scheduleAtFixedRate(this::tickMatches, 50, 50, TimeUnit.MILLISECONDS);
        }
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
        String key = user.getName().toLowerCase(Locale.ROOT);
        accounts.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return new Account(user.getName(), user.getPasswordHash(), user.getNickname(),
                    user.getEmail(), user.getGender(), 1, CredentialHasher.hash(""),
                    user.getHighestScore());
            }
            return existing.withHighestScore(user.getHighestScore());
        });
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
            case "GAME_JOIN" -> joinGame(session, values);
            case "GAME_PLANT" -> placeNetworkPlant(session, values);
            case "GAME_ZOMBIE" -> placeNetworkZombie(session, values);
            case "GAME_MESSAGE" -> sendMessage(session, values);
            case "GAME_LEAVE" -> leaveGame(session);
            case "SCORE_SUBMIT" -> submitScore(session, values);
            case "LEADERBOARD" -> sendLeaderboard(session);
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
        disconnect(session);
        response(session, true, "OK", "Logged out successfully.");
    }

    private void randomMatch(Session session) {
        if (!loggedIn(session)) {
            response(session, false, "NOT_LOGGED_IN", "Log in before matchmaking.");
            return;
        }
        if (session.matchId != null || session.assignedMatchId != null) {
            response(session, false, "ALREADY_IN_MATCH", "You are already assigned to a match.");
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
        if (session.matchId != null || session.assignedMatchId != null) {
            response(session, false, "ALREADY_IN_MATCH", "You are already assigned to a match.");
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
        if (target.matchId != null || target.assignedMatchId != null) {
            response(session, false, "OPPONENT_BUSY", "Opponent is already in a match.");
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
        MatchRuntime runtime = new MatchRuntime(matchId, first, second);
        matches.put(matchId, runtime);
        first.assignedMatchId = matchId;
        first.assignedRole = IZombieNetworkState.Role.PLANTS;
        second.assignedMatchId = matchId;
        second.assignedRole = IZombieNetworkState.Role.ZOMBIES;
        event(first, "MATCH_FOUND", second.username, matchId, "PLANTS");
        event(second, "MATCH_FOUND", first.username, matchId, "ZOMBIES");
    }

    private void joinGame(Session session, String[] values) {
        if (!loggedIn(session) || values.length == 0 || values[0].isBlank()) {
            response(session, false, "INVALID_GAME_JOIN", "A match id is required.");
            return;
        }
        MatchRuntime runtime = matches.get(values[0]);
        if (runtime == null || !runtime.has(session)) {
            response(session, false, "MATCH_NOT_FOUND", "The match is no longer available.");
            return;
        }
        if (session.assignedRole == null || !values[0].equals(session.assignedMatchId)) {
            response(session, false, "INVALID_GAME_JOIN", "This player was not assigned to that match.");
            return;
        }
        IZombieNetworkMatch.ActionResult result = runtime.match.join(session.assignedRole);
        if (result.success()) {
            session.matchId = values[0];
            session.role = session.assignedRole;
            response(session, true, "GAME_JOINED", result.message(), session.role.name());
            broadcastState(runtime);
        } else {
            response(session, false, result.code(), result.message());
        }
    }

    private void placeNetworkPlant(Session session, String[] values) {
        if (values.length < 4) {
            response(session, false, "INVALID_ACTION", "Plant type, column and row are required.");
            return;
        }
        MatchRuntime runtime = matchFor(session, values[0]);
        if (runtime == null) {
            response(session, false, "MATCH_NOT_FOUND", "Join a match before playing.");
            return;
        }
        Integer column = parseCoordinate(values[2]);
        Integer row = parseCoordinate(values[3]);
        if (column == null || row == null) {
            response(session, false, "INVALID_ACTION", "Column and row must be integers.");
            return;
        }
        IZombieNetworkMatch.ActionResult result = runtime.match.placePlant(
            session.role, values[1], column, row);
        respondAction(session, result);
        if (result.success()) {
            broadcastState(runtime);
        }
    }

    private void placeNetworkZombie(Session session, String[] values) {
        if (values.length < 4) {
            response(session, false, "INVALID_ACTION", "Zombie type, column and row are required.");
            return;
        }
        MatchRuntime runtime = matchFor(session, values[0]);
        if (runtime == null) {
            response(session, false, "MATCH_NOT_FOUND", "Join a match before playing.");
            return;
        }
        Integer column = parseCoordinate(values[2]);
        Integer row = parseCoordinate(values[3]);
        if (column == null || row == null) {
            response(session, false, "INVALID_ACTION", "Column and row must be integers.");
            return;
        }
        IZombieNetworkMatch.ActionResult result = runtime.match.placeZombie(
            session.role, values[1], column, row);
        respondAction(session, result);
        if (result.success()) {
            broadcastState(runtime);
        }
    }

    private void sendMessage(Session session, String[] values) {
        // values: matchId, receiver, type, contentId, soundId
        if (values.length < 5) {
            response(session, false, "INVALID_MESSAGE", "Match id, receiver, type, content id and sound id are required.");
            return;
        }
        MatchRuntime runtime = matchFor(session, values[0]);
        if (runtime == null) {
            response(session, false, "MATCH_NOT_FOUND", "Join a match before sending messages.");
            return;
        }
        IZombieNetworkMatch.ActionResult result = runtime.match.sendMessage(
            session.role, values[1], values[2], values[3], values[4]);
        respondAction(session, result);
        if (result.success()) {
            Session opponent = runtime.opponent(session);
            if (opponent != null) {
                event(opponent, "MESSAGE", session.username, values[1], values[2], values[3], values[4]);
            }
        }
    }

    private void leaveGame(Session session) {
        String activeMatchId = session.matchId == null ? session.assignedMatchId : session.matchId;
        MatchRuntime runtime = activeMatchId == null ? null : matches.get(activeMatchId);
        IZombieNetworkState.Role activeRole = session.role == null ? session.assignedRole : session.role;
        if (runtime != null && activeRole != null) {
            runtime.match.leave(activeRole);
            Session opponent = runtime.opponent(session);
            if (opponent != null) {
                event(opponent, "MATCH_ABORTED", "The other player left the match.");
            }
            matches.remove(runtime.matchId);
        }
        clearMatch(session);
        response(session, true, "GAME_LEFT", "Left the match.");
    }

    private void submitScore(Session session, String[] values) {
        if (!loggedIn(session) || values.length < 2) {
            response(session, false, "INVALID_SCORE", "Match id and score are required.");
            return;
        }
        MatchRuntime runtime = matchFor(session, values[0]);
        if (runtime == null || runtime.match.snapshot().phase() == IZombieNetworkState.Phase.PLAYING
            || runtime.match.snapshot().phase() == IZombieNetworkState.Phase.WAITING) {
            response(session, false, "SCORE_NOT_READY", "Scores can only be submitted after a match ends.");
            return;
        }
        Integer score = parseCoordinate(values[1]);
        if (score == null || score < 0 || score > 1000000) {
            response(session, false, "INVALID_SCORE", "Score is invalid.");
            return;
        }
        IZombieNetworkState finished = runtime.match.snapshot();
        int authoritativeScore = session.role == IZombieNetworkState.Role.PLANTS
            ? finished.plantScore() : finished.zombieScore();
        if (score > authoritativeScore) {
            response(session, false, "INVALID_SCORE", "Submitted score exceeds the server score.");
            return;
        }
        updateHighestScore(session.username, score);
        response(session, true, "SCORE_UPDATED", "Score saved on the server.", Integer.toString(score));
    }

    private void sendLeaderboard(Session session) {
        if (!loggedIn(session)) {
            response(session, false, "NOT_LOGGED_IN", "Log in before requesting the leaderboard.");
            return;
        }
        List<Account> sorted = new ArrayList<>(accounts.values());
        sorted.sort(Comparator.comparingInt(Account::highestScore).reversed()
            .thenComparing(Account::username, String.CASE_INSENSITIVE_ORDER));
        List<String> data = new ArrayList<>();
        data.add(Integer.toString(sorted.size()));
        for (Account account : sorted) {
            data.add(account.username());
            data.add(account.nickname());
            data.add(Integer.toString(account.highestScore()));
        }
        response(session, true, "LEADERBOARD", "Leaderboard loaded.", data.toArray(String[]::new));
    }

    private void tickMatches() {
        if (!running) {
            return;
        }
        for (MatchRuntime runtime : matches.values()) {
            IZombieNetworkState before = runtime.match.snapshot();
            runtime.match.tick(0.05f);
            IZombieNetworkState after = runtime.match.snapshot();
            if (after.revision() != before.revision()) {
                broadcastState(runtime);
            }
            if (before.phase() == IZombieNetworkState.Phase.PLAYING
                && after.phase() != IZombieNetworkState.Phase.PLAYING) {
                event(runtime.first, "GAME_OVER", after.winner() == null ? "" : after.winner().name(),
                    Integer.toString(after.plantScore()), Integer.toString(after.zombieScore()));
                event(runtime.second, "GAME_OVER", after.winner() == null ? "" : after.winner().name(),
                    Integer.toString(after.plantScore()), Integer.toString(after.zombieScore()));
                updateHighestScore(runtime.first.username,
                    after.plantScore());
                updateHighestScore(runtime.second.username,
                    after.zombieScore());
            }
        }
    }

    private void broadcastState(MatchRuntime runtime) {
        String[] fields = IZombieNetworkStateCodec.encode(runtime.match.snapshot());
        event(runtime.first, "GAME_STATE", fields);
        event(runtime.second, "GAME_STATE", fields);
    }

    private MatchRuntime matchFor(Session session, String matchId) {
        if (session == null || session.matchId == null || session.role == null
            || matchId == null || !session.matchId.equals(matchId)) {
            return null;
        }
        MatchRuntime runtime = matches.get(matchId);
        return runtime != null && runtime.has(session) ? runtime : null;
    }

    private static Integer parseCoordinate(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void respondAction(Session session, IZombieNetworkMatch.ActionResult result) {
        response(session, result.success(), result.success() ? "ACTION_ACCEPTED" : result.code(),
            result.message());
    }

    private void updateHighestScore(String username, int score) {
        if (username == null) {
            return;
        }
        String key = username.toLowerCase(Locale.ROOT);
        accounts.computeIfPresent(key, (ignored, account) -> account.withHighestScore(score));
        saveAccounts();
    }

    private void cancelMatch(Session session) {
        synchronized (queueLock) {
            removeFromQueue(session);
        }
        String assignedId = session.assignedMatchId;
        if (assignedId != null) {
            MatchRuntime runtime = matches.remove(assignedId);
            if (runtime != null) {
                Session opponent = runtime.opponent(session);
                if (opponent != null) {
                    event(opponent, "MATCH_ABORTED", "The other player cancelled matchmaking.");
                    clearMatch(opponent);
                }
            }
            clearMatch(session);
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
        String activeMatchId = session.matchId == null ? session.assignedMatchId : session.matchId;
        MatchRuntime runtime = activeMatchId == null ? null : matches.get(activeMatchId);
        if (runtime != null && session.role != null) {
            runtime.match.leave(session.role);
            Session opponent = runtime.opponent(session);
            if (opponent != null) {
                event(opponent, "MATCH_ABORTED", "The other player disconnected.");
                clearMatch(opponent);
            }
            matches.remove(runtime.matchId);
        }
        clearMatch(session);
        disconnectUser(session);
    }

    private void clearMatch(Session session) {
        session.matchId = null;
        session.assignedMatchId = null;
        session.role = null;
        session.assignedRole = null;
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
                    int score = fields.length >= 8
                        ? Integer.parseInt(Protocol.decode(fields[7])) : 0;
                    accounts.put(key, new Account(Protocol.decode(fields[0]), Protocol.decode(fields[1]),
                        Protocol.decode(fields[2]), Protocol.decode(fields[3]), Protocol.decode(fields[4]),
                        Integer.parseInt(Protocol.decode(fields[5])), Protocol.decode(fields[6]), score));
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
                    Protocol.encode(Integer.toString(account.question)), Protocol.encode(account.answerHash),
                    Protocol.encode(Integer.toString(account.highestScore)));
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
        matches.clear();
        synchronized (queueLock) {
            randomQueue.clear();
        }
        gameTicker.shutdownNow();
    }

    private static final class Session {
        private final Socket socket;
        private BufferedWriter output;
        private String username;
        private String assignedMatchId;
        private IZombieNetworkState.Role assignedRole;
        private String matchId;
        private IZombieNetworkState.Role role;

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

    private static final class MatchRuntime {
        private final String matchId;
        private final Session first;
        private final Session second;
        private final IZombieNetworkMatch match;

        private MatchRuntime(String matchId, Session first, Session second) {
            this.matchId = matchId;
            this.first = first;
            this.second = second;
            this.match = new IZombieNetworkMatch(matchId);
        }

        private boolean has(Session session) {
            return first == session || second == session;
        }

        private Session opponent(Session session) {
            return first == session ? second : second == session ? first : null;
        }
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

        private Account withHighestScore(int score) {
            return score <= highestScore
                ? this
                : new Account(username, passwordHash, nickname, email, gender, question, answerHash, score);
        }
    }
}
