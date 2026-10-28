package com.greenmobility.modules.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.modules.payment.domain.OutboxEvent;
import com.greenmobility.modules.payment.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateType, String aggregateId, String routingKey, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, routingKey, payloadJson);
            outboxEventRepository.save(event);
            log.info("Recorded outbox event: aggregate={}:{}, routingKey={}", aggregateType, aggregateId, routingKey);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to record outbox event", e);
        }
    }
}

