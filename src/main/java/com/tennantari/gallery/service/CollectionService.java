package com.tennantari.gallery.service;

import com.tennantari.gallery.model.Collection;
import com.tennantari.gallery.model.Media;
import com.tennantari.gallery.repository.CollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final CollectionRepository collectionRepository;
    private final Optional<GoogleDriveService> googleDriveService;

    public CollectionService(CollectionRepository collectionRepository, Optional<GoogleDriveService> googleDriveService) {
        this.collectionRepository = collectionRepository;
        this.googleDriveService = googleDriveService;
    }

    public Collection createCollection(String name, String description, String ownerEmail) {
        Collection collection = new Collection(name, description, ownerEmail);
        return collectionRepository.save(collection);
    }

    public List<Collection> getCollectionsForUser(String ownerEmail) {
        return collectionRepository.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail);
    }

    public List<Collection> getAllCollections() {
        return collectionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Collection getCollectionById(Long id) {
        return collectionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Collection not found with id: " + id));
    }

    @Transactional
    public void addMediaToCollection(Long collectionId, Media media) {
        Collection collection = getCollectionById(collectionId);
        collection.addMedia(media);
        collectionRepository.save(collection);

        googleDriveService.ifPresent(drive -> {
            if (collection.isBackupEnabled() && drive.isEnabled() && collection.getDriveFolderId() != null) {
                drive.uploadFileAsync(
                    "uploads/" + media.getStoredFilename(),
                    media.getOriginalFilename(),
                    media.getContentType(),
                    collection.getDriveFolderId(),
                    media.getId()
                );
            }
        });
    }

    @Transactional
    public void removeMediaFromCollection(Long collectionId, Media media) {
        Collection collection = getCollectionById(collectionId);
        collection.removeMedia(media);
        collectionRepository.save(collection);
    }

    public void deleteCollection(Long id) {
        collectionRepository.deleteById(id);
    }

    @Transactional
    public Collection updateCollectionBackup(Long id, boolean backupEnabled, String driveFolderName) {
        Collection collection = getCollectionById(id);
        collection.setBackupEnabled(backupEnabled);

        googleDriveService.ifPresent(drive -> {
            if (backupEnabled && drive.isEnabled()) {
                try {
                    String folderId = drive.getOrCreateFolder(driveFolderName, drive.getRootFolderId());
                    collection.setDriveFolderId(folderId);
                } catch (Exception e) {
                    log.error("Failed to create Drive folder for collection {}: {}", collection.getName(), e.getMessage());
                }
            }
        });

        return collectionRepository.save(collection);
    }
}
