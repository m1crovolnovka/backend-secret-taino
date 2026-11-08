package com.taino.image;

/**
 * Pluggable image storage. Swap provider without touching call sites.
 */
public interface ImageStorage {

    StoredImage upload(byte[] content, String contentType, String logicalName);

    /**
     * Best-effort delete. Providers that do not support delete may no-op.
     */
    void delete(StoredImage image);
}
