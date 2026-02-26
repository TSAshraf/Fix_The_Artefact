import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();
    public static ThemeManager get() { return INSTANCE; }

    private Theme current = Theme.LIGHT;

    // Weak refs so panels/popovers can be GC'd if they go away
    private final List<WeakReference<Component>> registered = new ArrayList<>();

    private ThemeManager() {}

    public Theme getCurrent() {
        return current;
    }

    /** Convenience: the full palette for the current theme. */
    public Theme.Palette palette() {
        return current.palette;
    }

    /**
     * Register a root component (frame/panel/overlay/etc). Theme will be applied immediately.
     * Call this once when the UI element is created/shown.
     */
    public void register(Component c) {
        if (c == null) return;

        // Avoid duplicates + clean dead refs while we're here
        cleanupAndDedupe(c);

        registered.add(new WeakReference<>(c));
        applyThemeToComponentTree(c); // apply immediately
    }

    public void unregister(Component c) {
        if (c == null) return;

        for (Iterator<WeakReference<Component>> it = registered.iterator(); it.hasNext(); ) {
            Component x = it.next().get();
            if (x == null || x == c) it.remove();
        }
    }

    public void toggleTheme() {
        setTheme(current.next()); // LIGHT <-> DARK
    }

    public void setTheme(Theme t) {
        if (t == null) return;
        if (t == current) return;

        // Always apply on the Swing EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setTheme(t));
            return;
        }

        current = t;
        applyToAllRegistered();
    }

    private void applyToAllRegistered() {
        for (Iterator<WeakReference<Component>> it = registered.iterator(); it.hasNext(); ) {
            Component c = it.next().get();
            if (c == null) {
                it.remove();
                continue;
            }
            applyThemeToComponentTree(c);
        }
    }

    private void applyThemeToComponentTree(Component root) {
        // Apply recursively to standard Swing components using BASE palette
        applyRecursively(root);

        // Give custom components a hook (overlays/popovers/custom painting)
        refreshThemeTree(root);

        if (root instanceof JComponent jc) {
            jc.revalidate();
            jc.repaint();
        } else {
            root.repaint();
        }
    }

    private void applyRecursively(Component c) {
        if (c == null) return;

        Theme.Palette.Base b = current.palette.base;

        // ---- Baseline styling rules (BASE palette only) ----
        if (c instanceof JPanel || c instanceof JLayeredPane || c instanceof JRootPane) {
            c.setBackground(b.appBg);
            c.setForeground(b.text);

        } else if (c instanceof JLabel label) {
            label.setForeground(b.text);

        } else if (c instanceof AbstractButton btn) {
            btn.setBackground(b.controlBg);
            btn.setForeground(b.text);

            // Only set a border if it doesn't already have a "special" border
            if (btn instanceof JComponent jc) {
                maybeApplyLineBorder(jc, b.controlBorder);
            }

        } else if (c instanceof JTextComponent tc) {
            tc.setBackground(b.inputBg);
            tc.setForeground(b.text);
            tc.setCaretColor(b.text);

            if (tc instanceof JComponent jc) {
                maybeApplyLineBorder(jc, b.inputBorder);
            }

        } else if (c instanceof JList<?> list) {
            list.setBackground(b.listBg);
            list.setForeground(b.text);
            list.setSelectionBackground(b.listSelectionBg);

        } else {
            // Default foreground for other components
            c.setForeground(b.text);
        }

        // Recurse
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursively(child);
            }
        }
    }

    /**
     * Calls ThemeAware.refreshTheme() on any ThemeAware components in the tree.
     * Overlays/popovers implement ThemeAware and apply OVERLAY palette inside refreshTheme().
     */
    public static void refreshThemeTree(Component c) {
        if (c == null) return;

        if (c instanceof ThemeAware ta) {
            ta.refreshTheme();
        }

        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshThemeTree(child);
            }
        }
    }

    private void cleanupAndDedupe(Component incoming) {
        for (Iterator<WeakReference<Component>> it = registered.iterator(); it.hasNext(); ) {
            Component existing = it.next().get();
            if (existing == null) {
                it.remove();
            } else if (existing == incoming) {
                // already registered — remove old ref so we re-add a fresh one
                it.remove();
            }
        }
    }

    /**
     * Avoid stomping on component-specific borders (e.g., custom rounded borders).
     * If the border is null or a default UIResource, we can safely replace it.
     */
    private static void maybeApplyLineBorder(JComponent jc, Color borderColor) {
        Border border = jc.getBorder();
        boolean replace =
                border == null ||
                        border.getClass().getName().contains("UIResource");

        if (replace) {
            jc.setBorder(BorderFactory.createLineBorder(borderColor));
        }
    }
}
