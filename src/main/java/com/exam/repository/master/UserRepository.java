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

    /**
     * Find the admin user for a specific tenant by tenant DB ID.
     * Replaces the previous findAll().stream().filter() which loaded ALL users into memory.
     */
    Optional<AppUser> findFirstByTenant_Id(UUID tenantId);
}
