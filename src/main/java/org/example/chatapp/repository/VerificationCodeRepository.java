package org.example.chatapp.repository;

import org.example.chatapp.entity.User;
import org.example.chatapp.entity.VerificationCode;
import org.example.chatapp.service.enums.VerificationCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    void deleteByUserAndType(User user, VerificationCodeType type);
    Optional<VerificationCode> findByVerificationCode(String code);

    Optional<VerificationCode> findByUserAndType(User user, VerificationCodeType type);
}