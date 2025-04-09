// Import JavaFX classes for embedding in Swing and Media Handling
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

// MusicPlayer class handles audio playback using JavaFX MediaPlayer
public class MusicPlayer {
    private MediaPlayer mediaPlayer; // MediaPlayer instance

    public MusicPlayer(String filePath) {
        new JFXPanel(); // Initialize JavaFX runtime for MediaPlayer support in Swing
        Media media = new Media(new File(filePath).toURI().toString()); // Media object created from file path
        mediaPlayer = new MediaPlayer(media); // Instantiates MediaPlayer with created Media
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop the music indefinitely
    }

    // Play the Audio
    public void play() {
        mediaPlayer.play();
    }

    // Pause the Audio
    public void pause() {
        mediaPlayer.pause();
    }

    // Toggle between playing and pausing the audio
    public void togglePlayPause() {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }
}