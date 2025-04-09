import java.awt.image.BufferedImage;

// Represents a single puzzle piece in the Jigsaw game
public class PuzzlePiece {
    public BufferedImage image; // image segment corresponding to this puzzle piece
    public int x, y; // current position for the puzzle piece
    public int correctX, correctY; // correct position for the puzzle piece
    public boolean placed = false; // flag for whether it is in the correct place or not

    // Constructor to create a puzzle piece with its image and the correct position
    public PuzzlePiece(BufferedImage image, int correctX, int correctY) {
        this.image = image;
        this.correctX = correctX;
        this.correctY = correctY;
    }
}