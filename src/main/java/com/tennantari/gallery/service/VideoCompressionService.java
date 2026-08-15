package com.tennantari.gallery.service;

import com.tennantari.gallery.model.Media;
import com.tennantari.gallery.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class VideoCompressionService {

    private static final Logger log = LoggerFactory.getLogger(VideoCompressionService.class);

    @Value("${gallery.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    @Value("${gallery.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private final MediaRepository mediaRepository;

    public VideoCompressionService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    private static final long MIN_COMPRESS_SIZE = 50L * 1024 * 1024;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String compressVideo(String storedFilename, Long mediaId) {
        if (!mediaRepository.existsById(mediaId)) {
            log.info("Media {} deleted before compression started, skipping", mediaId);
            return null;
        }

        Path inputPath = uploadPath.resolve(storedFilename);
        if (!Files.exists(inputPath)) {
            log.warn("Video file not found for compression: {}", inputPath);
            return null;
        }

        long originalSize;
        try {
            originalSize = Files.size(inputPath);
        } catch (IOException e) {
            log.warn("Could not read file size for {}: {}", storedFilename, e.getMessage());
            return null;
        }

        if (originalSize < MIN_COMPRESS_SIZE) {
            log.info("Skipped compression for {} — file too small ({})", storedFilename, formatSize(originalSize));
            return null;
        }

        String name = storedFilename;
        int dot = storedFilename.lastIndexOf('.');
        if (dot > 0) {
            name = storedFilename.substring(0, dot);
        }

        String tempFilename = name + "-compressed.mp4";
        Path tempPath = uploadPath.resolve(tempFilename);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputPath.toString(),
                "-c:v", "libx264",
                "-crf", "23",
                "-preset", "veryfast",
                "-threads", "2",
                "-c:a", "aac",
                "-b:a", "128k",
                "-y",
                tempPath.toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (!mediaRepository.existsById(mediaId)) {
                log.info("Media {} was deleted during compression, discarding result", mediaId);
                Files.deleteIfExists(tempPath);
                return null;
            }

            if (exitCode != 0) {
                log.warn("FFmpeg compression failed with exit code {} for {}:\n{}", exitCode, storedFilename, output);
                Files.deleteIfExists(tempPath);
                mediaRepository.findById(mediaId).ifPresent(media -> {
                    media.setCompressed(true);
                    mediaRepository.save(media);
                });
                return null;
            }

            long compressedSize = Files.size(tempPath);

            if (compressedSize < originalSize) {
                String mp4Filename = name + ".mp4";
                Path mp4Path = uploadPath.resolve(mp4Filename);
                Files.move(tempPath, mp4Path, StandardCopyOption.REPLACE_EXISTING);
                if (!inputPath.equals(mp4Path)) {
                    Files.deleteIfExists(inputPath);
                }
                mediaRepository.findById(mediaId).ifPresent(media -> {
                    media.setStoredFilename(mp4Filename);
                    media.setFileSize(compressedSize);
                    media.setCompressed(true);
                    mediaRepository.save(media);
                });
                log.info("Compressed {} from {} to {} (saved {}%)", storedFilename,
                    formatSize(originalSize), formatSize(compressedSize),
                    (originalSize - compressedSize) * 100 / originalSize);
                return mp4Filename;
            } else {
                Files.deleteIfExists(tempPath);
                mediaRepository.findById(mediaId).ifPresent(media -> {
                    media.setCompressed(true);
                    mediaRepository.save(media);
                });
                log.info("Skipped compression for {} — compressed file was larger", storedFilename);
            }

            return null;
        } catch (IOException | InterruptedException e) {
            log.warn("Video compression failed for {}: {}", storedFilename, e.getMessage());
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            mediaRepository.findById(mediaId).ifPresent(media -> {
                media.setCompressed(true);
                mediaRepository.save(media);
            });
            return null;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
