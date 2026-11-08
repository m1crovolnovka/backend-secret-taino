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
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final RoomAccessService access;

    public ImageUploadService(
            ImageProcessor processor,
            ImageRepository imageRepository,
            UserRepository userRepository,
            RoomAccessService access
    ) {
        this.processor = processor;
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
        UUID id = UUID.randomUUID();
        String path = "/api/images/" + id;

        ImageEntity entity = new ImageEntity();
        entity.setId(id);
        entity.setOwner(owner);
        entity.setStorageKey(id.toString());
        entity.setPublicUrl(path);
        entity.setDisplayUrl(path);
        entity.setThumbUrl(path);
        entity.setDeleteUrl(null);
        entity.setMimeType(processed.mimeType());
        entity.setWidth(processed.width() > 0 ? processed.width() : null);
        entity.setHeight(processed.height() > 0 ? processed.height() : null);
        entity.setByteSize(processed.content().length);
        entity.setContent(processed.content());
        entity = imageRepository.save(entity);

        return new ImageResponse(entity.getId(), entity.getDisplayUrl(), entity.getThumbUrl(), entity.getWidth(), entity.getHeight());
    }

    @Transactional(readOnly = true)
    public ImageEntity requireWithContent(UUID id) {
        ImageEntity entity = imageRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "Фото не найдено"));
        if (entity.getContent() == null || entity.getContent().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "IMAGE_EMPTY", "Фото недоступно");
        }
        return entity;
    }

    public record ImageResponse(UUID id, String url, String thumbUrl, Integer width, Integer height) {
    }
}
