package com.trendly.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path uploadDir;
    private final String urlPrefix;

    public MediaStorageService(
            @Value("${app.media.upload-dir}") String uploadDir,
            @Value("${app.media.url-prefix}") String urlPrefix
    ) {
        this.uploadDir = Path.of(uploadDir);
        this.urlPrefix = urlPrefix;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory", e);
        }
    }

    /** Stores the file on disk and returns its public URL (e.g. "/media/abc123.jpg"). */
    public String store(MultipartFile file) {
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed");
        }

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
        String filename = UUID.randomUUID() + extension;

        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded file", e);
        }

        return urlPrefix + "/" + filename;
    }

    /** Deletes a previously stored file given its public URL. Safe to call on already-missing files. */
    public void delete(String url) {
        if (url == null || !url.startsWith(urlPrefix + "/")) return;
        String filename = url.substring((urlPrefix + "/").length());
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            // best-effort cleanup; not worth failing the request over
        }
    }
}
