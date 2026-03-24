import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;

public class LoadGamePanel extends JPanel implements ThemeAware {

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    private BufferedImage backgroundImage;
    private LoadGameListener listener;

    private JPanel buttonPanel;
    private JButton backButton;
    private OrnateFramePanel framedButtons;

    private final Dimension fixedSize = new Dimension(320, 44);
    private final float normalFont = 18f;
    private final float hoverFont  = 22f;

    public interface LoadGameListener {
        void onProfileSelected(String profileName);
        void onBackToMenu();
    }

    public void setLoadGameListener(LoadGameListener listener) {
        this.listener = listener;
    }

    public LoadGamePanel() {
        setPreferredSize(new Dimension(600, 400));
        setOpaque(true);
        setLayout(new GridBagLayout());

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // Wrap in scroll pane for many profiles
        JScrollPane scrollPane = new JScrollPane(buttonPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(340, 400));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Wrap scroll pane in a plain panel so OrnateFramePanel accepts it
        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(scrollPane, BorderLayout.CENTER);

        framedButtons = new OrnateFramePanel(
                scrollWrapper, 8, 10, 1, ORNATE_FRAME
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(18, 18, 18, 18);
        gbc.fill = GridBagConstraints.NONE;
        add(framedButtons, gbc);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    /** Call this every time you navigate to this screen. */
    public void refreshProfiles() {
        buttonPanel.removeAll();

        List<String> profiles = SaveManager.listProfiles();

        if (profiles.isEmpty()) {
            JLabel emptyLabel = new JLabel("No saved profiles found.");
            emptyLabel.setFont(new Font("Serif", Font.ITALIC, 16));
            emptyLabel.setForeground(ThemeManager.get().palette().base.text);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonPanel.add(Box.createVerticalStrut(12));
            buttonPanel.add(emptyLabel);
        } else {
            buttonPanel.add(Box.createVerticalStrut(4));
            for (String name : profiles) {
                String summary = SaveManager.profileSummary(name);
                String label = (summary != null) ? summary : name;

                JButton btn = makeMenuButton(label, fixedSize, normalFont, hoverFont);
                btn.setAlignmentX(Component.CENTER_ALIGNMENT);

                // Left-click: load profile
                btn.addActionListener(e -> {
                    if (listener != null) listener.onProfileSelected(name);
                });

                // Right-click: delete profile
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            int confirm = JOptionPane.showConfirmDialog(
                                    LoadGamePanel.this,
                                    "Delete profile '" + name + "'?",
                                    "Delete Profile",
                                    JOptionPane.YES_NO_OPTION
                            );
                            if (confirm == JOptionPane.YES_OPTION) {
                                SaveManager.deleteProfile(name);
                                refreshProfiles();
                            }
                        }
                    }
                });

                buttonPanel.add(btn);
                buttonPanel.add(Box.createVerticalStrut(8));
            }
        }

        // Back button (always at the bottom)
        buttonPanel.add(Box.createVerticalStrut(12));
        backButton = makeMenuButton("Back to Main Menu", fixedSize, normalFont, hoverFont);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setForeground(ThemeManager.get().palette().base.text);
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });
        buttonPanel.add(backButton);
        buttonPanel.add(Box.createVerticalStrut(4));

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private JButton makeMenuButton(String text, Dimension fixedSize, float normalFont, float hoverFont) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setPreferredSize(fixedSize);
        b.setMinimumSize(fixedSize);
        b.setMaximumSize(fixedSize);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverFont));
                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
                b.repaint();
            }
        });
        return b;
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor("/Rome/Artifacts/", theme);

        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(ThemeManager.get().palette().base.appBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}

