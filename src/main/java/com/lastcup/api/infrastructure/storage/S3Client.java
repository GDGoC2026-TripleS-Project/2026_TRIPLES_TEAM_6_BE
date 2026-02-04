package com.lastcup.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
public class S3Client {

    private static final Logger log = LoggerFactory.getLogger(S3Client.class);

    private final software.amazon.awssdk.services.s3.S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3Client(
            @Value("${aws.region:ap-northeast-2}") String region,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.client = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(Region.of(region))
                .build();
        this.bucket = bucket;
        this.publicBaseUrl = buildBaseUrl(region, bucket, publicBaseUrl);
    }

    public UploadResult upload(String directory, MultipartFile file) {
        validateBucket();
        String key = createKey(directory, file.getOriginalFilename());
        putObject(key, file);
        return new UploadResult(key, publicBaseUrl + "/" + key, fileSize(file));
    }

    private String createKey(String directory, String originalFilename) {
        String safeDir = normalizeDir(directory);
        String ext = extractExtension(originalFilename);
        return safeDir + "/" + UUID.randomUUID() + ext;
    }

    private String normalizeDir(String directory) {
        if (directory == null || directory.isBlank()) {
            return "uploads";
        }
        if (directory.endsWith("/")) {
            return directory.substring(0, directory.length() - 1);
        }
        return directory;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return originalFilename.substring(idx);
    }

    private void putObject(String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            log.error("S3 upload failed: bucket={}, key={}, size={}, contentType={}",
                    bucket,
                    key,
                    fileSize(file),
                    file != null ? file.getContentType() : null,
                    e);
            throw new IllegalStateException("s3 upload failed", e);
        }
    }

    private long fileSize(MultipartFile file) {
        if (file == null) {
            return 0;
        }
        return file.getSize();
    }

    private void validateBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("s3 upload failed");
        }
    }

    private String buildBaseUrl(String region, String bucket, String configuredBaseUrl) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return trimSlash(configuredBaseUrl);
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }

    private String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
