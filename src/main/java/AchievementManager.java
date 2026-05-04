import java.util.LinkedHashMap;
import java.util.Map;

public final class AchievementManager {
    private AchievementManager() {}

    // XP rewards by difficulty
    private static final int XP_EASY   = 50;
    private static final int XP_MEDIUM = 100;
    private static final int XP_HARD   = 200;
    private static final int XP_CUSTOM_PER_CELL = 5; // rows * cols * 5

    // Exponential levelling: XP needed = 100 * level^1.5
    public static int xpForLevel(int level) {
        return (int) (100 * Math.pow(level, 1.5));
    }

    public static int totalXpForLevel(int level) {
        int total = 0;
        for (int i = 1; i < level; i++) {
            total += xpForLevel(i);
        }
        return total;
    }

    /** Award XP for completing a puzzle. Returns XP earned. */
    public static int awardXp(GameState state, String difficulty, int rows, int cols) {
        int xp;
        switch (difficulty.toUpperCase()) {
            case "EASY":   xp = XP_EASY; break;
            case "MEDIUM": xp = XP_MEDIUM; break;
            case "HARD":   xp = XP_HARD; break;
            case "CUSTOM": xp = rows * cols * XP_CUSTOM_PER_CELL; break;
            default:       xp = XP_MEDIUM; break;
        }
        state.xp += xp;

        // Check for level up
        while (state.xp >= totalXpForLevel(state.level + 1)) {
            state.level++;
        }
        return xp;
    }

    /** Returns progress to next level as 0.0 - 1.0 */
    public static double levelProgress(GameState state) {
        int currentLevelXp = totalXpForLevel(state.level);
        int nextLevelXp = totalXpForLevel(state.level + 1);
        int range = nextLevelXp - currentLevelXp;
        if (range <= 0) return 1.0;
        return (double) (state.xp - currentLevelXp) / range;
    }

    // ─── Achievement definitions ───

    public static final Map<String, Achievement> ALL = new LinkedHashMap<>();

    // Collection paths used for per-collection achievements
    private static final String[] COLLECTION_PATHS = {
            "/Ancient Cyprus/Artifacts/",
            "/Ancient Greece/Artifacts/",
            "/Ancient Egypt/Artifacts/",
            "/Ancient Near East/Artifacts/",
            "/Rome/Artifacts/"
    };

    static {
        ALL.put("first_puzzle",       new Achievement("First Steps",        "Complete your first puzzle",               1));
        ALL.put("five_puzzles",       new Achievement("Getting the Hang",   "Complete 5 puzzles",                       5));
        ALL.put("ten_puzzles",        new Achievement("Dedicated",          "Complete 10 puzzles",                      10));
        ALL.put("twentyfive_puzzles", new Achievement("Puzzle Master",      "Complete 25 puzzles",                      25));
        ALL.put("fifty_puzzles",      new Achievement("Archaeologist",      "Complete 50 puzzles",                      50));
        ALL.put("first_hard",         new Achievement("Challenge Accepted", "Complete a Hard puzzle",                   1));
        ALL.put("first_custom",       new Achievement("Custom Crafter",     "Complete a Custom puzzle",                 1));
        ALL.put("speed_demon",        new Achievement("Speed Demon",        "Complete a puzzle in under 60 seconds",    1));
        ALL.put("no_hints",           new Achievement("Unaided",            "Complete a puzzle without using hints",     1));
        ALL.put("fav_five",           new Achievement("Curator",            "Favourite 5 artefacts",                    5));
        ALL.put("all_collection",     new Achievement("Completionist",      "Complete all puzzles in a collection",     1));
        ALL.put("level_ten",          new Achievement("Veteran",            "Reach level 10",                           10));

        // Per-collection achievements
        ALL.put("col_cyprus",    new Achievement("Jewel of the Levant",   "Complete all Ancient Cyprus puzzles",     1));
        ALL.put("col_greece",    new Achievement("Hellenic Scholar",      "Complete all Ancient Greece puzzles",     1));
        ALL.put("col_egypt",     new Achievement("Gift of the Nile",     "Complete all Ancient Egypt puzzles",      1));
        ALL.put("col_near_east", new Achievement("Cradle of Civilisation","Complete all Ancient Near East puzzles",  1));
        ALL.put("col_rome",      new Achievement("Glory of Rome",        "Complete all Ancient Rome puzzles",       1));
        ALL.put("world_conqueror", new Achievement("World Conqueror",    "Complete every puzzle in all collections",1));
    }

    public static class Achievement {
        public final String name;
        public final String description;
        public final int target;

        Achievement(String name, String description, int target) {
            this.name = name;
            this.description = description;
            this.target = target;
        }
    }

