package com.taino.security;

import com.taino.config.TainoProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption for PII fields (Telegram usernames) at rest.
 * Values are stored as {@code ENC1:<base64(iv+ciphertext)>}. Plaintext legacy rows pass through until rewritten on next login.
 */
@Component
@Lazy(false)
public class FieldCrypto {

    private static final String PREFIX = "ENC1:";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final TainoProperties properties;
    private SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldCrypto(TainoProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String material = properties.getPiiSecret();
        if (!StringUtils.hasText(material)) {
            material = properties.getJwt().getSecret();
        }
        if (!StringUtils.hasText(material) || material.length() < 16) {
            throw new IllegalStateException("Set TAINO_PII_SECRET or JWT_SECRET (min 16 chars) for field encryption");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            key = new SecretKeySpec(digest, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot init field crypto", ex);
        }
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        if (plain.startsWith(PREFIX)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buf.put(iv);
            buf.put(cipherBytes);
            return PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Encrypt failed", ex);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored; // legacy plaintext
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LEN];
            byte[] cipherBytes = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Decrypt failed", ex);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
