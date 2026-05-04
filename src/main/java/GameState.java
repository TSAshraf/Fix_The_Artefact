import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    public Map<String, ProgressEntry> progress = new HashMap<>(); // Always initialise so you never get null checks everywhere

    public static class ProgressEntry {
        public String jigsawPath; // e.g. "/Rome/Artifacts/somePuzzle.png"
        public String collectionPath; // e.g. "/Rome/Artifacts/"
        public String collection; // e.g. "/Rome/Artifacts/"

        // progress stats
        public boolean completed;
        public String bestDifficulty; // "EASY" / "MEDIUM" / "HARD" / "CUSTOM"
        public int bestTimeSeconds;
        public int attempts;

        // misc
        public long lastPlayedEpoch;
    }

    // UI / settings
    public String theme = "DARK"; // "DARK" or "LIGHT" (match your Theme enum names)
    public boolean musicPlaying = true;
    public String currentMusicTrack = ""; // resource filename of the currently selected track ("" = use default)
    public boolean zenMode = false;

    // Control bar customisation (HE-23). Both default to letting FixThePotGamePanel use its
    // built-in defaults; the parser only overrides when the field is actually present in the
    // saved JSON, so existing profiles get a one-time migration to the new defaults below.
    // Stable IDs of buttons hidden in NORMAL mode. Default: all visible.
    public List<String> hiddenButtons = new ArrayList<>();

    // Stable IDs of buttons hidden in ZEN mode. Default: everything except navigation + help + settings.
    public List<String> zenHiddenButtons = new ArrayList<>(java.util.Arrays.asList(
            "back", "music", "trackChooser", "restart", "timer", "info",
            "showCompleted", "split", "chooser", "theme", "hint", "favourite"
    ));
    // Stable IDs of buttons in their display order (shared across modes).
    // Empty = use FixThePotGamePanel's default order.
    public List<String> buttonOrder = new ArrayList<>();

    // True once the first-launch tutorial has run for this profile. Reset via Settings \u2192 Show tutorial again/
    public boolean tutorialShown = false;

    // Toolbar / assembly-area customisation (Tier 2 & 3)
    // Icon size in px for the game toolbar buttons. Chosen from {20, 24, 28, 32}.
    public int toolbarIconSize = 24;
    // Assembly-area (puzzle canvas) background colour in Normal mode, packed ARGB. Default: opaque black.
    public int assemblyAreaColorNormal = 0xFF000000;
    // Assembly-area (puzzle canvas) background colour in Zen mode, packed ARGB. Default: opaque black.
    public int assemblyAreaColorZen = 0xFF000000;
    // When true, the toolbar fades out after a period of mouse inactivity over the game area.
    public boolean autoHideToolbar = false;

    // Game-panel background dim percentage (0\u201380). Higher values paint a
    // darker translucent overlay on top of the themed background image so users
    // can tone down visual noise during gameplay without losing the themed
    // imagery entirely. Added in response to participant usability feedback.

    public int backgroundDim = 0;

    // Timer
    public boolean timerRunning = true;
    public int elapsedSeconds = 0;

    // Navigation / last selections
    public String currentCollection = "/Rome/Artifacts/";
    public int selectedJigsawIndex = 0; // index in imageComboBox
    public String selectedJigsawPath = null; // optional (handy if indices change)

    // Difficulty (good for later)
    public Difficulty difficulty = new Difficulty();

    public static class Difficulty {
        public String mode = "MEDIUM"; // EASY/MEDIUM/HARD/CUSTOM
        public int rows = 0; // used only if CUSTOM
        public int cols = 0;
    }

    public int xp = 0;
    public int level = 1;
    public List<String> achievements = new ArrayList<>();
    public String avatarStyle = "adventurer";
    public String avatarSeed = "";
    public Map<String, String> avatarOptions = new HashMap<>(); // hair, eyes, mouth, etc.
    public String avatarImagePath = "";  // cached local path to the avatar PNG
    public String profileName = "";
    public List<String> favourites = new ArrayList<>();
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
