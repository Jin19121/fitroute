// domain/user/repository/UserRepository.java
package com.fitroute.domain.user.repository;

import com.fitroute.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEncryptedEmail(String encryptedEmail); // findByEmail → findByEncryptedEmail

    boolean existsByEncryptedEmail(String encryptedEmail); // existsByEmail → existsByEncryptedEmail
}