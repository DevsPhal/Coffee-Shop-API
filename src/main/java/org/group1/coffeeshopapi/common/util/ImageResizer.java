package org.group1.coffeeshopapi.common.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Downscales an image to fit a bounding box and re-encodes it as JPEG. Never upscales, and
// falls back to the original bytes if the format can't be decoded (e.g. WEBP, animated GIF).
public final class ImageResizer {

    private ImageResizer() {
    }

    public static byte[] fitToJpeg(byte[] original, int maxWidth, int maxHeight) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            return original;
        }
        if (source == null) {
            return original;
        }

        double scale = Math.min(1.0,
                Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight()));
        int targetWidth = Math.max(1, Math.round((float) (source.getWidth() * scale)));
        int targetHeight = Math.max(1, Math.round((float) (source.getHeight() * scale)));

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetWidth, targetHeight);
        g.drawImage(source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            return original;
        }
    }
}
