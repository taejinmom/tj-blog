package com.taejin.authservice.repository;

import com.taejin.authservice.entity.RefreshToken;
import com.taejin.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.token = :token")
    int revokeByToken(@Param("token") String token);
}
