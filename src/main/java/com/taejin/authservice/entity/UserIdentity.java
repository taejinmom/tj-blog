package com.taejin.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 외부 OAuth2 제공자의 계정과 내부 {@link User}를 연결하는 엔티티.
 *
 * 한 User는 여러 제공자(Google, GitHub 등)의 identity를 가질 수 있다.
 * (provider, providerUserId) 조합이 유일하다.
 */
@Entity
@Table(
        name = "user_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_identity_provider_subject",
                        columnNames = {"provider", "provider_user_id"})
        },
        indexes = {
                @Index(name = "idx_identity_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 제공자 식별자 (예: "google", "github"). */
    @Column(nullable = false, length = 32)
    private String provider;

    /** 제공자 측 사용자 고유 ID (Google sub, GitHub id). */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 제공자가 제공한 이메일(참고용). User.email과 다를 수 있음. */
    @Column(length = 255)
    private String emailAtProvider;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
