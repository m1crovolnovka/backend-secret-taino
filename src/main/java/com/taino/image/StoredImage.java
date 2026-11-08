package com.taino.image;

public record StoredImage(
        String storageKey,
        String publicUrl,
        String displayUrl,
        String deleteUrl,
        String thumbUrl,
        String mimeType,
        int width,
        int height,
        int byteSize
) {
}
