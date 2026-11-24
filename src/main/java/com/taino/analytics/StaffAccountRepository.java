package com.taino.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffAccountRepository extends JpaRepository<StaffAccountEntity, Long> {
    Optional<StaffAccountEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);
}
