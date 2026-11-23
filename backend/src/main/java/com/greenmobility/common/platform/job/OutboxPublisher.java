package com.greenmobility.common.platform.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.platform.domain.OutboxEvent;
import com.greenmobility.common.platform.repository.OutboxEventRepository;
import com.greenmobility.config.RabbitMQConfig;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final long CONFIRM_TIMEOUT_SECONDS = 5L;

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "OutboxPublisher_publish", lockAtLeastFor = "500ms", lockAtMostFor = "10s")
    @Transactional
    public void publishPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> pending = outboxRepository.findPendingEventsForPublishing(now, 50);

        for (OutboxEvent event : pending) {
            try {
                Object payloadObj = objectMapper.readValue(event.getPayload(), Map.class);
                CorrelationData correlationData = new CorrelationData(event.getId().toString());
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        event.getRoutingKey(),
                        payloadObj,
                        correlationData
                );

                // Wait for real broker publisher confirm before marking SENT (R8)
                CorrelationData.Confirm confirm = correlationData.getFuture()
                        .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (confirm == null || !confirm.isAck()) {
                    String reason = confirm != null ? confirm.getReason() : "null confirm";
                    throw new IllegalStateException("Broker NACK for outbox event " + event.getId() + ": " + reason);
                }
                if (correlationData.getReturned() != null) {
                    throw new IllegalStateException("Message returned unroutable for routingKey=" + event.getRoutingKey()
                            + ", replyText=" + correlationData.getReturned().getReplyText());
                }

                event.setStatus("SENT");
                event.setSentAt(Instant.now());
                outboxRepository.save(event);
                log.debug("Confirmed & marked SENT outbox event {} -> {}", event.getId(), event.getRoutingKey());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                int nextAttempts = event.getAttempts() + 1;
                event.setAttempts(nextAttempts);

                if (nextAttempts >= 10) {
                    event.setStatus("FAILED");
                    log.error("Outbox event {} marked as FAILED after 10 attempts (stopped auto-polling; requires admin redrive)",
                            event.getId());
                } else {
                    long backoffSec = Math.min(300L, (long) Math.pow(2, nextAttempts));
                    event.setNextAttemptAt(Instant.now().plusSeconds(backoffSec));
                }
                outboxRepository.save(event);
            }
        }
    }
}
