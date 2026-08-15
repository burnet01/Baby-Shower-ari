package com.tennantari.gallery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String thumbnailFilename;

    private String driveFileId;

    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean compressed;

    public boolean isCompressed() { return compressed; }
    public void setCompressed(boolean compressed) { this.compressed = compressed; }

    public enum MediaType {
        IMAGE, VIDEO
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Media() {}

    public Media(String originalFilename, String storedFilename, String contentType, long fileSize, MediaType mediaType) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.mediaType = mediaType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFileUrl() {
        return "/uploads/" + storedFilename;
    }

    public String getThumbnailUrl() {
        if (thumbnailFilename != null) {
            return "/uploads/thumbs/" + thumbnailFilename;
        }
        return getFileUrl();
    }

    public boolean hasThumbnail() {
        return thumbnailFilename != null;
    }

    public String getThumbnailFilename() { return thumbnailFilename; }
    public void setThumbnailFilename(String thumbnailFilename) { this.thumbnailFilename = thumbnailFilename; }

    public String getDriveFileId() { return driveFileId; }
    public void setDriveFileId(String driveFileId) { this.driveFileId = driveFileId; }

    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}
