import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {

    // Always initialise so you never get null checks everywhere
    public Map<String, ProgressEntry> progress = new HashMap<>();

    public static class ProgressEntry {
        public String jigsawPath;            // e.g. "/Rome/Artifacts/somePuzzle.png" (optional)

        // Keep your existing field:
        public String collectionPath;        // e.g. "/Rome/Artifacts/"
        public String collection;            // e.g. "/Rome/Artifacts/"

        // --- progress stats ---
        public boolean completed;
        public String bestDifficulty;        // "EASY" / "MEDIUM" / "HARD" / "CUSTOM"
        public int bestTimeSeconds;
        public int attempts;

        // --- misc ---
        public long lastPlayedEpoch;         // epoch millis recommended
    }

    // --- UI / settings ---
    public String theme = "DARK";            // "DARK" or "LIGHT" (match your Theme enum names)
    public boolean musicPlaying = true;
    public boolean zenMode = false;

    // --- timer ---
    public boolean timerRunning = true;
    public int elapsedSeconds = 0;

    // --- navigation / last selections ---
    public String currentCollection = "/Rome/Artifacts/";
    public int selectedJigsawIndex = 0;      // index in imageComboBox
    public String selectedJigsawPath = null; // optional (handy if indices change)

    // --- difficulty (good for later) ---
    public Difficulty difficulty = new Difficulty();

    public static class Difficulty {
        public String mode = "MEDIUM";       // EASY/MEDIUM/HARD/CUSTOM
        public int rows = 0;                 // used only if CUSTOM
        public int cols = 0;
    }

    // --- future-proof fields for later ---
    public int xp = 0;
    public List<String> achievements = new ArrayList<>();
    public String profileName = ""; // profile
    public List<String> favourites = new ArrayList<>(); // favourites
    public boolean isFavourite(String path) {
        return favourites != null && favourites.contains(path);
    }

    public void toggleFavourite(String path) {
        if (favourites == null) favourites = new ArrayList<>();
        if (favourites.contains(path)) {
            favourites.remove(path);
        } else {
            favourites.add(path);
        }
    }

}
