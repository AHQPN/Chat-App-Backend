package org.example.chatapp.repository;

import org.example.chatapp.entity.User;
import org.example.chatapp.service.enums.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User,Integer> {
    Boolean existsByPhoneNumber(String phoneNumber);
    Boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    
    Page<User> findByUserIdNot(Integer userId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.userId != :userId AND u.userType != :userType")
    Page<User> findByUserIdNotAndUserTypeNot(@Param("userId") Integer userId, @Param("userType") RoleEnum userType, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.userType != :userType")
    Page<User> findByUserTypeNot(@Param("userType") RoleEnum userType, Pageable pageable);

    Optional<User> findByPhoneNumberOrEmail(String phoneNumber, String email);

    @Query("SELECT u FROM User u WHERE u.userType != :userType AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByFullNameContainingIgnoreCaseAndUserTypeNot(@Param("name") String name, @Param("userType") RoleEnum userType);

    List<User> findByFullNameContainingIgnoreCase(String name);
}
