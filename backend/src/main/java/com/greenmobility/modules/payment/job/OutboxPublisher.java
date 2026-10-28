package com.greenmobility.modules.payment.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.config.RabbitMQConfig;
import com.greenmobility.modules.payment.domain.OutboxEvent;
import com.greenmobility.modules.payment.repository.OutboxEventRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

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
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, event.getRoutingKey(), payloadObj);

                event.setStatus("SENT");
                event.setSentAt(Instant.now());
                outboxRepository.save(event);
                log.debug("Successfully published outbox event {} to {}", event.getId(), event.getRoutingKey());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                int nextAttempts = event.getAttempts() + 1;
                event.setAttempts(nextAttempts);

                if (nextAttempts >= 10) {
                    event.setStatus("FAILED");
                    log.error("Outbox event {} marked as FAILED after 10 attempts", event.getId());
                } else {
                    long backoffSec = (long) Math.pow(2, nextAttempts);
                    event.setNextAttemptAt(Instant.now().plusSeconds(backoffSec));
                }
                outboxRepository.save(event);
            }
        }
    }
}

