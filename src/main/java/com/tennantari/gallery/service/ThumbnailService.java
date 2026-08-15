package com.tennantari.gallery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ThumbnailService {

    @Value("${gallery.upload-dir}")
    private String uploadDir;

    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 300;
    private static final String THUMB_DIR = "thumbs";
    private static final String THUMB_FORMAT = "jpg";
    private static final int THUMB_QUALITY = 85;

    public String generateThumbnail(String storedFilename, InputStream inputStream, String contentType) {
        if (!contentType.startsWith("image/") || "image/gif".equals(contentType)) {
            return null;
        }

        try {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) return null;

            int origWidth = originalImage.getWidth();
            int origHeight = originalImage.getHeight();

            int targetWidth = MAX_WIDTH;
            int targetHeight = MAX_HEIGHT;

            double ratio = Math.min((double) MAX_WIDTH / origWidth, (double) MAX_HEIGHT / origHeight);
            if (ratio >= 1.0) {
                targetWidth = origWidth;
                targetHeight = origHeight;
            } else {
                targetWidth = (int) (origWidth * ratio);
                targetHeight = (int) (origHeight * ratio);
            }

            BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            Path thumbDir = Paths.get(uploadDir, THUMB_DIR);
            if (!Files.exists(thumbDir)) {
                Files.createDirectories(thumbDir);
            }

            String thumbFilename = storedFilename.substring(0, storedFilename.lastIndexOf('.')) + "-thumb." + THUMB_FORMAT;
            Path thumbPath = thumbDir.resolve(thumbFilename);
            ImageIO.write(thumbnail, THUMB_FORMAT, thumbPath.toFile());

            return thumbFilename;
        } catch (IOException e) {
            return null;
        }
    }

    public Path getThumbnailPath(String thumbFilename) {
        return Paths.get(uploadDir, THUMB_DIR, thumbFilename);
    }
}
