import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class MusicPlayer {
    private MediaPlayer mediaPlayer;

    public MusicPlayer(String resourcePath) {
        new JFXPanel();

        URL url = MusicPlayer.class.getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("Missing audio resource: " + resourcePath);
        }

        String mediaUri;
        if ("jar".equals(url.getProtocol())) {
            // Extract to temp file, MediaPlayer can't play from inside JARs
            try {
                String ext = resourcePath.substring(resourcePath.lastIndexOf('.'));
                File tmp = File.createTempFile("ftp-music-", ext);
                tmp.deleteOnExit();
                try (InputStream in = MusicPlayer.class.getResourceAsStream(resourcePath);
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    in.transferTo(out);
                }
                mediaUri = tmp.toURI().toString();
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract audio: " + resourcePath, e);
            }
        } else {
            mediaUri = url.toExternalForm();
        }

        Media media = new Media(mediaUri);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

        // Surface asynchronous JavaFX errors instead of silently parking in ERROR state.
        // (Without this, codec failures or stream errors mid-playback are invisible to the app.)
        mediaPlayer.setOnError(() -> {
            Throwable err = mediaPlayer.getError();
            System.err.println("[Music] MediaPlayer runtime error for "
                    + resourcePath + ": "
                    + (err != null ? err.getClass().getSimpleName() + " - " + err.getMessage() : "unknown"));
        });
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
