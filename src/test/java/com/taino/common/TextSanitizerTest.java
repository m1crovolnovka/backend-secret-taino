package com.taino.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextSanitizerTest {

    @Test
    void stripsTags() {
        assertEquals("hello", TextSanitizer.plain("<b>hello</b>", 100));
    }

    @Test
    void acceptsHttps() {
        assertEquals("https://example.com/a", TextSanitizer.httpUrl("https://example.com/a"));
    }

    @Test
    void prefixesHttps() {
        assertEquals("https://wb.ru/item", TextSanitizer.httpUrl("wb.ru/item"));
    }

    @Test
    void rejectsJavascriptUrl() {
        assertThrows(ApiException.class, () -> TextSanitizer.httpUrl("javascript:alert(1)"));
    }
}
