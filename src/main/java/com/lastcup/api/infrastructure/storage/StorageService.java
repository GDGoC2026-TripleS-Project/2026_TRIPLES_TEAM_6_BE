package com.lastcup.api.infrastructure.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

    private final S3Client s3Client;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public UploadResult uploadProfileImage(Long userId, MultipartFile file) {
        validateFile(file);
        String directory = "users/" + userId + "/profile";
        return s3Client.upload(directory, file);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        if (!isImage(file.getContentType())) {
            throw new IllegalArgumentException("file is not image");
        }
    }

    private boolean isImage(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }
}
