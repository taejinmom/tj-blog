package com.taejin.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * RBAC 역할. Task #3에서 권한(Permission)과의 매핑까지 확장 예정.
 * 현재는 이름 기반(ROLE_USER, ROLE_ADMIN 등)으로만 사용.
 */
@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_roles_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예: ROLE_USER, ROLE_ADMIN */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public static Role of(String name) {
        return Role.builder().name(name).build();
    }
}
