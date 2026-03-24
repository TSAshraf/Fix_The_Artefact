import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Profile-based save/load for GameState without external libraries.
 *
 * Save location:
 *   Windows: %APPDATA%/FixTheArtefact/profiles/{name}.json
 *   macOS/Linux: ~/.fix-the-artefact/profiles/{name}.json
 */
public final class SaveManager {
    private SaveManager() {}

    private static final String APP_DIR_WIN = "FixTheArtefact";
    private static final String APP_DIR_NIX = ".fix-the-artefact";
    private static final String PROFILES_DIR = "profiles";

    private static String activeProfile = null;

    // --------------------- Profile management ---------------------

    public static void setActiveProfile(String name) {
        activeProfile = name;
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static boolean profileExists(String name) {
        return name != null && Files.exists(profilePath(name));
    }

    /** Returns sorted list of all profile names. */
    public static List<String> listProfiles() {
        Path dir = profilesDir();
        if (!Files.isDirectory(dir)) return Collections.emptyList();

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        String fn = p.getFileName().toString();
                        return fn.substring(0, fn.length() - 5); // strip .json
                    })
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** Summary for display in the profile picker. Returns null if profile missing. */
    public static String profileSummary(String name) {
        GameState gs = loadProfile(name);
        if (gs == null) return null;

        int completed = 0;
        for (GameState.ProgressEntry pe : gs.progress.values()) {
            if (pe.completed) completed++;
        }
        String col = gs.currentCollection != null ? gs.currentCollection : "—";
        return name + " — " + completed + " completed | " + gs.elapsedSeconds + "s";
    }

