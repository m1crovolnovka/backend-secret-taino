package com.taino.user;

import com.taino.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    /** Telegram @username — AES-GCM encrypted at rest via {@link EncryptedStringConverter}. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "first_name", nullable = false, length = 512)
    private String firstName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "last_name", length = 512)
    private String lastName;

    @Column(name = "language_code", length = 16)
    private String languageCode;

    /** When the bot first wrote to this user (creates chat history after startapp). */
    @Column(name = "bot_greeted_at")
    private Instant botGreetedAt;

    /** First tracking link that brought this user (utm / campaign attribution). */
    @Column(name = "attribution_link_id")
    private Long attributionLinkId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Instant getBotGreetedAt() {
        return botGreetedAt;
    }

    public void setBotGreetedAt(Instant botGreetedAt) {
        this.botGreetedAt = botGreetedAt;
    }

    public Long getAttributionLinkId() {
        return attributionLinkId;
    }

    public void setAttributionLinkId(Long attributionLinkId) {
        this.attributionLinkId = attributionLinkId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
