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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
