package com.aisdlc.urlshortener.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEventEntity, Long> {

    List<ClickEventEntity> findByShortLinkIdOrderByOccurredAtDesc(Long shortLinkId);

    long countByShortLinkId(Long shortLinkId);
}
