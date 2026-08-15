package com.tennantari.gallery.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.tennantari.gallery.model.Media;
import com.tennantari.gallery.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "gallery.drive.enabled", havingValue = "true")
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(DriveScopes.DRIVE_FILE);

    @Value("${gallery.drive.enabled:false}")
    private boolean driveEnabled;

    @Value("${gallery.drive.credentials-path:credentials.json}")
    private String credentialsPath;

    @Value("${gallery.drive.token-path:token.json}")
    private String tokenPath;

    @Value("${gallery.drive.root-folder-name:Tennant-Ari Gallery}")
    private String rootFolderName;

    private Drive driveService;
    private String rootFolderId;
    private final MediaRepository mediaRepository;

    public GoogleDriveService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @PostConstruct
    public void init() {
        if (!driveEnabled) {
            log.info("Google Drive backup is disabled");
            return;
        }
        try {
            driveService = getDriveService();
            rootFolderId = getOrCreateFolder(rootFolderName, null);
            log.info("Google Drive backup enabled. Root folder: {} (ID: {})", rootFolderName, rootFolderId);
        } catch (Exception e) {
            log.error("Failed to initialize Google Drive: {}", e.getMessage());
            driveEnabled = false;
        }
    }

    public boolean isEnabled() {
        return driveEnabled && driveService != null;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public String getRootFolderLink() {
        if (!isEnabled() || rootFolderId == null) return null;
        return "https://drive.google.com/drive/folders/" + rootFolderId;
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        InputStream credsStream = new FileInputStream(credentialsPath);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(credsStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(Paths.get(tokenPath).toFile()))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("Tennant-Ari Gallery")
                .build();
    }

    public String getOrCreateFolder(String folderName, String parentFolderId) throws IOException {
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName.replace("'", "\\'") + "' and trashed=false";
        if (parentFolderId != null) {
            query += " and '" + parentFolderId + "' in parents";
        } else {
            query += " and 'root' in parents";
        }

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        if (parentFolderId != null) {
            folderMetadata.setParents(List.of(parentFolderId));
        }

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    @Async
    public void uploadFileAsync(String localFilePath, String fileName, String contentType, String driveFolderId, Long mediaId) {
        if (!isEnabled() || driveFolderId == null) return;

        try {
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(List.of(driveFolderId));

            FileContent mediaContent = new FileContent(contentType, new java.io.File(localFilePath));

            File uploaded = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            log.info("Backed up {} to Drive (ID: {})", fileName, uploaded.getId());

            mediaRepository.findById(mediaId).ifPresent(media -> {
                media.setDriveFileId(uploaded.getId());
                mediaRepository.save(media);
            });
        } catch (IOException e) {
            log.error("Failed to back up {} to Drive: {}", fileName, e.getMessage());
        }
    }

    @Async
    public void deleteFileAsync(String driveFileId) {
        if (!isEnabled() || driveFileId == null) return;

        try {
            driveService.files().delete(driveFileId).execute();
            log.info("Deleted file from Drive (ID: {})", driveFileId);
        } catch (IOException e) {
            log.error("Failed to delete file from Drive (ID: {}): {}", driveFileId, e.getMessage());
        }
    }

    public String uploadFile(String localFilePath, String fileName, String contentType, String driveFolderId) throws IOException {
        if (!isEnabled() || driveFolderId == null) return null;

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(List.of(driveFolderId));

        FileContent mediaContent = new FileContent(contentType, new java.io.File(localFilePath));

        File uploaded = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        return uploaded.getId();
    }
}
