import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

// MusicPlayer class handles audio playback using JavaFX MediaPlayer
public class MusicPlayer {
    private MediaPlayer mediaPlayer;

    /**
     * @param resourcePath classpath resource, e.g. "/audio/background.mp3"
     */
    public MusicPlayer(String resourcePath) {
        // Initialize JavaFX runtime for MediaPlayer support in Swing
        new JFXPanel();

        URL url = MusicPlayer.class.getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("Missing audio resource: " + resourcePath);
        }

        Media media = new Media(url.toExternalForm());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    }

    public void play() {
        mediaPlayer.play();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void togglePlayPause() {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
}
