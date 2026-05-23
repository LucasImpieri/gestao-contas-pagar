package com.impieri.gestaocontaspagar.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CSV_IMPORT_QUEUE = "csv.import";

    @Bean
    public Queue csvImportQueue() {
        return new Queue(CSV_IMPORT_QUEUE, true);
    }

    @Bean
    public SimpleMessageConverter messageConverter(ConnectionFactory connectionFactory) {
        SimpleMessageConverter converter = new SimpleMessageConverter();

        converter.setAllowedListPatterns(
                java.util.List.of(
                        "com.impieri.gestaocontaspagar.*",
                        "java.util.*",
                        "java.lang.*"
                )
        );

        return converter;
    }
}