    /** Check and award any newly earned achievements. Returns list of newly unlocked IDs. */
    public static java.util.List<String> checkAchievements(GameState state) {
        java.util.List<String> newlyUnlocked = new java.util.ArrayList<>();

        int completed = 0;
        boolean hasHard = false;
        boolean hasCustom = false;
        boolean hasSpeedRun = false;
        Map<String, Integer> collectionCounts = new java.util.HashMap<>();
        Map<String, Integer> collectionTotals = new java.util.HashMap<>();

        for (var entry : state.progress.entrySet()) {
            GameState.ProgressEntry pe = entry.getValue();
            if (pe.completed) {
                completed++;
                if ("HARD".equals(pe.bestDifficulty)) hasHard = true;
                if ("CUSTOM".equals(pe.bestDifficulty)) hasCustom = true;
                if (pe.bestTimeSeconds > 0 && pe.bestTimeSeconds < 60) hasSpeedRun = true;
            }
            if (pe.collectionPath != null) {
                collectionTotals.merge(pe.collectionPath, 1, Integer::sum);
                if (pe.completed) {
                    collectionCounts.merge(pe.collectionPath, 1, Integer::sum);
                }
            }
        }

        int favCount = (state.favourites != null) ? state.favourites.size() : 0;

        checkAndAward(state, newlyUnlocked, "first_puzzle",       completed >= 1);
        checkAndAward(state, newlyUnlocked, "five_puzzles",       completed >= 5);
        checkAndAward(state, newlyUnlocked, "ten_puzzles",        completed >= 10);
        checkAndAward(state, newlyUnlocked, "twentyfive_puzzles", completed >= 25);
        checkAndAward(state, newlyUnlocked, "fifty_puzzles",      completed >= 50);
        checkAndAward(state, newlyUnlocked, "first_hard",         hasHard);
        checkAndAward(state, newlyUnlocked, "first_custom",       hasCustom);
        checkAndAward(state, newlyUnlocked, "speed_demon",        hasSpeedRun);
        checkAndAward(state, newlyUnlocked, "fav_five",           favCount >= 5);
        checkAndAward(state, newlyUnlocked, "level_ten",          state.level >= 10);

        // Check if any collection is fully completed
        boolean anyCollectionComplete = false;
        int collectionsFullyDone = 0;
        for (var cp : collectionTotals.entrySet()) {
            Integer done = collectionCounts.get(cp.getKey());
            if (done != null && done >= cp.getValue() && cp.getValue() >= 5) {
                anyCollectionComplete = true;
                collectionsFullyDone++;
            }
        }
        checkAndAward(state, newlyUnlocked, "all_collection", anyCollectionComplete);

        // Per-collection achievements
        checkAndAward(state, newlyUnlocked, "col_cyprus",
                isCollectionComplete("/Ancient Cyprus/Artifacts/", collectionCounts, collectionTotals));
        checkAndAward(state, newlyUnlocked, "col_greece",
                isCollectionComplete("/Ancient Greece/Artifacts/", collectionCounts, collectionTotals));
        checkAndAward(state, newlyUnlocked, "col_egypt",
                isCollectionComplete("/Ancient Egypt/Artifacts/", collectionCounts, collectionTotals));
        checkAndAward(state, newlyUnlocked, "col_near_east",
                isCollectionComplete("/Ancient Near East/Artifacts/", collectionCounts, collectionTotals));
        checkAndAward(state, newlyUnlocked, "col_rome",
                isCollectionComplete("/Rome/Artifacts/", collectionCounts, collectionTotals));

        // World Conqueror: all 5 collections fully complete
        checkAndAward(state, newlyUnlocked, "world_conqueror", collectionsFullyDone >= COLLECTION_PATHS.length);

        return newlyUnlocked;
    }

    private static boolean isCollectionComplete(String path,
                                                Map<String, Integer> counts, Map<String, Integer> totals) {
        Integer total = totals.get(path);
        Integer done  = counts.get(path);
        return total != null && done != null && done >= total && total >= 5;
    }

    private static void checkAndAward(GameState state, java.util.List<String> newlyUnlocked,
                                      String id, boolean condition) {
        if (condition && !state.achievements.contains(id)) {
            state.achievements.add(id);
            newlyUnlocked.add(id);
        }
    }

    /** Get progress value for an achievement (for progress bars). */
    public static int getProgress(GameState state, String achievementId) {
        int completed = 0;
        for (GameState.ProgressEntry pe : state.progress.values()) {
            if (pe.completed) completed++;
        }
        int favCount = (state.favourites != null) ? state.favourites.size() : 0;

        switch (achievementId) {
            case "first_puzzle":       return Math.min(completed, 1);
            case "five_puzzles":       return Math.min(completed, 5);
            case "ten_puzzles":        return Math.min(completed, 10);
            case "twentyfive_puzzles": return Math.min(completed, 25);
            case "fifty_puzzles":      return Math.min(completed, 50);
            case "first_hard":
                for (GameState.ProgressEntry pe : state.progress.values())
                    if (pe.completed && "HARD".equals(pe.bestDifficulty)) return 1;
                return 0;
            case "first_custom":
                for (GameState.ProgressEntry pe : state.progress.values())
                    if (pe.completed && "CUSTOM".equals(pe.bestDifficulty)) return 1;
                return 0;
            case "speed_demon":
                for (GameState.ProgressEntry pe : state.progress.values())
                    if (pe.completed && pe.bestTimeSeconds > 0 && pe.bestTimeSeconds < 60) return 1;
                return 0;
            case "fav_five":           return Math.min(favCount, 5);
            case "level_ten":          return Math.min(state.level, 10);
            case "col_cyprus":         return collectionProgressFor(state, "/Ancient Cyprus/Artifacts/");
            case "col_greece":         return collectionProgressFor(state, "/Ancient Greece/Artifacts/");
            case "col_egypt":          return collectionProgressFor(state, "/Ancient Egypt/Artifacts/");
            case "col_near_east":      return collectionProgressFor(state, "/Ancient Near East/Artifacts/");
            case "col_rome":           return collectionProgressFor(state, "/Rome/Artifacts/");
            case "world_conqueror": {
                int done = 0;
                for (String cp : COLLECTION_PATHS) {
                    if (collectionProgressFor(state, cp) >= 1) done++;
                }
                return done >= COLLECTION_PATHS.length ? 1 : 0;
            }
            default: return 0;
        }
    }

    /** Returns 1 if the collection is fully complete, 0 otherwise. */
    private static int collectionProgressFor(GameState state, String collectionPath) {
        int total = 0, done = 0;
        for (GameState.ProgressEntry pe : state.progress.values()) {
            if (collectionPath.equals(pe.collectionPath)) {
                total++;
                if (pe.completed) done++;
            }
        }
        return (total >= 5 && done >= total) ? 1 : 0;
    }
}
