package com.tennantari.gallery.controller;

import com.tennantari.gallery.model.Collection;
import com.tennantari.gallery.model.Media;
import com.tennantari.gallery.service.CollectionService;
import com.tennantari.gallery.service.GoogleDriveService;
import com.tennantari.gallery.service.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class GalleryController {

    private final MediaService mediaService;
    private final CollectionService collectionService;
    private final Optional<GoogleDriveService> googleDriveService;

    public GalleryController(MediaService mediaService, CollectionService collectionService,
                             Optional<GoogleDriveService> googleDriveService) {
        this.mediaService = mediaService;
        this.collectionService = collectionService;
        this.googleDriveService = googleDriveService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String filter,
                        @RequestParam(required = false) Long collectionId,
                        @AuthenticationPrincipal UserDetails principal,
                        Model model) {

        boolean isLoggedIn = (principal != null && principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        model.addAttribute("currentFilter", filter);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("driveEnabled", googleDriveService.map(GoogleDriveService::isEnabled).orElse(false));
        model.addAttribute("driveFolderLink", googleDriveService.map(GoogleDriveService::getRootFolderLink).orElse(null));

        List<Collection> collections = collectionService.getAllCollections();
        model.addAttribute("collections", collections);

        // Viewing a specific collection
        if (collectionId != null) {
            Collection collection = collectionService.getCollectionById(collectionId);
            List<Media> mediaInCollection = collection.getMediaItems().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
            model.addAttribute("mediaList", mediaInCollection);
            model.addAttribute("currentCollection", collection);
            model.addAttribute("totalCount", mediaInCollection.size());
            model.addAttribute("hasMore", false);
            model.addAttribute("currentPage", 0);
            model.addAttribute("hasAnyContent", !mediaInCollection.isEmpty());
            model.addAttribute("collectionSections", Collections.emptyList());
            model.addAttribute("uncategorizedMedia", Collections.emptyList());
            return "gallery";
        }

        // Home view: group by collections + uncategorized
        List<Collection> allCollections = collectionService.getAllCollections();
        List<Map<String, Object>> sections = new ArrayList<>();

        for (Collection col : allCollections) {
            List<Media> mediaInCol = col.getMediaItems().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
            if (!mediaInCol.isEmpty()) {
                Map<String, Object> section = new HashMap<>();
                section.put("name", col.getName());
                section.put("id", col.getId());
                section.put("media", mediaInCol);
                sections.add(section);
            }
        }

        List<Media> uncategorized = mediaService.getUncategorizedMedia();
        if (!uncategorized.isEmpty()) {
            Map<String, Object> section = new HashMap<>();
            section.put("name", "Uncategorized");
            section.put("id", null);
            section.put("media", uncategorized);
            sections.add(section);
        }

        int totalItems = (int) mediaService.getTotalCount();
        model.addAttribute("collectionSections", sections);
        model.addAttribute("uncategorizedMedia", uncategorized);
        model.addAttribute("mediaList", Collections.emptyList());
        model.addAttribute("totalCount", totalItems);
        model.addAttribute("hasMore", false);
        model.addAttribute("currentPage", 0);
        model.addAttribute("hasAnyContent", totalItems > 0);

        return "gallery";
    }

    @GetMapping("/api/media/page")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMediaPage(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page) {

        var mediaPage = mediaService.getMediaPage(filter, page);
        Map<String, Object> response = new HashMap<>();
        response.put("content", mediaPage.getContent());
        response.put("hasMore", mediaPage.hasNext());
        response.put("page", page);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) Long collectionId) {

        List<Map<String, Object>> mediaList = new ArrayList<>();
        int failCount = 0;

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    Media media = mediaService.uploadFile(file);
                    if (collectionId != null) {
                        collectionService.addMediaToCollection(collectionId, media);
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", media.getId());
                    item.put("originalFilename", media.getOriginalFilename());
                    item.put("storedFilename", media.getStoredFilename());
                    item.put("fileSize", media.getFileSize());
                    item.put("fileSizeFormatted", media.getFileSizeFormatted());
                    item.put("mediaType", media.getMediaType().name());
                    item.put("compressed", media.isCompressed());
                    item.put("fileUrl", media.getFileUrl());
                    item.put("thumbnailUrl", media.getThumbnailUrl());
                    item.put("thumbnailFilename", media.getThumbnailFilename());
                    mediaList.add(item);
                } catch (IllegalArgumentException | IOException e) {
                    failCount++;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("media", mediaList);
        response.put("successCount", mediaList.size());
        response.put("failCount", failCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/media/{id}")
    @ResponseBody
    public ResponseEntity<Media> getMedia(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @DeleteMapping("/media/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Media deleted successfully");
        return ResponseEntity.ok(response);
    }

    // --- Collections (admin only, enforced by SecurityConfig) ---

    @PostMapping("/media/batch-delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> batchDeleteMedia(@RequestBody List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            try {
                mediaService.deleteMedia(id);
                deleted++;
            } catch (Exception e) {
                // skip failed
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("deleted", deleted);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/collection/{id}")
    public void downloadCollection(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Collection collection = collectionService.getCollectionById(id);
        String zipName = collection.getName().replaceAll("[^a-zA-Z0-9_.-]", "_") + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Media media : collection.getMediaItems()) {
                java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", media.getStoredFilename());
                if (java.nio.file.Files.exists(filePath)) {
                    zos.putNextEntry(new ZipEntry(media.getOriginalFilename()));
                    java.nio.file.Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    @GetMapping("/download/selected")
    public void downloadSelected(@RequestParam String ids, HttpServletResponse response) throws IOException {
        List<Long> idList = new ArrayList<>();
        for (String s : ids.split(",")) {
            try { idList.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) {}
        }
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"selected.zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Long id : idList) {
                try {
                    Media media = mediaService.getMediaById(id);
                    java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", media.getStoredFilename());
                    if (java.nio.file.Files.exists(filePath)) {
                        zos.putNextEntry(new ZipEntry(media.getOriginalFilename()));
                        java.nio.file.Files.copy(filePath, zos);
                        zos.closeEntry();
                    }
                } catch (Exception e) {
                    // skip missing
                }
            }
        }
    }

    @PostMapping("/collections")
    @ResponseBody
    public ResponseEntity<Collection> createCollection(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        Collection collection = collectionService.createCollection(name, description, "admin");
        return ResponseEntity.ok(collection);
    }

    @PostMapping("/collections/{id}/add-media")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addMediaToCollection(
            @PathVariable Long id,
            @RequestParam Long mediaId) {
        Media media = mediaService.getMediaById(mediaId);
        collectionService.addMediaToCollection(id, media);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/collections/{id}/remove-media")
    @ResponseBody
    public ResponseEntity<Map<String, String>> removeMediaFromCollection(
            @PathVariable Long id,
            @RequestParam Long mediaId) {
        Media media = mediaService.getMediaById(mediaId);
        collectionService.removeMediaFromCollection(id, media);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/collections/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/collections/{id}/backup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCollectionBackup(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            @RequestParam(required = false) String folderName) {
        Collection collection = collectionService.updateCollectionBackup(id, enabled,
            folderName != null ? folderName : collectionService.getCollectionById(id).getName());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("backupEnabled", collection.isBackupEnabled());
        response.put("driveFolderId", collection.getDriveFolderId());
        return ResponseEntity.ok(response);
    }
}
