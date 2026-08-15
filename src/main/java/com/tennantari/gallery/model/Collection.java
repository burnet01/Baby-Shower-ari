package com.tennantari.gallery.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "collection_media",
        joinColumns = @JoinColumn(name = "collection_id"),
        inverseJoinColumns = @JoinColumn(name = "media_id")
    )
    private Set<Media> mediaItems = new HashSet<>();

    @Column(nullable = false)
    private String ownerEmail;

    private boolean backupEnabled;

    private String driveFolderId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Collection() {}

    public Collection(String name, String description, String ownerEmail) {
        this.name = name;
        this.description = description;
        this.ownerEmail = ownerEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<Media> getMediaItems() { return mediaItems; }
    public void setMediaItems(Set<Media> mediaItems) { this.mediaItems = mediaItems; }

    public void addMedia(Media media) {
        this.mediaItems.add(media);
    }

    public void removeMedia(Media media) {
        this.mediaItems.remove(media);
    }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public boolean isBackupEnabled() { return backupEnabled; }
    public void setBackupEnabled(boolean backupEnabled) { this.backupEnabled = backupEnabled; }

    public String getDriveFolderId() { return driveFolderId; }
    public void setDriveFolderId(String driveFolderId) { this.driveFolderId = driveFolderId; }

    public int getMediaCount() { return mediaItems.size(); }
}
