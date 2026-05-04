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

    private final List<WeakReference<Component>> registered = new ArrayList<>();

    private ThemeManager() {}

    public Theme getCurrent() {
        return current;
    }

    public Theme.Palette palette() {
        return current.palette;
    }

    public void register(Component c) {
        if (c == null) return;
        cleanupAndDedupe(c);
        registered.add(new WeakReference<>(c));
        applyThemeToComponentTree(c);
    }

    public void unregister(Component c) {
        if (c == null) return;
        for (Iterator<WeakReference<Component>> it = registered.iterator(); it.hasNext(); ) {
            Component x = it.next().get();
            if (x == null || x == c) it.remove();
        }
    }

    public void toggleTheme() {
        setTheme(current.next());
    }

    public void setTheme(Theme t) {
        if (t == null) return;
        if (t == current) return;

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
        applyRecursively(root);
        refreshThemeTree(root);

        if (root instanceof JComponent jc) {
            jc.revalidate();
            jc.repaint();
        } else {
            root.repaint();
        }
    }

    private void applyRecursively(Component c) {
        applyRecursively(c, false);
    }

    private void applyRecursively(Component c, boolean insideThemeAware) {
        if (c == null) return;

        // If this component is ThemeAware, it manages its own children's styling.
        // Apply only top-level background, then let refreshTheme() handle the rest.
        boolean selfIsThemeAware = (c instanceof ThemeAware);
        if (selfIsThemeAware && insideThemeAware) {
            return;
        }

        // If we're inside a ThemeAware parent, don't override child colours/fonts,
        // the parent's refreshTheme() will set them correctly.
        if (!insideThemeAware) {
            Theme.Palette.Base b = current.palette.base;
            Theme.Palette.Fonts f = current.palette.fonts;

            if (c instanceof JPanel || c instanceof JLayeredPane || c instanceof JRootPane) {
                c.setBackground(b.appBg);
                c.setForeground(b.text);

            } else if (c instanceof JLabel label) {
                label.setForeground(b.text);
                label.setFont(f.body);

            } else if (c instanceof AbstractButton btn) {
                btn.setBackground(b.controlBg);
                btn.setForeground(b.text);
                btn.setFont(f.button);

                if (btn instanceof JComponent jc) {
                    maybeApplyLineBorder(jc, b.controlBorder);
                }

            } else if (c instanceof JTextComponent tc) {
                tc.setBackground(b.inputBg);
                tc.setForeground(b.text);
                tc.setCaretColor(b.text);
                tc.setFont(f.body);

                if (tc instanceof JComponent jc) {
                    maybeApplyLineBorder(jc, b.inputBorder);
                }

            } else if (c instanceof JList<?> list) {
                list.setBackground(b.listBg);
                list.setForeground(b.text);
                list.setSelectionBackground(b.listSelectionBg);
                list.setFont(f.body);

            } else {
                c.setForeground(b.text);
            }
        }

        // Recurse into children, but mark that we're inside a ThemeAware subtree
        boolean childFlag = insideThemeAware || selfIsThemeAware;
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursively(child, childFlag);
            }
        }
    }

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
                it.remove();
            }
        }
    }

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
