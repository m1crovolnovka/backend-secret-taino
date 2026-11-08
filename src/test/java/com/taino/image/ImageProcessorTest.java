package com.taino.image;

import com.taino.common.ApiException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    @Test
    void reencodesJpegAndStripsToRgb() throws Exception {
        byte[] png = samplePng(40, 30);
        ImageProcessor.ProcessedImage out = processor.process(png, "image/png");

        assertEquals("image/jpeg", out.mimeType());
        assertEquals(40, out.width());
        assertEquals(30, out.height());
        assertTrue(out.content().length > 0);
        assertEquals("image/jpeg", ImageProcessor.sniffMime(out.content()));
    }

    @Test
    void rejectsNonImage() {
        ApiException ex = assertThrows(ApiException.class,
                () -> processor.process("not-an-image".getBytes(), "text/plain"));
        assertEquals("IMAGE_TYPE", ex.getCode());
    }

    @Test
    void downscalesLargeImage() throws Exception {
        byte[] png = samplePng(2000, 1000);
        ImageProcessor.ProcessedImage out = processor.process(png, "image/png");
        assertTrue(out.width() <= ImageProcessor.MAX_EDGE);
        assertTrue(out.height() <= ImageProcessor.MAX_EDGE);
    }

    private static byte[] samplePng(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                img.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
