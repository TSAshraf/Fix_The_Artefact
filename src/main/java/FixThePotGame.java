import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FixThePotGame extends JPanel implements MouseListener, MouseMotionListener {
    private ArrayList<PuzzlePiece> pieces;      // pieces in logical (image) coords
    private PuzzlePiece selectedPiece = null;
    private int offsetX, offsetY;               // drag offsets in logical coords
    private final int snapThreshold = 100;      // screen-pixel feel, converted to logical

    private BufferedImage potImage;

    private int puzzleRows = 2;
    private int puzzleCols = 2;

    // view transform
    private double viewScale = 1.0;             // screen = draw + logical * scale
    private double drawX = 0;
    private double drawY = 0;

    // solved listener
    public interface PuzzleSolvedListener { void puzzleSolved(); }
    private PuzzleSolvedListener solvedListener;
    public void setPuzzleSolvedListener(PuzzleSolvedListener l) { this.solvedListener = l; }

    public FixThePotGame() {
        try {
            potImage = ImageIO.read(new File("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-1.jpg"));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load jug-1.jpg");
            System.exit(1);
        }
        pieces = new ArrayList<>();
        createPieces();
        addMouseListener(this);
        addMouseMotionListener(this);
        setOpaque(false);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(1000, 1000); }

    private void createPieces() {
        pieces.clear();
        int pieceW = potImage.getWidth()  / puzzleCols;
        int pieceH = potImage.getHeight() / puzzleRows;

        for (int r = 0; r < puzzleRows; r++) {
            for (int c = 0; c < puzzleCols; c++) {
                int cx = c * pieceW;  // correct logical X
                int cy = r * pieceH;  // correct logical Y
                BufferedImage img = potImage.getSubimage(cx, cy, pieceW, pieceH);
                PuzzlePiece p = new PuzzlePiece(img, cx, cy);

                // random logical start area (same as before)
                p.x = (int) (Math.random() * (1000 - pieceW));
                p.y = (int) (Math.random() * (1000 - pieceH));
                pieces.add(p);
            }
        }
        Collections.shuffle(pieces);
    }

    public void restartGame() { createPieces(); repaint(); }
    public void setDifficulty(int rows, int cols) {
        this.puzzleRows = Math.max(1, rows);
        this.puzzleCols = Math.max(1, cols);
        restartGame();
    }
    public BufferedImage getPotImage() { return potImage; }
    public void setImage(String filePath) {
        try { potImage = ImageIO.read(new File(filePath)); }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load " + filePath);
            return;
        }
        restartGame();
    }

    // --- view math ---
    private void updateViewTransform() {
        int pw = getWidth(), ph = getHeight();
        int iw = potImage.getWidth(), ih = potImage.getHeight();
        if (pw <= 0 || ph <= 0 || iw <= 0 || ih <= 0) {
            viewScale = 1.0; drawX = drawY = 0; return;
        }
        double sx = pw / (double) iw, sy = ph / (double) ih;
        viewScale = Math.min(1.0, Math.min(sx, sy)); // never upscale
        double sw = iw * viewScale, sh = ih * viewScale;
        drawX = (pw - sw) / 2.0;
        drawY = (ph - sh) / 2.0;
    }
    private int screenToLogicalX(int sx) { return (int)Math.round((sx - drawX) / viewScale); }
    private int screenToLogicalY(int sy) { return (int)Math.round((sy - drawY) / viewScale); }
    private int logicalToScreenX(int lx) { return (int)Math.round(drawX + lx * viewScale); }
    private int logicalToScreenY(int ly) { return (int)Math.round(drawY + ly * viewScale); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (potImage == null) return;

        updateViewTransform();
        Graphics2D g2 = (Graphics2D) g.create();

        // assembly area background
        int areaX = (int)Math.round(drawX);
        int areaY = (int)Math.round(drawY);
        int areaW = (int)Math.round(potImage.getWidth()  * viewScale);
        int areaH = (int)Math.round(potImage.getHeight() * viewScale);
        g2.setColor(Color.BLACK);
        g2.fillRect(areaX, areaY, areaW, areaH);

        // draw pieces scaled
        for (PuzzlePiece p : pieces) {
            int px = logicalToScreenX(p.x);
            int py = logicalToScreenY(p.y);
            int pw = (int)Math.round(p.image.getWidth()  * viewScale);
            int ph = (int)Math.round(p.image.getHeight() * viewScale);
            g2.drawImage(p.image, px, py, pw, ph, this);
        }
        g2.dispose();
    }

    // --- mouse ---
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = screenToLogicalX(e.getX());
        int my = screenToLogicalY(e.getY());
        for (int i = pieces.size() - 1; i >= 0; i--) {
            PuzzlePiece p = pieces.get(i);
            int pw = p.image.getWidth(), ph = p.image.getHeight();
            if (mx >= p.x && mx <= p.x + pw && my >= p.y && my <= p.y + ph) {
                selectedPiece = p;
                offsetX = mx - p.x;
                offsetY = my - p.y;
                pieces.remove(i);
                pieces.add(p);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
                break;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedPiece != null) {
            int mx = screenToLogicalX(e.getX());
            int my = screenToLogicalY(e.getY());
            selectedPiece.x = mx - offsetX;
            selectedPiece.y = my - offsetY;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (selectedPiece != null) {
            // snap to the piece’s correct logical position (NO extra offset)
            int snapX = selectedPiece.correctX;
            int snapY = selectedPiece.correctY;

            // keep the same on-screen snap feel regardless of scale
            int logicalThreshold = (int)Math.round(snapThreshold / Math.max(0.0001, viewScale));

            if (Math.abs(selectedPiece.x - snapX) < logicalThreshold &&
                    Math.abs(selectedPiece.y - snapY) < logicalThreshold) {
                selectedPiece.x = snapX;
                selectedPiece.y = snapY;
                selectedPiece.placed = true;

                boolean solved = true;
                for (PuzzlePiece p : pieces) {
                    if (!p.placed) { solved = false; break; }
                }
                if (solved) {
                    JOptionPane.showMessageDialog(this, "Congratulations! You fixed the pot!");
                    if (solvedListener != null) solvedListener.puzzleSolved();
                }
            }
            selectedPiece = null;
            repaint();
        }
        setCursor(Cursor.getDefaultCursor());
    }

    // unused
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    // --- piece model ---
    private static final class PuzzlePiece {
        final BufferedImage image;
        final int correctX, correctY;  // logical correct top-left
        int x, y;                      // logical current top-left
        boolean placed = false;
        PuzzlePiece(BufferedImage img, int cx, int cy) { image = img; correctX = cx; correctY = cy; }
    }
}
