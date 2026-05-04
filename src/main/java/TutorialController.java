import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

// State machine for the first-run tutorial. Owns a TutorialOverlay and advances
// through a fixed sequence of steps, each pointing at a component in a specific
// screen. The MainFrame notifies the controller when the user completes each
// gated action (picking a collection, a site, a jigsaw, etc.) and the controller
// advances to the next step.

public class TutorialController {

    // Sentinel returned when a step has no natural pointer target.
    public static final Rectangle NO_TARGET = null;

    // Owner notified when the tutorial finishes or is skipped.
    public interface CompletionListener {
        void onTutorialCompleted();
    }

    private final JFrame frame;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final TutorialOverlay overlay;

    // Target providers, wired by MainFrame
    private Supplier<Rectangle> collectionCardTarget;   // step 2
    private Supplier<Rectangle> mapSiteTarget;          // step 3
    private Supplier<Rectangle> jigsawRowTarget;        // step 4
    private Supplier<Rectangle> puzzleAreaTarget;       // step 5
    private Supplier<Rectangle> arrowsTarget;           // step 6 (prev arrow button)
    private Supplier<Rectangle> helpButtonTarget;       // step 7
    private Supplier<Rectangle> backButtonTarget;       // step 8

    private CompletionListener completionListener;

    // Possible steps. Some are gated by an external event (user action).
    enum Step {
        WELCOME, // message-only, Next advances
        PICK_COLLECTION, // click a collection card
        PICK_SITE, // click a site marker or View All (only if map-backed collection)
        PICK_JIGSAW, // select a jigsaw on the timeline + Play
        GAME_INTRO, // Next advances
        GAME_ARROWS, // Next advances, clarifies that left/right cycle within collection (P3, P4)
        GAME_CONTROLS, // Next advances
        GAME_BACK, // Finish advances, covers back + home buttons
        DONE
    }

    private Step currentStep = Step.WELCOME;
    private boolean active = false;

    public TutorialController(JFrame frame, CardLayout cardLayout, JPanel cardPanel) {
        this.frame = frame;
        this.cardLayout = cardLayout;
        this.cardPanel = cardPanel;
        this.overlay = new TutorialOverlay();
        this.overlay.setListener(new TutorialOverlay.ActionListener() {
            @Override public void onNext() { advance(); }
            @Override public void onSkip() { skip(); }
        });
    }

    public void setCollectionCardTarget(Supplier<Rectangle> r) { this.collectionCardTarget = r; }
    public void setMapSiteTarget(Supplier<Rectangle> r) { this.mapSiteTarget = r; }
    public void setJigsawRowTarget(Supplier<Rectangle> r) { this.jigsawRowTarget = r; }
    public void setPuzzleAreaTarget(Supplier<Rectangle> r) { this.puzzleAreaTarget = r; }
    public void setArrowsTarget(Supplier<Rectangle> r) { this.arrowsTarget = r; }
    public void setHelpButtonTarget(Supplier<Rectangle> r) { this.helpButtonTarget = r; }
    public void setBackButtonTarget(Supplier<Rectangle> r) { this.backButtonTarget = r; }

    public void setCompletionListener(CompletionListener l) { this.completionListener = l; }

    public boolean isActive() { return active; }

    // Expose the overlay so target providers can convert component bounds into its space.
    public JComponent getOverlayComponent() { return overlay; }

    // Begin the tutorial at step 1 (Welcome on Collections screen).
    public void start() {
        active = true;
        currentStep = Step.WELCOME;
        attachOverlay();
        renderStep();
    }

    // Called by MainFrame when the user picks a collection card on step 2.
    public void onCollectionSelected(boolean isMapBacked) {
        if (!active || currentStep != Step.PICK_COLLECTION) return;
        // If the collection has a map, go to the map step; otherwise skip to timeline
        currentStep = isMapBacked ? Step.PICK_SITE : Step.PICK_JIGSAW;
        // Rendering happens after the CardLayout transition, via onScreenSettled
    }

    // Called when the map panel reports a site selection on step 3.
    public void onSiteSelected() {
        if (!active || currentStep != Step.PICK_SITE) return;
        currentStep = Step.PICK_JIGSAW;
    }

    // Called when the user enters the game panel (step 4 \u2192 5).
    public void onGameScreenEntered() {
        if (!active) return;
        if (currentStep == Step.PICK_JIGSAW) {
            currentStep = Step.GAME_INTRO;
            renderStep();
        }
    }

    // Called when a puzzle is solved. If the tutorial is still running, finish it,
    // the user has demonstrated they understand the core loop, so the remaining
    // in-game tutorial steps are redundant, and the post-puzzle reflection prompt
    // should not have to compete with the tutorial overlay for screen space.

    public void onPuzzleSolved() {
        if (active) finish();
    }

    // Called by MainFrame after any CardLayout transition so we can re-measure the
    // target (panels may not have been laid out when the step transition fired).
    public void onScreenSettled() {
        if (!active) return;
        // Defer to the next EDT pass so the target component has finished laying out.
        // Uses the coalescing path so rapid back-to-back transitions don't queue up.
        scheduleRender();
    }

    // Manual advance for non-gated steps (Next / Finish button).
    private void advance() {
        if (!active) return;
        switch (currentStep) {
            case WELCOME:        currentStep = Step.PICK_COLLECTION; break;
            case GAME_INTRO:     currentStep = Step.GAME_ARROWS;     break;
            case GAME_ARROWS:    currentStep = Step.GAME_CONTROLS;   break;
            case GAME_CONTROLS:  currentStep = Step.GAME_BACK;       break;
            case GAME_BACK:      finish(); return;
            default: break;
        }
        renderStep();
    }

