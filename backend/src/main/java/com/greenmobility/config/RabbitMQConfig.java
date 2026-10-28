package com.greenmobility.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "greenmobility.topic.exchange";

    public static final String QUEUE_TRIP_MATCHING_REQUESTS = "q.trip.matching.requests";
    public static final String ROUTING_KEY_TRIP_REQUESTED = "trip.event.requested";

    public static final String QUEUE_TRIP_MATCHED = "q.trip.status.matched";
    public static final String ROUTING_KEY_TRIP_MATCHED = "trip.event.matched";

    public static final String QUEUE_TRIP_CANCELLED = "q.trip.status.cancelled";
    public static final String ROUTING_KEY_TRIP_CANCELLED = "trip.event.cancelled";

    public static final String ROUTING_KEY_TRIP_DISPATCHED = "trip.event.dispatched";

    // Sprint 3: Trip execution lifecycle events
    public static final String QUEUE_TRIP_COMPLETED = "q.trip.status.completed";
    public static final String ROUTING_KEY_TRIP_COMPLETED = "trip.event.completed";

    @Bean
    public TopicExchange greenMobilityExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue tripMatchingQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_MATCHING_REQUESTS).build();
    }

    @Bean
    public Binding tripMatchingBinding(Queue tripMatchingQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(tripMatchingQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_REQUESTED);
    }

    @Bean
    public Queue tripMatchedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_MATCHED).build();
    }

    @Bean
    public Binding tripMatchedBinding(Queue tripMatchedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(tripMatchedQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_MATCHED);
    }

    @Bean
    public Queue tripCancelledQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_CANCELLED).build();
    }

    @Bean
    public Binding tripCancelledBinding(Queue tripCancelledQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(tripCancelledQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_CANCELLED);
    }

    // Sprint 3: Trip completed queue (consumed by Sprint 4 Carbon Engine & Sprint 5 Payment)
    @Bean
    public Queue tripCompletedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED).build();
    }

    @Bean
    public Binding tripCompletedBinding(Queue tripCompletedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(tripCompletedQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_COMPLETED);
    }

    // Sprint 4: Carbon calculated queue (consumed by Sprint 6 Double-entry Ledger & Incentive)
    public static final String QUEUE_CARBON_CALCULATED = "q.carbon.calculated";
    public static final String ROUTING_KEY_CARBON_CALCULATED = "carbon.event.calculated";

    // Sprint 5: Payment Consumer Queue & DLQ (Exclusive queue for Payment to prevent competing consumers)
    public static final String QUEUE_TRIP_COMPLETED_PAYMENT = "q.trip.completed.payment";
    public static final String QUEUE_TRIP_COMPLETED_PAYMENT_DLQ = "q.trip.completed.payment.dlq";
    public static final String DLX_EXCHANGE_NAME = "dlx.greenmobility";
    public static final String ROUTING_KEY_PAYMENT_DLQ = "dlq.trip.completed.payment";

    public static final String QUEUE_PAYMENT_EVENTS = "q.payment.events";
    public static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.event.succeeded";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.event.failed";
    public static final String ROUTING_KEY_PAYMENT_REFUNDED = "payment.event.refunded";
    public static final String ROUTING_KEY_WALLET_TOPUP = "wallet.event.topup.succeeded";
    public static final String ROUTING_KEY_WITHDRAWAL_UPDATED = "withdrawal.event.updated";

    @Bean
    public Queue carbonCalculatedQueue() {
        return QueueBuilder.durable(QUEUE_CARBON_CALCULATED).build();
    }

    @Bean
    public Binding carbonCalculatedBinding(Queue carbonCalculatedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(carbonCalculatedQueue).to(greenMobilityExchange).with(ROUTING_KEY_CARBON_CALCULATED);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue paymentTripCompletedDlq() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED_PAYMENT_DLQ).build();
    }

    @Bean
    public Binding paymentTripCompletedDlqBinding(Queue paymentTripCompletedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentTripCompletedDlq).to(deadLetterExchange).with(ROUTING_KEY_PAYMENT_DLQ);
    }

    @Bean
    public Queue paymentTripCompletedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED_PAYMENT)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Binding paymentTripCompletedBinding(Queue paymentTripCompletedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentTripCompletedQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_COMPLETED);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_EVENTS).build();
    }

    @Bean
    public Binding paymentSucceededBinding(Queue paymentEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_PAYMENT_SUCCEEDED);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_PAYMENT_FAILED);
    }

    @Bean
    public Binding paymentRefundedBinding(Queue paymentEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_PAYMENT_REFUNDED);
    }

    @Bean
    public Binding walletTopupBinding(Queue paymentEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_WALLET_TOPUP);
    }

    @Bean
    public Binding withdrawalUpdatedBinding(Queue paymentEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_WITHDRAWAL_UPDATED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