    public static GameState loadProfile(String name) {
        if (name == null) return null;
        Path p = profilePath(name);
        if (!Files.exists(p)) return null;
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            GameState gs = parse(json);
            if (gs != null) gs.profileName = name;
            return gs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteProfile(String name) {
        if (name == null) return;
        try {
            Files.deleteIfExists(profilePath(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameState loadOrDefault() {
        if (activeProfile == null) return new GameState();
        GameState gs = loadProfile(activeProfile);
        return (gs != null) ? gs : new GameState();
    }

    public static void save(GameState s) {
        if (s == null || activeProfile == null) return;
        s.profileName = activeProfile;

        Path p = profilePath(activeProfile);
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(
                    p,
                    toJson(s),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --------------------- Path helpers ---------------------

    private static Path profilesDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        if (isWindows) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                return Paths.get(System.getProperty("user.home"), APP_DIR_WIN, PROFILES_DIR);
            }
            return Paths.get(appData, APP_DIR_WIN, PROFILES_DIR);
        } else {
            return Paths.get(System.getProperty("user.home"), APP_DIR_NIX, PROFILES_DIR);
        }
    }

    private static Path profilePath(String name) {
        return profilesDir().resolve(sanitize(name) + ".json");
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        // Keep letters, digits, spaces, hyphens, underscores
        return name.replaceAll("[^a-zA-Z0-9 _\\-]", "").trim();
    }

    // --------------------- JSON writing ---------------------

    private static String toJson(GameState s) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"version\": 2,\n");
        sb.append("  \"profileName\": ").append(q(s.profileName)).append(",\n");
        sb.append("  \"theme\": ").append(q(s.theme)).append(",\n");
        sb.append("  \"currentCollection\": ").append(q(s.currentCollection)).append(",\n");
        sb.append("  \"selectedJigsawIndex\": ").append(s.selectedJigsawIndex).append(",\n");
        sb.append("  \"selectedJigsawPath\": ").append(q(s.selectedJigsawPath)).append(",\n");
        sb.append("  \"elapsedSeconds\": ").append(s.elapsedSeconds).append(",\n");
        sb.append("  \"timerRunning\": ").append(s.timerRunning).append(",\n");
        sb.append("  \"musicPlaying\": ").append(s.musicPlaying).append(",\n");
        sb.append("  \"zenMode\": ").append(s.zenMode).append(",\n");

        // Difficulty
        sb.append("  \"difficultyMode\": ").append(q(s.difficulty.mode)).append(",\n");
        sb.append("  \"difficultyRows\": ").append(s.difficulty.rows).append(",\n");
        sb.append("  \"difficultyCols\": ").append(s.difficulty.cols).append(",\n");

        // Favourites
        sb.append("  \"favourites\": [");
        if (s.favourites != null && !s.favourites.isEmpty()) {
            int fi = 0;
            for (String fav : s.favourites) {
                if (fi > 0) sb.append(",");
                sb.append("\n    ").append(q(fav));
                fi++;
            }
            sb.append("\n  ");
        }
        sb.append("],\n");

        // Progress map
        sb.append("  \"progress\": {");
        int i = 0;
        for (var entry : s.progress.entrySet()) {
            GameState.ProgressEntry pe = entry.getValue();
            if (i > 0) sb.append(",");
            sb.append("\n    ").append(q(entry.getKey())).append(": {");
            sb.append("\n      \"completed\": ").append(pe.completed).append(",");
            sb.append("\n      \"bestDifficulty\": ").append(q(pe.bestDifficulty)).append(",");
            sb.append("\n      \"bestTimeSeconds\": ").append(pe.bestTimeSeconds).append(",");
            sb.append("\n      \"attempts\": ").append(pe.attempts).append(",");
            sb.append("\n      \"collectionPath\": ").append(q(pe.collectionPath)).append(",");
            sb.append("\n      \"lastPlayedEpoch\": ").append(pe.lastPlayedEpoch);
            sb.append("\n    }");
            i++;
        }
        if (!s.progress.isEmpty()) sb.append("\n  ");
        sb.append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String q(String s) {
        if (s == null) return "null";
        String esc = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + esc + "\"";
    }

    // --------------------- JSON reading ---------------------

    private static GameState parse(String json) {
        if (json == null) return null;

        GameState s = new GameState();

        s.profileName = getString(json, "profileName", s.profileName);
        s.theme = getString(json, "theme", s.theme);
        s.currentCollection = getString(json, "currentCollection", s.currentCollection);
        s.selectedJigsawPath = getString(json, "selectedJigsawPath", s.selectedJigsawPath);

        s.selectedJigsawIndex = getInt(json, "selectedJigsawIndex", s.selectedJigsawIndex);
        s.elapsedSeconds = getInt(json, "elapsedSeconds", s.elapsedSeconds);

        s.timerRunning = getBool(json, "timerRunning", s.timerRunning);
        s.musicPlaying = getBool(json, "musicPlaying", s.musicPlaying);
        s.zenMode = getBool(json, "zenMode", s.zenMode);

        // Difficulty
        s.difficulty.mode = getString(json, "difficultyMode", s.difficulty.mode);
        s.difficulty.rows = getInt(json, "difficultyRows", s.difficulty.rows);
        s.difficulty.cols = getInt(json, "difficultyCols", s.difficulty.cols);

        // Favourites
        parseFavourites(json, s);

        // Progress map
        parseProgress(json, s);

        // Safety checks
        if (s.currentCollection == null) s.currentCollection = "/Rome/Artifacts/";
        if (s.selectedJigsawIndex < 0) s.selectedJigsawIndex = 0;
        if (s.elapsedSeconds < 0) s.elapsedSeconds = 0;

        return s;
    }

    private static String getString(String json, String key, String def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(null|\"(.*?)\")", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return def;
        String raw = m.group(1);
        if ("null".equals(raw)) return null;
        String val = m.group(2);
        if (val == null) return def;
        return val.replace("\\n", "\n").replace("\\r", "\r")
                .replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static int getInt(String json, String key, int def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (!m.find()) return def;
        try { return Integer.parseInt(m.group(1)); }
        catch (Exception ignored) { return def; }
    }

    private static boolean getBool(String json, String key, boolean def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        if (!m.find()) return def;
        return Boolean.parseBoolean(m.group(1));
    }

    private static long getLong(String json, String key, long def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (!m.find()) return def;
        try { return Long.parseLong(m.group(1)); }
        catch (Exception ignored) { return def; }
    }

    private static void parseFavourites(String json, GameState s) {
        // Find "favourites": [ ... ]
        int start = json.indexOf("\"favourites\"");
        if (start < 0) return;

        int bracketStart = json.indexOf('[', start);
        if (bracketStart < 0) return;

        int bracketEnd = json.indexOf(']', bracketStart);
        if (bracketEnd < 0) return;

        String block = json.substring(bracketStart + 1, bracketEnd);

        // Match each quoted string
        Pattern strPat = Pattern.compile("\"(.*?)\"");
        Matcher m = strPat.matcher(block);
        while (m.find()) {
            String val = m.group(1)
                    .replace("\\n", "\n").replace("\\r", "\r")
                    .replace("\\\"", "\"").replace("\\\\", "\\");
            s.favourites.add(val);
        }
    }

    private static void parseProgress(String json, GameState s) {
        int start = json.indexOf("\"progress\"");
        if (start < 0) return;

        int braceStart = json.indexOf('{', start);
        if (braceStart < 0) return;

        int depth = 1;
        int pos = braceStart + 1;
        while (pos < json.length() && depth > 0) {
            char ch = json.charAt(pos);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            pos++;
        }
        String block = json.substring(braceStart + 1, pos - 1);

        Pattern entryPat = Pattern.compile("\"(.*?)\"\\s*:\\s*\\{([^}]+)\\}");
        Matcher em = entryPat.matcher(block);

        while (em.find()) {
            String path = em.group(1)
                    .replace("\\n", "\n").replace("\\r", "\r")
                    .replace("\\\"", "\"").replace("\\\\", "\\");
            String body = em.group(2);

            GameState.ProgressEntry pe = new GameState.ProgressEntry();
            pe.completed = getBool(body, "completed", false);
            pe.bestDifficulty = getString(body, "bestDifficulty", "UNKNOWN");
            pe.bestTimeSeconds = getInt(body, "bestTimeSeconds", 0);
            pe.attempts = getInt(body, "attempts", 0);
            pe.collectionPath = getString(body, "collectionPath", null);
            pe.lastPlayedEpoch = getLong(body, "lastPlayedEpoch", 0L);

            s.progress.put(path, pe);
        }
    }
}
