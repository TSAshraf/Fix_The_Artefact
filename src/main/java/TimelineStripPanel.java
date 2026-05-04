import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Horizontal scrollable timeline strip showing artefact thumbnails in
// chronological order. Each card displays a small thumbnail, the artefact
// name, and its date. Completed puzzles get a checkmark overlay.
// Replaces the plain JComboBox jigsaw selector.

public class TimelineStripPanel extends JPanel implements ThemeAware {

    public interface SelectionListener {
        void onTimelineSelected(String imagePath, int index);
    }

    private SelectionListener selectionListener;
    private final List<CardPanel> cards = new ArrayList<>();
    private final JPanel stripPanel;
    private final JScrollPane scrollPane;
    private final JLabel dateAxisLabel;

    private int selectedIndex = -1;
    private String collectionPath;

    public TimelineStripPanel() {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);

        // The horizontal strip of cards
        stripPanel = new JPanel();
        stripPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        stripPanel.setOpaque(false);

        scrollPane = new JScrollPane(stripPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(60);

        add(scrollPane, BorderLayout.CENTER);

        // Date axis label at bottom
        dateAxisLabel = new JLabel("", SwingConstants.CENTER);
        dateAxisLabel.setBorder(new EmptyBorder(2, 0, 2, 0));
        add(dateAxisLabel, BorderLayout.SOUTH);
    }

