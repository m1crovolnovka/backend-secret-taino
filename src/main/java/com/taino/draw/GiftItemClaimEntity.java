package com.taino.draw;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "gift_item_claims")
public class GiftItemClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personal_item_id", nullable = false)
    private Long personalItemId;

    @Column(name = "room_item_id", nullable = false)
    private Long roomItemId;

    @Column(name = "claimer_user_id", nullable = false)
    private Long claimerUserId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @PrePersist
    void onCreate() {
        if (claimedAt == null) {
            claimedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getPersonalItemId() {
        return personalItemId;
    }

    public void setPersonalItemId(Long personalItemId) {
        this.personalItemId = personalItemId;
    }

    public Long getRoomItemId() {
        return roomItemId;
    }

    public void setRoomItemId(Long roomItemId) {
        this.roomItemId = roomItemId;
    }

    public Long getClaimerUserId() {
        return claimerUserId;
    }

    public void setClaimerUserId(Long claimerUserId) {
        this.claimerUserId = claimerUserId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }
}
