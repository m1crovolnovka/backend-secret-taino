package com.taino.form;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormRepository extends JpaRepository<FormEntity, Long> {
    Optional<FormEntity> findByMemberId(Long memberId);
}
