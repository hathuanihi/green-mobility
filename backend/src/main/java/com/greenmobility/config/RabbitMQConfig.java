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
    public static final String DLX_EXCHANGE_NAME = "dlx.greenmobility";

    public static final String QUEUE_TRIP_MATCHING_REQUESTS = "q.trip.matching.requests";
    public static final String ROUTING_KEY_TRIP_REQUESTED = "trip.event.requested";

    public static final String QUEUE_TRIP_MATCHED = "q.trip.status.matched";
    public static final String ROUTING_KEY_TRIP_MATCHED = "trip.event.matched";

    public static final String QUEUE_TRIP_CANCELLED = "q.trip.status.cancelled";
    public static final String ROUTING_KEY_TRIP_CANCELLED = "trip.event.cancelled";

    public static final String ROUTING_KEY_TRIP_DISPATCHED = "trip.event.dispatched";

    // Sprint 3 & 4: Trip completed -> Carbon queue + DLQ
    public static final String QUEUE_TRIP_COMPLETED = "q.trip.status.completed";
    public static final String QUEUE_TRIP_COMPLETED_DLQ = "q.trip.status.completed.dlq";
    public static final String ROUTING_KEY_TRIP_COMPLETED = "trip.event.completed";
    public static final String ROUTING_KEY_TRIP_COMPLETED_DLQ = "dlq.trip.status.completed";

    // Sprint 5: Payment Consumer Queue & DLQ
    public static final String QUEUE_TRIP_COMPLETED_PAYMENT = "q.trip.completed.payment";
    public static final String QUEUE_TRIP_COMPLETED_PAYMENT_DLQ = "q.trip.completed.payment.dlq";
    public static final String ROUTING_KEY_PAYMENT_DLQ = "dlq.trip.completed.payment";

    public static final String QUEUE_PAYMENT_EVENTS = "q.payment.events";
    public static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.event.succeeded";
    public static final String ROUTING_KEY_PAYMENT_FAILED = "payment.event.failed";
    public static final String ROUTING_KEY_PAYMENT_REFUNDED = "payment.event.refunded";
    public static final String ROUTING_KEY_WALLET_TOPUP = "wallet.event.topup.succeeded";
    public static final String ROUTING_KEY_WITHDRAWAL_UPDATED = "withdrawal.event.updated";

    // Sprint 6: Incentive Consumer Queues & DLQs + Output Events for Sprint 7
    public static final String QUEUE_CARBON_CALCULATED = "q.carbon.calculated";
    public static final String QUEUE_CARBON_CALCULATED_DLQ = "q.carbon.calculated.dlq";
    public static final String ROUTING_KEY_CARBON_CALCULATED = "carbon.event.calculated";
    public static final String ROUTING_KEY_CARBON_CALCULATED_DLQ = "dlq.carbon.calculated";

    public static final String QUEUE_PAYMENT_SUCCEEDED_INCENTIVE = "q.payment.succeeded.incentive";
    public static final String QUEUE_PAYMENT_SUCCEEDED_INCENTIVE_DLQ = "q.payment.succeeded.incentive.dlq";
    public static final String ROUTING_KEY_PAYMENT_SUCCEEDED_INCENTIVE_DLQ = "dlq.payment.succeeded.incentive";

    public static final String QUEUE_PAYMENT_REFUNDED_INCENTIVE = "q.payment.refunded.incentive";
    public static final String QUEUE_PAYMENT_REFUNDED_INCENTIVE_DLQ = "q.payment.refunded.incentive.dlq";
    public static final String ROUTING_KEY_PAYMENT_REFUNDED_INCENTIVE_DLQ = "dlq.payment.refunded.incentive";

    public static final String QUEUE_INCENTIVE_EVENTS = "q.incentive.events";
    public static final String ROUTING_KEY_INCENTIVE_CREDITED = "incentive.event.credited";
    public static final String ROUTING_KEY_INCENTIVE_REDEEMED = "incentive.event.redeemed";

    @Bean
    public TopicExchange greenMobilityExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE_NAME, true, false);
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

    // Carbon TripCompleted queue + DLQ
    @Bean
    public Queue tripCompletedDlq() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED_DLQ).build();
    }

    @Bean
    public Binding tripCompletedDlqBinding(Queue tripCompletedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(tripCompletedDlq).to(deadLetterExchange).with(ROUTING_KEY_TRIP_COMPLETED_DLQ);
    }

    @Bean
    public Queue tripCompletedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_TRIP_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Binding tripCompletedBinding(Queue tripCompletedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(tripCompletedQueue).to(greenMobilityExchange).with(ROUTING_KEY_TRIP_COMPLETED);
    }

    // Payment TripCompleted queue + DLQ
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

    // Payment events queue
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

    // Sprint 6: Carbon calculated queue + DLQ
    @Bean
    public Queue carbonCalculatedDlq() {
        return QueueBuilder.durable(QUEUE_CARBON_CALCULATED_DLQ).build();
    }

    @Bean
    public Binding carbonCalculatedDlqBinding(Queue carbonCalculatedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(carbonCalculatedDlq).to(deadLetterExchange).with(ROUTING_KEY_CARBON_CALCULATED_DLQ);
    }

    @Bean
    public Queue carbonCalculatedQueue() {
        return QueueBuilder.durable(QUEUE_CARBON_CALCULATED)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_CARBON_CALCULATED_DLQ)
                .build();
    }

    @Bean
    public Binding carbonCalculatedBinding(Queue carbonCalculatedQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(carbonCalculatedQueue).to(greenMobilityExchange).with(ROUTING_KEY_CARBON_CALCULATED);
    }

    // Sprint 6: Payment succeeded queue for Incentive + DLQ
    @Bean
    public Queue paymentSucceededIncentiveDlq() {
        return QueueBuilder.durable(QUEUE_PAYMENT_SUCCEEDED_INCENTIVE_DLQ).build();
    }

    @Bean
    public Binding paymentSucceededIncentiveDlqBinding(Queue paymentSucceededIncentiveDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentSucceededIncentiveDlq).to(deadLetterExchange).with(ROUTING_KEY_PAYMENT_SUCCEEDED_INCENTIVE_DLQ);
    }

    @Bean
    public Queue paymentSucceededIncentiveQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_SUCCEEDED_INCENTIVE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_PAYMENT_SUCCEEDED_INCENTIVE_DLQ)
                .build();
    }

    @Bean
    public Binding paymentSucceededIncentiveBinding(Queue paymentSucceededIncentiveQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentSucceededIncentiveQueue).to(greenMobilityExchange).with(ROUTING_KEY_PAYMENT_SUCCEEDED);
    }

    // Sprint 6: Payment refunded queue for Incentive + DLQ
    @Bean
    public Queue paymentRefundedIncentiveDlq() {
        return QueueBuilder.durable(QUEUE_PAYMENT_REFUNDED_INCENTIVE_DLQ).build();
    }

    @Bean
    public Binding paymentRefundedIncentiveDlqBinding(Queue paymentRefundedIncentiveDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentRefundedIncentiveDlq).to(deadLetterExchange).with(ROUTING_KEY_PAYMENT_REFUNDED_INCENTIVE_DLQ);
    }

    @Bean
    public Queue paymentRefundedIncentiveQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_REFUNDED_INCENTIVE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_PAYMENT_REFUNDED_INCENTIVE_DLQ)
                .build();
    }

    @Bean
    public Binding paymentRefundedIncentiveBinding(Queue paymentRefundedIncentiveQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(paymentRefundedIncentiveQueue).to(greenMobilityExchange).with(ROUTING_KEY_PAYMENT_REFUNDED);
    }

    // Sprint 6 -> Sprint 7 Incentive output events queue
    @Bean
    public Queue incentiveEventsQueue() {
        return QueueBuilder.durable(QUEUE_INCENTIVE_EVENTS).build();
    }

    @Bean
    public Binding incentiveCreditedBinding(Queue incentiveEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(incentiveEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_INCENTIVE_CREDITED);
    }

    @Bean
    public Binding incentiveRedeemedBinding(Queue incentiveEventsQueue, TopicExchange greenMobilityExchange) {
        return BindingBuilder.bind(incentiveEventsQueue).to(greenMobilityExchange).with(ROUTING_KEY_INCENTIVE_REDEEMED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper typeMapper =
                new org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
