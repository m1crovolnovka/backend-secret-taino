package com.taino.image;

import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates MIME by content, strips EXIF by re-encoding, and downscales large images.
 */
@Component
public class ImageProcessor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_EDGE = 1600;

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    public ProcessedImage process(byte[] raw, String declaredContentType) {
        if (raw == null || raw.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_EMPTY", "Пустой файл");
        }
        if (raw.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_TOO_LARGE", "Максимум 5 МБ");
        }

        String sniff = sniffMime(raw);
        if (sniff == null || !ALLOWED.contains(sniff)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_TYPE", "Разрешены JPEG, PNG, WebP");
        }
        if (declaredContentType != null && !declaredContentType.isBlank()) {
            String normalized = declaredContentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!ALLOWED.contains(normalized) && !"application/octet-stream".equals(normalized)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_TYPE", "Разрешены JPEG, PNG, WebP");
            }
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_CORRUPT", "Не удалось прочитать изображение");
        }
        if (source == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_CORRUPT", "Не удалось прочитать изображение");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_CORRUPT", "Некорректный размер изображения");
        }

        double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(width, height));
        int targetW = Math.max(1, (int) Math.round(width * scale));
        int targetH = Math.max(1, (int) Math.round(height * scale));

        BufferedImage rgb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }

        // Re-encode as JPEG → EXIF/metadata stripped; anonymous UUID name used at upload.
        byte[] out;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(rgb, "jpg", baos)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_ENCODE", "Не удалось перекодировать изображение");
            }
            out = baos.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_ENCODE", "Не удалось перекодировать изображение");
        }

        return new ProcessedImage(out, "image/jpeg", targetW, targetH);
    }

    static String sniffMime(byte[] raw) {
        if (raw.length >= 3 && (raw[0] & 0xFF) == 0xFF && (raw[1] & 0xFF) == 0xD8 && (raw[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (raw.length >= 8
                && raw[0] == (byte) 0x89 && raw[1] == 0x50 && raw[2] == 0x4E && raw[3] == 0x47
                && raw[4] == 0x0D && raw[5] == 0x0A && raw[6] == 0x1A && raw[7] == 0x0A) {
            return "image/png";
        }
        if (raw.length >= 12
                && raw[0] == 'R' && raw[1] == 'I' && raw[2] == 'F' && raw[3] == 'F'
                && raw[8] == 'W' && raw[9] == 'E' && raw[10] == 'B' && raw[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    public record ProcessedImage(byte[] content, String mimeType, int width, int height) {
    }
}
