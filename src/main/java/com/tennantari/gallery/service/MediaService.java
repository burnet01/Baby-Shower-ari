package com.tennantari.gallery.service;

import com.tennantari.gallery.model.Collection;
import com.tennantari.gallery.model.Media;
import com.tennantari.gallery.repository.CollectionRepository;
import com.tennantari.gallery.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class MediaService {

    private static final int PAGE_SIZE = 40;
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;
    private final CollectionRepository collectionRepository;
    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;
    private final VideoCompressionService videoCompressionService;
    private final Optional<GoogleDriveService> googleDriveService;

    public MediaService(MediaRepository mediaRepository, CollectionRepository collectionRepository,
                        FileStorageService fileStorageService,
                        ThumbnailService thumbnailService,
                        VideoCompressionService videoCompressionService,
                        Optional<GoogleDriveService> googleDriveService) {
        this.mediaRepository = mediaRepository;
        this.collectionRepository = collectionRepository;
        this.fileStorageService = fileStorageService;
        this.thumbnailService = thumbnailService;
        this.videoCompressionService = videoCompressionService;
        this.googleDriveService = googleDriveService;
    }

    public Media uploadFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (!FileStorageService.isAllowed(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        String storedFilename = fileStorageService.storeFile(file);

        Media.MediaType mediaType = FileStorageService.isImage(contentType)
            ? Media.MediaType.IMAGE
            : Media.MediaType.VIDEO;

        Media media = new Media(
            file.getOriginalFilename(),
            storedFilename,
            contentType,
            file.getSize(),
            mediaType
        );

        media = mediaRepository.save(media);

        if (FileStorageService.isImage(contentType) && !"image/gif".equals(contentType)) {
            generateThumbnailAsync(media.getId(), storedFilename, contentType);
        }

        if (FileStorageService.isVideo(contentType)) {
            compressVideoAsync(media.getId(), storedFilename);
        }

        return media;
    }

    @Async
    public void compressVideoAsync(Long mediaId, String storedFilename) {
        try {
            videoCompressionService.compressVideo(storedFilename, mediaId);
        } catch (Exception e) {
            log.warn("Video compression failed for {}: {}", storedFilename, e.getMessage());
        }
    }

    @Async
    public void generateThumbnailAsync(Long mediaId, String storedFilename, String contentType) {
        try {
            Path storedPath = Paths.get("uploads", storedFilename);
            String thumbFilename = thumbnailService.generateThumbnail(
                storedFilename, Files.newInputStream(storedPath), contentType);
            if (thumbFilename != null) {
                mediaRepository.findById(mediaId).ifPresent(media -> {
                    media.setThumbnailFilename(thumbFilename);
                    mediaRepository.save(media);
                });
            }
        } catch (IOException e) {
            log.warn("Failed to generate thumbnail for {}: {}", storedFilename, e.getMessage());
        }
    }

    public List<Media> getAllMedia() {
        return mediaRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<Media> getMediaPage(String filter, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        if ("images".equals(filter)) {
            return mediaRepository.findByMediaTypeOrderByCreatedAtDesc(Media.MediaType.IMAGE, pageable);
        } else if ("videos".equals(filter)) {
            return mediaRepository.findByMediaTypeOrderByCreatedAtDesc(Media.MediaType.VIDEO, pageable);
        }
        return mediaRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<Media> getMediaByType(Media.MediaType mediaType) {
        return mediaRepository.findByMediaTypeOrderByCreatedAtDesc(mediaType);
    }

    public long getTotalCount() {
        return mediaRepository.count();
    }

    public List<Media> getUncategorizedMedia() {
        return mediaRepository.findUncategorizedMedia();
    }

    public Media getMediaById(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));
    }

    @Transactional
    public void deleteMedia(Long id) {
        Media media = getMediaById(id);
        for (Collection col : collectionRepository.findAll()) {
            if (col.getMediaItems().remove(media)) {
                collectionRepository.save(col);
            }
        }
        fileStorageService.deleteFile(media.getStoredFilename());
        if (media.getThumbnailFilename() != null) {
            fileStorageService.deleteFile("thumbs/" + media.getThumbnailFilename());
        }
        if (media.getDriveFileId() != null) {
            googleDriveService.ifPresent(drive -> drive.deleteFileAsync(media.getDriveFileId()));
        }
        mediaRepository.delete(media);
    }
}
