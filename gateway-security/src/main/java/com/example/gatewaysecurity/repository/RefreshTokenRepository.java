package com.example.gatewaysecurity.repository;

import com.example.gatewaysecurity.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Query("""
            select t from RefreshToken t inner join User u  on  t.user.id = u.id
            where u.id = :userId and  (t.expired = false  or t.revoked = false )
            """)
    List<RefreshToken> findAllTokensByUser(Long userId);

    Optional<RefreshToken> findByToken(String token);
}
