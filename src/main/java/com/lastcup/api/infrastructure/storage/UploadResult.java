package com.lastcup.api.infrastructure.storage;

public record UploadResult(
        String key,
        String url,
        long size
) {
}