    public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }

    // Load timeline entries for the given collection, checking completion against save state.
    public void loadCollection(String collectionPath, Set<String> completedPaths) {
        this.collectionPath = collectionPath;
        stripPanel.removeAll();
        cards.clear();
        selectedIndex = -1;

        List<TimelineData.Entry> entries = TimelineData.forCollection(collectionPath);
        if (entries.isEmpty()) {
            // Fallback: no timeline data
            dateAxisLabel.setText("");
            revalidate();
            repaint();
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            TimelineData.Entry entry = entries.get(i);
            boolean completed = completedPaths != null && completedPaths.contains(entry.imagePath);
            CardPanel card = new CardPanel(entry, i, completed);
            cards.add(card);
            stripPanel.add(card);
        }

        // Date axis: show range
        String earliest = entries.get(0).dateLabel;
        String latest = entries.get(entries.size() - 1).dateLabel;
        dateAxisLabel.setText(earliest + "  \u2192  " + latest);

        refreshTheme();
        revalidate();
        repaint();
    }

    // Programmatically select a card by image path.
    public void selectByPath(String imagePath) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).entry.imagePath.equals(imagePath)) {
                selectCard(i, false);
                return;
            }
        }
    }

    // Select a card by index.
    public void selectCard(int index, boolean notify) {
        if (index < 0 || index >= cards.size()) return;
        int old = selectedIndex;
        selectedIndex = index;
        if (old >= 0 && old < cards.size()) cards.get(old).repaint();
        cards.get(index).repaint();

        // Scroll to show the selected card
        SwingUtilities.invokeLater(() -> {
            Rectangle r = cards.get(index).getBounds();
            stripPanel.scrollRectToVisible(r);
        });

        if (notify && selectionListener != null) {
            selectionListener.onTimelineSelected(cards.get(index).entry.imagePath, index);
        }
    }

    // Mark a specific card as completed.
    public void markCompleted(String imagePath) {
        for (CardPanel c : cards) {
            if (c.entry.imagePath.equals(imagePath)) {
                c.completed = true;
                c.repaint();
                return;
            }
        }
    }

    // Get the currently selected image path, or null.
    public String getSelectedImagePath() {
        if (selectedIndex >= 0 && selectedIndex < cards.size()) {
            return cards.get(selectedIndex).entry.imagePath;
        }
        return null;
    }

    public int getCardCount() { return cards.size(); }

    // Theme

    @Override
    public void refreshTheme() {
        Theme t = ThemeManager.get().getCurrent();
        Theme.Palette.Overlay o = t.palette.overlay;
        Theme.Palette.Fonts f = t.palette.fonts;

        dateAxisLabel.setFont(f.caption);
        dateAxisLabel.setForeground(o.text);

        for (CardPanel c : cards) {
            c.refreshCardTheme(o, f);
        }
    }

    // Inner card panel
    private class CardPanel extends JPanel {
        final TimelineData.Entry entry;
        final int cardIndex;
        boolean completed;
        private BufferedImage thumbnail;
        private boolean thumbnailLoaded = false;

        private final JLabel nameLabel;
        private final JLabel dateLabel;
        private final JLabel thumbLabel;

        CardPanel(TimelineData.Entry entry, int cardIndex, boolean completed) {
            this.entry = entry;
            this.cardIndex = cardIndex;
            this.completed = completed;

            setLayout(new BorderLayout(0, 2));
            setPreferredSize(new Dimension(100, 115));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Thumbnail area
            thumbLabel = new JLabel("", SwingConstants.CENTER);
            thumbLabel.setPreferredSize(new Dimension(90, 65));
            thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(thumbLabel, BorderLayout.CENTER);

            // Name
            nameLabel = new JLabel(entry.displayName, SwingConstants.CENTER);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(nameLabel, BorderLayout.SOUTH);

            // Date
            dateLabel = new JLabel(entry.dateLabel, SwingConstants.CENTER);
            dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(dateLabel, BorderLayout.NORTH);

            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Color.GRAY, 1, true),
                    new EmptyBorder(4, 4, 4, 4)
            ));

            // Click handler
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectCard(cardIndex, true);
                }
            });

            // Load thumbnail async
            loadThumbnail();
        }

        private void loadThumbnail() {
            new SwingWorker<BufferedImage, Void>() {
                @Override
                protected BufferedImage doInBackground() {
                    try (var in = TimelineStripPanel.class.getResourceAsStream(entry.imagePath)) {
                        if (in == null) return null;
                        BufferedImage full = javax.imageio.ImageIO.read(in);
                        if (full == null) return null;
                        // Scale to fit 86*60
                        int tw = 86, th = 60;
                        double scale = Math.min((double) tw / full.getWidth(), (double) th / full.getHeight());
                        int sw = (int) (full.getWidth() * scale);
                        int sh = (int) (full.getHeight() * scale);
                        BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = scaled.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.drawImage(full, 0, 0, sw, sh, null);
                        g2.dispose();
                        return scaled;
                    } catch (Exception ex) {
                        return null;
                    }
                }

                @Override
                protected void done() {
                    try {
                        thumbnail = get();
                        thumbnailLoaded = true;
                        if (thumbnail != null) {
                            thumbLabel.setIcon(new ImageIcon(thumbnail));
                        }
                    } catch (Exception ignore) {}
                }
            }.execute();
        }

        void refreshCardTheme(Theme.Palette.Overlay o, Theme.Palette.Fonts f) {
            boolean selected = (cardIndex == selectedIndex);

            setBackground(selected ? o.buttonBg : o.cardFill);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(selected ? new Color(255, 215, 0) : o.cardStroke, selected ? 2 : 1, true),
                    new EmptyBorder(4, 4, 4, 4)
            ));
            setOpaque(true);

            nameLabel.setFont(f.caption);
            nameLabel.setForeground(o.text);
            dateLabel.setFont(new Font(f.caption.getFamily(), Font.ITALIC, f.caption.getSize() - 2));
            dateLabel.setForeground(new Color(o.text.getRed(), o.text.getGreen(), o.text.getBlue(), 180));
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);

            // Draw checkmark overlay if completed
            if (completed) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 18;
                int x = getWidth() - size - 6;
                int y = 6;
                g2.setColor(new Color(34, 139, 34)); // forest green
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                // Checkmark path
                g2.drawLine(x + 4, y + 9, x + 7, y + 13);
                g2.drawLine(x + 7, y + 13, x + 14, y + 5);
                g2.dispose();
            }

            // Gold highlight border if selected
            if (cardIndex == selectedIndex) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 215, 0, 60));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        }
    }
}
