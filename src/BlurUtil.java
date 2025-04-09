import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

// Utility clas to apply a simple box blur to an image (the background image)
public class BlurUtil {
    /**
     * Blurs the given image using a simple box blur with the specified radius.
     * A larger radius produces a stronger blur.
     */
    public static BufferedImage blurImage(BufferedImage image, int radius) {
        int size = radius * 2 + 1;
        float[] matrix = new float[size * size];
        float weight = 1.0f / (size * size);
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = weight;
        }
        Kernel kernel = new Kernel(size, size, matrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null); // Apply the blur and return the new image
    }
}