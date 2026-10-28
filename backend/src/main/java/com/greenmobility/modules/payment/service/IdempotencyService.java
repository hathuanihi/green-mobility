package com.greenmobility.modules.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.modules.payment.domain.IdempotencyKeyRecord;
import com.greenmobility.modules.payment.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String computeHash(Object requestBody) {
        if (requestBody == null) {
            return "EMPTY";
        }
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> ResponseEntity<T> executeWithIdempotency(
            String idempotencyKey,
            UUID userId,
            String endpoint,
            Object requestBody,
            Class<T> responseType,
            Supplier<ResponseEntity<T>> execution
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // No idempotency key provided, proceed directly
            return execution.get();
        }

        String requestHash = computeHash(requestBody);

        Optional<IdempotencyKeyRecord> existing = repository.findByUserIdAndEndpointAndKey(userId, endpoint, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyKeyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                log.warn("Idempotency key reused with different payload: user={}, endpoint={}, key={}",
                        userId, endpoint, idempotencyKey);
                throw new PaymentBusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã được sử dụng với nội dung yêu cầu khác");
            }

            try {
                T body = objectMapper.readValue(record.getResponseBody(), responseType);
                return ResponseEntity.status(record.getResponseStatus()).body(body);
            } catch (Exception e) {
                log.error("Failed to deserialize cached idempotency response: {}", e.getMessage());
            }
        }

        ResponseEntity<T> response = execution.get();

        try {
            String responseJson = objectMapper.writeValueAsString(response.getBody());
            IdempotencyKeyRecord newRecord = new IdempotencyKeyRecord(
                    userId,
                    endpoint,
                    idempotencyKey,
                    requestHash,
                    response.getStatusCode().value(),
                    responseJson
            );
            repository.save(newRecord);
        } catch (Exception e) {
            log.error("Failed to store idempotency record: {}", e.getMessage(), e);
        }

        return response;
    }
}

