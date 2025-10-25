package org.example.chatapp.repository;

import org.example.chatapp.entity.RefreshToken;
import org.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Integer> {
    Optional<RefreshToken> findByRefreshToken(String token);

    @Modifying
    int deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteAllByUser(User user);
}
