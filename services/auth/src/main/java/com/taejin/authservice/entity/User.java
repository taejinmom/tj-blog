package com.taejin.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 인증 주체. 로컬 로그인(email + passwordHash)과 소셜 로그인을 모두 지원한다.
 *
 * 설계 노트:
 * - passwordHash는 nullable: OAuth 전용 계정은 비밀번호가 없을 수 있음 (oauth-dev가 확장).
 * - roles는 별도 Role 엔티티와 ManyToMany로 연결하여 RBAC(Task #3)에서 확장.
 * - OAuth 연동은 별도의 UserIdentity(provider, providerUserId) 엔티티로 분리할 것 → oauth-dev 담당.
 *   이 User 엔티티 자체는 변경하지 않고, UserIdentity가 User를 FK로 참조하는 형태로 확장 가능.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt 해시. OAuth 전용 계정은 null 허용. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(length = 100)
    private String displayName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
