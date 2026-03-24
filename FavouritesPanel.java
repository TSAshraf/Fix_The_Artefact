import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;

public class FavouritesPanel extends JPanel implements ThemeAware {

    private JPanel listPanel;
    private JScrollPane scrollPane;
    private BufferedImage backgroundImage;
    private FavouritesListener listener;

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public interface FavouritesListener {
        void onFavouriteSelected(String jigsawPath);
        void onBackToCollections();
    }

    public void setFavouritesListener(FavouritesListener listener) {
        this.listener = listener;
    }

    public FavouritesPanel() {
        setLayout(new GridBagLayout());
        setOpaque(true);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(scrollPane, BorderLayout.CENTER);
        scrollWrapper.setPreferredSize(new Dimension(320, 300));

        OrnateFramePanel framedList = new OrnateFramePanel(
                scrollWrapper, 8, 10, 1, ORNATE_FRAME
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(18, 18, 18, 18);
        add(framedList, gbc);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    /** Rebuild the favourites list from the active profile's save state. */
    public void refreshFavourites() {
        listPanel.removeAll();

        GameState s = SaveManager.loadOrDefault();
        List<String> favs = s.favourites;

        final Dimension btnSize = new Dimension(280, 40);
        final int normalFont = 16;
        final int hoverFont  = 19;

        if (favs == null || favs.isEmpty()) {
            JLabel empty = new JLabel("No favourites yet");
            empty.setFont(new Font("Serif", Font.ITALIC, 16));
            empty.setForeground(ThemeManager.get().palette().base.text);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(12));
            listPanel.add(empty);
        } else {
            for (String path : favs) {
                String displayName = ArtifactCatalog.displayName(path);

                JButton btn = new JButton( displayName);
                btn.setFont(new Font("Serif", Font.BOLD, normalFont));
                btn.setPreferredSize(btnSize);
                btn.setMinimumSize(btnSize);
                btn.setMaximumSize(btnSize);
                btn.setOpaque(false);
                btn.setContentAreaFilled(false);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.setFocusable(false);
                btn.setForeground(ThemeManager.get().palette().base.text);
                btn.setAlignmentX(Component.CENTER_ALIGNMENT);

                btn.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        btn.setFont(btn.getFont().deriveFont(Font.BOLD, hoverFont));
                        btn.repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        btn.setFont(btn.getFont().deriveFont(Font.BOLD, normalFont));
                        btn.repaint();
                    }
                });

                // Left-click: open that jigsaw
                btn.addActionListener(e -> {
                    if (listener != null) listener.onFavouriteSelected(path);
                });

                // Right-click: unfavourite
                btn.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            int confirm = JOptionPane.showConfirmDialog(
                                    FavouritesPanel.this,
                                    "Remove \"" + displayName + "\" from favourites?",
                                    "Unfavourite",
                                    JOptionPane.YES_NO_OPTION
                            );
                            if (confirm == JOptionPane.YES_OPTION) {
                                GameState gs = SaveManager.loadOrDefault();
                                gs.toggleFavourite(path);
                                SaveManager.save(gs);
                                refreshFavourites();
                            }
                        }
                    }
                });

                listPanel.add(btn);
            }
        }

        // Back button at the bottom
        listPanel.add(Box.createVerticalStrut(12));
        JButton backBtn = new JButton("Back to Collections");
        backBtn.setFont(new Font("Serif", Font.BOLD, normalFont));
        backBtn.setPreferredSize(btnSize);
        backBtn.setMinimumSize(btnSize);
        backBtn.setMaximumSize(btnSize);
        backBtn.setOpaque(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setFocusable(false);
        backBtn.setForeground(ThemeManager.get().palette().base.text);
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                backBtn.setFont(backBtn.getFont().deriveFont(Font.BOLD, hoverFont));
                backBtn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                backBtn.setFont(backBtn.getFont().deriveFont(Font.BOLD, normalFont));
                backBtn.repaint();
            }
        });
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToCollections();
        });
        listPanel.add(backBtn);

        listPanel.revalidate();
        listPanel.repaint();
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
