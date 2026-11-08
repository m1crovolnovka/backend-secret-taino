package com.taino.image;

import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Validates MIME by content, strips EXIF by re-encoding, and downscales large images.
 */
@Component
public class ImageProcessor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_EDGE = 1280;
    public static final float JPEG_QUALITY = 0.72f;

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

        // Client already compresses to JPEG ≤ MAX_EDGE — skip ImageIO re-encode (biggest server latency).
        if ("image/jpeg".equals(sniff) && raw.length <= 900_000) {
            Dimension dim = jpegDimensions(raw);
            if (dim != null && dim.w > 0 && dim.h > 0 && Math.max(dim.w, dim.h) <= MAX_EDGE) {
                return new ProcessedImage(raw, "image/jpeg", dim.w, dim.h);
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

        byte[] out = encodeJpeg(rgb);
        return new ProcessedImage(out, "image/jpeg", targetW, targetH);
    }

    /** Read SOF0/SOF2 dimensions without full decode. */
    private static Dimension jpegDimensions(byte[] raw) {
        int i = 2;
        while (i + 9 < raw.length) {
            if ((raw[i] & 0xFF) != 0xFF) return null;
            int marker = raw[i + 1] & 0xFF;
            int len = ((raw[i + 2] & 0xFF) << 8) | (raw[i + 3] & 0xFF);
            if (len < 2) return null;
            if (marker == 0xC0 || marker == 0xC2) {
                int h = ((raw[i + 5] & 0xFF) << 8) | (raw[i + 6] & 0xFF);
                int w = ((raw[i + 7] & 0xFF) << 8) | (raw[i + 8] & 0xFF);
                return new Dimension(w, h);
            }
            i += 2 + len;
        }
        return null;
    }

    private record Dimension(int w, int h) {}

    private static byte[] encodeJpeg(BufferedImage rgb) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_ENCODE", "Не удалось перекодировать изображение");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(rgb, null, null), param);
            writer.dispose();
            return baos.toByteArray();
        } catch (IOException ex) {
            writer.dispose();
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_ENCODE", "Не удалось перекодировать изображение");
        }
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
