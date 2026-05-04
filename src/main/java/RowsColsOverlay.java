import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.BiConsumer;

public class RowsColsOverlay extends JPanel implements ThemeAware {

    // layout / sizing constants
    private static final int MIN_VAL = 2;
    private static final int MAX_VAL = 20;

    // fonts
    private static final Font TITLE_FONT = new Font("Serif", Font.BOLD, 26);
    private static final Font LABEL_FONT = new Font("Serif", Font.PLAIN, 18);
    private static final Font BTN_FONT   = new Font("SansSerif", Font.BOLD, 18);

    // UI
    private final JPanel card = new JPanel(null);
    private final JLabel title = new JLabel("Custom split");
    private final JLabel rowsL = new JLabel("Rows:");
    private final JLabel colsL = new JLabel("Columns:");
    private final JTextField rowsField = new JTextField();
    private final JTextField colsField = new JTextField();
    private final JLabel hint = new JLabel("Enter numbers " + MIN_VAL + "–" + MAX_VAL);
    private static final Color ERROR_RED = new Color(200, 60, 60);
    private static final Font HINT_NORMAL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font HINT_ERROR  = new Font("SansSerif", Font.BOLD, 13);

    private final JButton ok = new JButton("OK");
    private final JButton cancel = new JButton("Cancel");

    private BiConsumer<Integer, Integer> onAccept;

    public RowsColsOverlay() {
        setOpaque(false);
        setLayout(null);

        // card container
        card.setLayout(null);
        card.setOpaque(true);
        add(card);

        // title + labels
        title.setFont(TITLE_FONT);
        card.add(title);

        rowsL.setFont(LABEL_FONT);
        card.add(rowsL);

        colsL.setFont(LABEL_FONT);
        card.add(colsL);

        // numeric only fields
        rowsField.setFont(LABEL_FONT);
        colsField.setFont(LABEL_FONT);
        rowsField.setHorizontalAlignment(JTextField.CENTER);
        colsField.setHorizontalAlignment(JTextField.CENTER);
        enforceDigits(rowsField);
        enforceDigits(colsField);
        card.add(rowsField);
        card.add(colsField);

        // HE-25: live validation as the user types, so feedback is immediate
        // rather than only appearing after the OK button is pressed.
        javax.swing.event.DocumentListener liveValidator = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { liveValidate(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { liveValidate(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { liveValidate(); }
        };
        rowsField.getDocument().addDocumentListener(liveValidator);
        colsField.getDocument().addDocumentListener(liveValidator);

        hint.setFont(HINT_NORMAL);
        card.add(hint);

        // buttons
        ok.setFont(BTN_FONT);
        cancel.setFont(BTN_FONT);
        ok.setFocusPainted(false);
        cancel.setFocusPainted(false);
        ok.setFocusable(false);
        cancel.setFocusable(false);
        card.add(ok);
        card.add(cancel);

        // layout calc on resize
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                int cw = Math.min(520, (int) (w * 0.8));
                int ch = 260;
                int cx = (w - cw) / 2, cy = (h - ch) / 2;
                card.setBounds(cx, cy, cw, ch);

                int m = 20;
                title.setBounds(m, m, cw - 2 * m, 34);

                int labelW = 120, fieldW = 120, rowY = 80, gap = 12;
                rowsL.setBounds(m, rowY, labelW, 28);
                rowsField.setBounds(m + labelW + gap, rowY, fieldW, 32);

                int colY = rowY + 48;
                colsL.setBounds(m, colY, labelW, 28);
                colsField.setBounds(m + labelW + gap, colY, fieldW, 32);

                hint.setBounds(m, colY + 40, cw - 2 * m, 18);

                int btnY = ch - m - 44;
                int btnW = 140, btnH = 40;
                cancel.setBounds(cw - m - btnW * 2 - 12, btnY, btnW, btnH);
                ok.setBounds(cw - m - btnW, btnY, btnW, btnH);
            }
        });

        // interactions
        ok.addActionListener(e -> tryAccept());
        cancel.addActionListener(e -> close());

        // ESC closes
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getActionMap().put("esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { close(); }
        });

        // Enter triggers OK
        rowsField.addActionListener(e -> ok.doClick());
        colsField.addActionListener(e -> ok.doClick());

        // Theme hookup
        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme.Palette p = ThemeManager.get().palette();

        // backdrop + card
        card.setBackground(p.overlay.cardFill);
        card.setBorder(new LineBorder(p.overlay.cardStroke, 2));

        // text
        title.setForeground(p.overlay.text);
        rowsL.setForeground(p.overlay.text);
        colsL.setForeground(p.overlay.text);
        hint.setForeground(p.overlay.text);

        // fields
        rowsField.setBackground(p.overlay.inputBg);
        rowsField.setForeground(p.overlay.text);
        rowsField.setCaretColor(p.overlay.text);
        colsField.setBackground(p.overlay.inputBg);
        colsField.setForeground(p.overlay.text);
        colsField.setCaretColor(p.overlay.text);

