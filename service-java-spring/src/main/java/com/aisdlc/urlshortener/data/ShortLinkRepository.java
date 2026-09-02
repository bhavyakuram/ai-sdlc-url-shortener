package com.aisdlc.urlshortener.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLinkEntity, Long> {

    Optional<ShortLinkEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}
