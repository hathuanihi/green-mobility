package com.greenmobility.common.platform.repository;

import com.greenmobility.common.platform.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM outbox_events e " +
            "WHERE e.status = 'NEW' AND e.next_attempt_at <= :now " +
            "ORDER BY e.created_at ASC " +
            "LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findPendingEventsForPublishing(@Param("now") Instant now, @Param("limit") int limit);

    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
