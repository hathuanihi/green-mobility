package com.greenmobility.modules.payment.repository;

import com.greenmobility.modules.payment.domain.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    List<PaymentWebhookEvent> findByGatewayAndGatewayTxnRefOrderByReceivedAtDesc(String gateway, String gatewayTxnRef);

    boolean existsByGatewayAndGatewayTxnRef(String gateway, String gatewayTxnRef);
}

