package com.taino.image;

import com.taino.common.ApiException;
import com.taino.room.RoomAccessService;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageUploadService {

    public static final int MAX_IMAGES_PER_USER = 40;

    private final ImageProcessor processor;
    private final ImageStorage storage;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final RoomAccessService access;

    public ImageUploadService(
            ImageProcessor processor,
            ImageStorage storage,
            ImageRepository imageRepository,
            UserRepository userRepository,
            RoomAccessService access
    ) {
        this.processor = processor;
        this.storage = storage;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional
    public ImageResponse uploadForRoom(Long userId, Long roomId, MultipartFile file) {
        access.requireActiveMember(roomId, userId);
        if (imageRepository.countByOwner_Id(userId) >= MAX_IMAGES_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_LIMIT", "Лимит загрузок исчерпан");
        }
        UserEntity owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Пользователь не найден"));

        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_READ", "Не удалось прочитать файл");
        }

        ImageProcessor.ProcessedImage processed = processor.process(raw, file.getContentType());
        StoredImage stored = storage.upload(processed.content(), processed.mimeType(), UUID.randomUUID().toString());

        ImageEntity entity = new ImageEntity();
        entity.setOwner(owner);
        entity.setStorageKey(stored.storageKey());
        entity.setPublicUrl(stored.publicUrl());
        entity.setDisplayUrl(stored.displayUrl());
        entity.setDeleteUrl(stored.deleteUrl());
        entity.setThumbUrl(stored.thumbUrl());
        entity.setMimeType(processed.mimeType());
        entity.setWidth(processed.width() > 0 ? processed.width() : stored.width());
        entity.setHeight(processed.height() > 0 ? processed.height() : stored.height());
        entity.setByteSize(processed.content().length);
        entity = imageRepository.save(entity);

        return new ImageResponse(entity.getId(), entity.getDisplayUrl(), entity.getThumbUrl(), entity.getWidth(), entity.getHeight());
    }

    public record ImageResponse(UUID id, String url, String thumbUrl, Integer width, Integer height) {
    }
}
