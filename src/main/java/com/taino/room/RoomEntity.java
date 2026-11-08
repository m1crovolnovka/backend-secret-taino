package com.taino.room;

import com.taino.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 32)
    private String theme = "newyear";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoomPhase phase = RoomPhase.REGISTRATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_mode", nullable = false, length = 32)
    private AdminMode adminMode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "registration_ends_at", nullable = false)
    private Instant registrationEndsAt;

    @Column(name = "delivery_at", nullable = false)
    private Instant deliveryAt;

    @Column(name = "delivery_place", length = 500)
    private String deliveryPlace;

    @Column(name = "budget_min", nullable = false)
    private int budgetMin;

    @Column(name = "budget_max", nullable = false)
    private int budgetMax;

    @Column(nullable = false, length = 8)
    private String currency = "BYN";

    @Column(name = "draw_locked", nullable = false)
    private boolean drawLocked;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public RoomPhase getPhase() {
        return phase;
    }

    public void setPhase(RoomPhase phase) {
        this.phase = phase;
    }

    public AdminMode getAdminMode() {
        return adminMode;
    }

    public void setAdminMode(AdminMode adminMode) {
        this.adminMode = adminMode;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public Instant getRegistrationEndsAt() {
        return registrationEndsAt;
    }

    public void setRegistrationEndsAt(Instant registrationEndsAt) {
        this.registrationEndsAt = registrationEndsAt;
    }

    public Instant getDeliveryAt() {
        return deliveryAt;
    }

    public void setDeliveryAt(Instant deliveryAt) {
        this.deliveryAt = deliveryAt;
    }

    public String getDeliveryPlace() {
        return deliveryPlace;
    }

    public void setDeliveryPlace(String deliveryPlace) {
        this.deliveryPlace = deliveryPlace;
    }

    public int getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(int budgetMin) {
        this.budgetMin = budgetMin;
    }

    public int getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(int budgetMax) {
        this.budgetMax = budgetMax;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDrawLocked() {
        return drawLocked;
    }

    public void setDrawLocked(boolean drawLocked) {
        this.drawLocked = drawLocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