        // normal (non-error) borders for fields
        rowsField.setBorder(new LineBorder(p.overlay.inputBorder, 1));
        colsField.setBorder(new LineBorder(p.overlay.inputBorder, 1));

        // buttons
        styleButton(ok, p);
        styleButton(cancel, p);

        repaint();
    }

    private void styleButton(JButton b, Theme.Palette p) {
        b.setForeground(p.overlay.text);
        b.setBackground(p.overlay.buttonBg);
        b.setBorder(new LineBorder(p.overlay.buttonBorder));
    }

    private void enforceDigits(JTextField f) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int off, String str, AttributeSet a) throws BadLocationException {
                if (str != null && str.chars().allMatch(Character::isDigit)) super.insertString(fb, off, str, a);
            }
            @Override public void replace(FilterBypass fb, int off, int len, String str, AttributeSet a) throws BadLocationException {
                if (str != null && str.chars().allMatch(Character::isDigit)) super.replace(fb, off, len, str, a);
            }
        });
    }

    private void tryAccept() {
        Integer r = parse(rowsField.getText());
        Integer c = parse(colsField.getText());

        Theme.Palette p = ThemeManager.get().palette();
        boolean okR = validateField(rowsField, r);
        boolean okC = validateField(colsField, c);
        // HE-14: emphasise the hint line when a submission is rejected so the
        // valid range is reinforced alongside the per-field red border.
        applyHintStyle(!okR || !okC, p);

        if (okR && okC) {
            if (onAccept != null) onAccept.accept(r, c);
        }
    }

    private Integer parse(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private boolean validateField(JTextField f, Integer v) {
        Theme.Palette p = ThemeManager.get().palette();
        boolean good = v != null && v >= MIN_VAL && v <= MAX_VAL;
        applyFieldStyle(f, !good, p);
        return good;
    }

    // HE-14 + HE-25: live validation that runs on every keystroke so the user
    // sees field- and hint-level feedback before submitting. Empty fields are treated as
    // not-yet-invalid so the form doesn't show an error before the user has typed anything.
    private void liveValidate() {
        Theme.Palette p = ThemeManager.get().palette();
        Integer r = parse(rowsField.getText());
        Integer c = parse(colsField.getText());

        boolean rEmpty = rowsField.getText().isBlank();
        boolean cEmpty = colsField.getText().isBlank();
        boolean rOk = rEmpty || (r != null && r >= MIN_VAL && r <= MAX_VAL);
        boolean cOk = cEmpty || (c != null && c >= MIN_VAL && c <= MAX_VAL);

        applyFieldStyle(rowsField, !rOk, p);
        applyFieldStyle(colsField, !cOk, p);
        applyHintStyle(!rOk || !cOk, p);
    }

    private void applyFieldStyle(JTextField f, boolean error, Theme.Palette p) {
        Color border = error ? ERROR_RED : p.overlay.inputBorder;
        f.setBorder(new LineBorder(border, error ? 2 : 1));
    }

    private void applyHintStyle(boolean error, Theme.Palette p) {
        hint.setFont(error ? HINT_ERROR : HINT_NORMAL);
        hint.setForeground(error ? ERROR_RED : p.overlay.text);
    }

    // API
    public void open(JComponent glassPane, BiConsumer<Integer, Integer> onAccept) {
        this.onAccept = onAccept;
        if (getParent() != glassPane) glassPane.add(this);
        setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
        setVisible(true);
        glassPane.setVisible(true);
        glassPane.repaint();
        SwingUtilities.invokeLater(() -> rowsField.requestFocusInWindow());
    }

    public void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) p.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // dim backdrop (theme-controlled)
        Theme.Palette p = ThemeManager.get().palette();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(p.overlay.scrim);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // Optional tiny toast for legacy "defaulted to easy"
    public static void showToast(JComponent glassPane, String msg) {
        Theme.Palette p = ThemeManager.get().palette();

        JPanel toast = new JPanel(new BorderLayout());
        toast.setOpaque(true);
        toast.setBackground(p.overlay.cardFill);
        toast.setBorder(new LineBorder(p.overlay.cardStroke));

        JLabel l = new JLabel("  " + msg + "  ");
        l.setForeground(p.overlay.text);
        toast.add(l, BorderLayout.CENTER);

        int w = 360, h = 36;
        int x = (glassPane.getWidth() - w) / 2;
        int y = glassPane.getHeight() - h - 40;
        toast.setBounds(x, y, w, h);

        glassPane.add(toast);
        glassPane.setComponentZOrder(toast, 0);
        glassPane.repaint();

        new Timer(1600, e -> {
            glassPane.remove(toast);
            glassPane.repaint();
        }) {{ setRepeats(false); }}.start();
    }
}
