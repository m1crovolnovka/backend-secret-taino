package com.taino.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.common.ApiException;
import com.taino.config.TainoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
public class ImgbbImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImgbbImageStorage.class);

    private final RestClient restClient;
    private final TainoProperties properties;
    private final ObjectMapper objectMapper;

    public ImgbbImageStorage(RestClient.Builder restClientBuilder,
                             TainoProperties properties,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        String baseUrl = properties.getImgbb().getBaseUrl();
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public StoredImage upload(byte[] content, String contentType, String logicalName) {
        String apiKey = properties.getImgbb().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMGBB_NOT_CONFIGURED", "Хранилище картинок не настроено");
        }

        String safeName = StringUtils.hasText(logicalName)
                ? logicalName.replaceAll("[^a-zA-Z0-9._-]", "_")
                : UUID.randomUUID().toString();
        // Never forward original user filenames — UUID prefix for anonymity.
        String uploadName = UUID.randomUUID() + "-" + safeName;
        if (!uploadName.toLowerCase().endsWith(".jpg") && !uploadName.toLowerCase().endsWith(".jpeg")) {
            uploadName = uploadName + ".jpg";
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        String filename = uploadName;
        body.part("image", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));
        body.part("name", uploadName);

        String json;
        try {
            json = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/1/upload")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.warn("ImgBB upload failed: {}", ex.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IMGBB_UPLOAD_FAILED", "Не удалось загрузить изображение");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.path("success").asBoolean(false)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "IMGBB_UPLOAD_FAILED", "ImgBB отклонил загрузку");
            }
            JsonNode data = root.path("data");
            String id = data.path("id").asText(null);
            String url = data.path("url").asText(null);
            String displayUrl = data.path("display_url").asText(url);
            if (!StringUtils.hasText(id) || !StringUtils.hasText(url)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "IMGBB_UPLOAD_FAILED", "Некорректный ответ ImgBB");
            }
            String deleteUrl = textOrNull(data, "delete_url");
            String thumbUrl = data.path("thumb").path("url").asText(null);
            return new StoredImage(
                    id,
                    url,
                    displayUrl,
                    deleteUrl,
                    thumbUrl,
                    contentType != null ? contentType : "image/jpeg",
                    data.path("width").asInt(0),
                    data.path("height").asInt(0),
                    content.length
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "IMGBB_UPLOAD_FAILED", "Некорректный ответ ImgBB");
        }
    }

    @Override
    public void delete(StoredImage image) {
        if (image == null || !StringUtils.hasText(image.deleteUrl())) {
            return;
        }
        try {
            // delete_url is absolute (ibb.co) — use a client without ImgBB baseUrl
            RestClient.create().get()
                    .uri(image.deleteUrl())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.debug("ImgBB delete skipped/failed for {}", image.storageKey());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }
}
