package com.exam.repository.master;

import com.exam.entity.master.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tenant"})
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByResetToken(String resetToken);
}
