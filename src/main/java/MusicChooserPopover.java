import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class MusicChooserPopover extends JPanel implements ThemeAware {

    interface PickListener extends Consumer<String> { void accept(String file); }

    // theme-driven paints (used by paintComponent/card paint)
    private Color scrimBg;
    private Color cardBg;
    private Color cardStroke;

    private final JPanel card = new JPanel(null) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = (cardBg != null) ? cardBg : new Color(0, 0, 0, 220);
            Color stroke = (cardStroke != null) ? cardStroke : new Color(220, 220, 220, 160);

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

            g2.setColor(stroke);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);

            g2.dispose();
        }
    };

    private final JLabel title = new JLabel("Choose background music");
    private final JTextField search = new JTextField();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JScrollPane sp = new JScrollPane(list);

    private List<String> allFiles = List.of();
    private PickListener onPick;

    private final JButton cancel = styledButton("Cancel");
    private final JButton ok     = styledButton("OK");

    MusicChooserPopover() {
        setOpaque(false);
        setLayout(null);

        card.setOpaque(false);
        add(card);

        title.setFont(new Font("Serif", Font.BOLD, 20));
        card.add(title);

        search.setFont(new Font("SansSerif", Font.PLAIN, 16));
        card.add(search);

        list.setFont(new Font("SansSerif", Font.PLAIN, 16));
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(true);
        FantasyScrollBarUI.install(sp);
        card.add(sp);

        card.add(cancel);
        card.add(ok);

        // interactions
        cancel.addActionListener(e -> close());
        ok.addActionListener(e -> chooseSelected());

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) chooseSelected();
            }
        });

        search.getDocument().addDocumentListener(new DocumentListener() {
            void refilter() {
                String q = search.getText().toLowerCase().trim();
                model.clear();
                for (String f : allFiles) {
                    String p = pretty(f);
                    if (q.isEmpty() || p.toLowerCase().contains(q)) model.addElement(p);
                }
                if (!model.isEmpty()) list.setSelectedIndex(0);
            }
            public void insertUpdate(DocumentEvent e) { refilter(); }
            public void removeUpdate(DocumentEvent e) { refilter(); }
            public void changedUpdate(DocumentEvent e) { refilter(); }
        });

        // esc to close
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getActionMap().put("esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { close(); }
        });

        // click outside closes
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!card.getBounds().contains(e.getPoint())) close();
            }
        });

        // lay out internals when card size changes
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutCardInternals();
            }
        });

        // theme hookup
        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme.Palette p = ThemeManager.get().palette();
        Theme.Palette.Base b = p.base;
        Theme.Palette.Overlay o = p.overlay;

        // overlay paints
        scrimBg    = o.scrim;
        cardBg     = o.cardFill;
        cardStroke = o.cardStroke;

        // text
        title.setForeground(o.text);

        // input
        search.setBackground(o.inputBg);
        search.setForeground(o.text);
        search.setCaretColor(o.text);
        search.setBorder(new LineBorder(o.inputBorder));

        // list
        list.setBackground(b.listBg);
        list.setForeground(b.text);
        list.setSelectionBackground(b.listSelectionBg);
        list.setSelectionForeground(b.text);
        sp.getViewport().setBackground(list.getBackground());

        // buttons
        styleButton(cancel, o);
        styleButton(ok, o);

        repaint();
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setFocusable(false);
        return b;
    }

    private void styleButton(JButton b, Theme.Palette.Overlay o) {
        b.setForeground(o.text);
        b.setBackground(o.buttonBg);
        b.setBorder(new LineBorder(o.buttonBorder));
    }

    private void layoutCardInternals() {
        int w = card.getWidth();
        int h = card.getHeight();
        if (w <= 0 || h <= 0) return;

        int m = 16;
        title.setBounds(m, m, w - 2*m, 26);
        search.setBounds(m, m + 30, w - 2*m, 34);

        sp.setBounds(
                m,
                m + 30 + 34 + 10,
                w - 2*m,
                h - (m + 30 + 34 + 10) - (m + 44) - 10
        );

        int btnW = 120, btnH = 40, gap = 12, y = h - m - btnH;
        cancel.setBounds(w - m - (btnW * 2 + gap), y, btnW, btnH);
        ok.setBounds(w - m - btnW, y, btnW, btnH);
    }

    private String pretty(String file) {
        String name = file.replace(".mp3", "");
        return name.replace("(chosic.com)", "")
                .replace("(freetouse.com)", "")
                .replace('_', ' ')
                .trim();
    }

    private void chooseSelected() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;

        String chosenPretty = model.get(idx);
        for (String f : allFiles) {
            if (chosenPretty.equals(pretty(f))) {
                if (onPick != null) onPick.accept(f);
                break;
            }
        }
    }

    // Open a 420*340 card just above the anchor button, clamped to the glass bounds.
    void openAbove(JComponent glassPane, JComponent anchor, String[] files, PickListener onPick) {
        this.onPick = onPick;
        this.allFiles = new ArrayList<>(Arrays.asList(files));

        model.clear();
        for (String f : allFiles) model.addElement(pretty(f));
        if (!model.isEmpty()) list.setSelectedIndex(0);

        if (getParent() != glassPane) glassPane.add(this);
        setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
        setVisible(true);
        glassPane.setVisible(true);

        int cw = 420, ch = 340;

        Point anchorOnScreen = anchor.getLocationOnScreen();
        Point glassOnScreen  = glassPane.getLocationOnScreen();
        int ax = anchorOnScreen.x - glassOnScreen.x;
        int ay = anchorOnScreen.y - glassOnScreen.y;

        int cx = ax + (anchor.getWidth() - cw) / 2;
        int cy = ay - ch - 12;

        cx = Math.max(12, Math.min(cx, glassPane.getWidth() - cw - 12));
        cy = Math.max(12, Math.min(cy, glassPane.getHeight() - ch - 12));

        card.setBounds(cx, cy, cw, ch);

        refreshTheme();
        layoutCardInternals();

        revalidate();
        repaint();

        SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
    }

    void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) p.repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(scrimBg != null ? scrimBg : new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
