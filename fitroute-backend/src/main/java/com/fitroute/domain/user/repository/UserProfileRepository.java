// domain/user/repository/UserProfileRepository.java
package com.fitroute.domain.user.repository;

import com.fitroute.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}