    private void skip() {
        finish();
    }

    private void finish() {
        currentStep = Step.DONE;
        active = false;
        detachOverlay();
        if (completionListener != null) completionListener.onTutorialCompleted();
    }

    private java.awt.event.ComponentAdapter resizeListener;

    // Coalescing flag: prevents more than one renderStep invocation from sitting
    // in the EDT queue at once. During a drag-resize, componentResized fires
    // 60×/sec, without this flag every event queues a fresh render task and the
    // queue piles up faster than Swing can drain it. With it, resize events still
    // trigger re-layout at the event rate but redundant queued tasks are skipped.
    private boolean renderPending = false;

    // Schedule renderStep() to run on the next EDT pass, coalescing duplicates.
    private void scheduleRender() {
        if (renderPending) return;
        renderPending = true;
        SwingUtilities.invokeLater(() -> {
            renderPending = false;
            renderStep();
        });
    }

    // Install the overlay on the layered pane, sized to fill the root.
    private void attachOverlay() {
        JRootPane root = frame.getRootPane();
        if (root == null) return;
        JLayeredPane layered = root.getLayeredPane();
        if (overlay.getParent() != null) overlay.getParent().remove(overlay);
        overlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(overlay, JLayeredPane.MODAL_LAYER);
        overlay.setVisible(true);
        overlay.refreshTheme();
        overlay.requestFocusInWindow();

        // Keep the overlay sized to the window on resize AND re-measure its
        // spotlight target once the underlying panels have finished laying out.
        // Reading target bounds directly inside componentResized() would return
        // stale coordinates because the map/timeline panels haven't repainted yet,
        // deferring via invokeLater runs after Swing's layout + paint pass so
        // viewAllBounds et al. reflect the new window size.
        if (resizeListener == null) {
            resizeListener = new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent e) {
                    if (!active) return;
                    overlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
                    scheduleRender();
                }
            };
            frame.addComponentListener(resizeListener);
        }
    }

    private void detachOverlay() {
        overlay.setVisible(false);
        Container parent = overlay.getParent();
        if (parent != null) {
            parent.remove(overlay);
            parent.revalidate();
            parent.repaint();
        }
    }

    // Update the overlay's content and spotlight based on the current step.
    private void renderStep() {
        if (!active) return;
        switch (currentStep) {
            case WELCOME:
                overlay.configure(
                        "Welcome!",
                        "Each card here is an ancient civilisation. You'll reassemble "
                                + "artefacts from each one, unlocking their stories as you go.",
                        null, false, false);
                break;

            case PICK_COLLECTION:
                overlay.configure(
                        "Pick a civilisation",
                        "Click any collection card to start. Some civilisations have "
                                + "an interactive map showing where each artefact was found.",
                        safeGet(collectionCardTarget), true, false);
                break;

            case PICK_SITE:
                overlay.configure(
                        "Explore the map",
                        "Each marker is an archaeological site. Click a site to see "
                                + "artefacts from that location, or choose View All Artefacts "
                                + "to see everything in chronological order.",
                        safeGet(mapSiteTarget), true, false);
                break;

            case PICK_JIGSAW:
                overlay.configure(
                        "Choose an artefact",
                        "Artefacts are listed in chronological order. Click a row to "
                                + "select it, then press Play \u2014 or double-click a row to "
                                + "jump straight in.",
                        safeGet(jigsawRowTarget), true, false);
                break;

            case GAME_INTRO:
                overlay.configure(
                        "Rebuild the artefact",
                        "Drag each fragment onto the shaded board. Pieces snap into "
                                + "place when they're close to the correct position. Take your "
                                + "time \u2014 there's no timer penalty.",
                        safeGet(puzzleAreaTarget), false, false);
                break;

            case GAME_ARROWS:
                overlay.configure(
                        "Browsing jigsaws",
                        "The left and right arrows cycle through jigsaws within this "
                                + "collection \u2014 they don't go back. To leave the game, use "
                                + "the back button (shown next).",
                        safeGet(arrowsTarget), false, false);
                break;

            case GAME_CONTROLS:
                overlay.configure(
                        "Need a hand?",
                        "Press H for a hint, ? for the full keyboard shortcut reference, "
                                + "or S to customise the toolbar. Tooltips show on hover if "
                                + "you're not sure what a button does.",
                        safeGet(helpButtonTarget), false, false);
                break;

            case GAME_BACK:
                overlay.configure(
                        "You're all set",
                        "When you're done, this back button returns you to Collections. "
                                + "The profile button next to it opens your profile directly. "
                                + "Progress, XP, and achievements all save automatically. Have fun!",
                        safeGet(backButtonTarget), false, true);
                break;

            default: break;
        }
    }

    private Rectangle safeGet(Supplier<Rectangle> s) {
        if (s == null) return null;
        try { return s.get(); } catch (Exception e) { return null; }
    }

    // Helper: convert a component's local bounds to the overlay's coordinate space
    // (which shares its root with the layered pane).
    // Returns null if the component is not currently rooted in the same window.
    public static Rectangle boundsInOverlaySpace(Component target, JComponent overlay) {
        if (target == null || overlay == null || !target.isShowing() || !overlay.isShowing()) {
            return null;
        }
        Rectangle r = new Rectangle(0, 0, target.getWidth(), target.getHeight());
        return SwingUtilities.convertRectangle(target, r, overlay);
    }
}
