import java.nio.file.Path;

// Thrown when a profile save file exists but cannot be parsed as a valid GameState.
// Carries enough context for the UI to tell the user which profile failed and where
// the quarantined backup of the corrupt file lives.

public class SaveCorruptException extends RuntimeException {
    private final String profileName;
    private final Path originalPath;
    private final Path backupPath;

    public SaveCorruptException(String profileName, Path originalPath, Path backupPath, String reason) {
        super("Profile '" + profileName + "' could not be loaded: " + reason
                + (backupPath != null ? " (backed up to " + backupPath.getFileName() + ")" : ""));
        this.profileName = profileName;
        this.originalPath = originalPath;
        this.backupPath = backupPath;
    }

    public String getProfileName() { return profileName; }
    public Path getOriginalPath() { return originalPath; }
    public Path getBackupPath() { return backupPath; }
}
