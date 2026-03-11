package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_firebase_uid", columnList = "uid"),
        @Index(name = "idx_users_location", columnList = "current_lat, current_lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;  // Firebase UID (PROJECT.md §6.4: firebase_uid; kept 'uid' for compatibility)

    private String phoneNumber;
    private String displayName;
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    private String photoUrl;
    private String fcmToken;

    @Column(name = "current_lat")
    private Double latitude;

    @Column(name = "current_lng")
    private Double longitude;

    private String address;
    private String subLocality;
    private String locality;
    private String administrativeArea;
    private String postalCode;
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    @Builder.Default
    private UserType userType = UserType.CLIENT;

    @Column(name = "last_location_update")
    private Instant lastLocationUpdate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum UserType {
        CLIENT,
        AGENT,
        BOTH
    }
}
