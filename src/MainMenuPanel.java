import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.File;

// Main menu panel that shows a background, ornate frame, and play button
public class MainMenuPanel extends JPanel implements ActionListener {
    private FramePanel framePanel;
    private Image backgroundImage;
    private MainMenuListener listener;

    // Interface for listening to main menu actions
    public interface MainMenuListener {
        void onPlayClicked();
    }

    // Sets the listener and passes it to the frame panel (if it exists)
    public void setMainMenuListener(MainMenuListener listener) {
        this.listener = listener;
        if (framePanel != null) {
            framePanel.setMainMenuListener(listener);
        }
    }

    // Constructor: sets up layout, loads images, and adds the frame panel
    public MainMenuPanel() {
        // Use GridBagLayout to center the content.
        setLayout(new GridBagLayout());
        setOpaque(false);

        // Load the background image.
        try {
            backgroundImage = ImageIO.read(new File("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/Fantasy-Background.jpg"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load the ornate frame image.
        Image ornateFrame = null;
        try {
            ornateFrame = ImageIO.read(new File("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/Ornate-1-Photoroom.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create the frame panel.
        framePanel = new FramePanel(ornateFrame);
        // If listener is already set, pass it along.
        if (listener != null) {
            framePanel.setMainMenuListener(listener);
        }
        add(framePanel, new GridBagConstraints());
    }

    // Draws the background image
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the background image scaled to fill the panel.
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // When an action is performed, notify the listener
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.onPlayClicked();
        }
    }

    // Nested panel class: a panel that draws the ornate frame and holds the Play button.
    private class FramePanel extends JPanel implements ActionListener {
        private Image frameImage;
        private JButton playButton;
        private MainMenuListener menuListener;

        // Customize these offsets to fine-tune the frame position
        private int offsetX = 0;  // positive = move frame to the right, negative = left
        private int offsetY = -5;   // positive = move frame down, negative = up

        public FramePanel(Image frameImage) {
            this.frameImage = frameImage;
            setOpaque(false);
            setLayout(new GridBagLayout());

            // Set a preferred size based on the frame image.
            if (frameImage != null) {
                int w = frameImage.getWidth(null);
                int h = frameImage.getHeight(null);
                setPreferredSize(new Dimension(w, h));
            } else {
                setPreferredSize(new Dimension(300, 200));
            }

            // Create a play button
            playButton = new JButton("Play");
            playButton.setFont(new Font("Serif", Font.BOLD, 28));
            playButton.setOpaque(false);
            playButton.setContentAreaFilled(false);
            playButton.setBorderPainted(false);
            playButton.setFocusPainted(false);
            playButton.addActionListener(this);

            // Hover effect, increases button font size when hovered
            // Maybe also add increased button size
            playButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    playButton.setFont(playButton.getFont().deriveFont(32f));
                    playButton.revalidate();
                    playButton.repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    playButton.setFont(playButton.getFont().deriveFont(28f));
                    playButton.revalidate();
                    playButton.repaint();
                }
            });
            add(playButton, new GridBagConstraints()); // Adds the play button
        }

        // Sets the listener for play button clicks
        public void setMainMenuListener(MainMenuListener listener) {
            this.menuListener = listener;
        }

        // Paint the ornate frame image around the Play button
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (frameImage != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); // Set transparency for the frame image.

                int padding = 10;
                Rectangle btnBounds = playButton.getBounds();

                // Calculate initial frame position around the button
                int frameX = btnBounds.x - padding;
                int frameY = btnBounds.y - padding;
                int frameW = btnBounds.width + 2 * padding;
                int frameH = btnBounds.height + 2 * padding;

                // Apply manual offsets to nudge the frame for asymmetry
                frameX += offsetX;
                frameY += offsetY;

                // Draw the frame
                g2.drawImage(frameImage, frameX, frameY, frameW, frameH, this);
                g2.dispose();
            }
        }

        // When the Play button is clicked, notify the listener
        @Override
        public void actionPerformed(ActionEvent e) {
            if (menuListener != null) {
                menuListener.onPlayClicked();
            }
        }
    }
}