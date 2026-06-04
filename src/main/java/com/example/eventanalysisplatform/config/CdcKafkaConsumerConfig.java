package com.example.eventanalysisplatform.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
public class CdcKafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> cdcConsumerFactory(){
        return new DefaultKafkaConsumerFactory<>(Map.of(
                "bootstrap.servers", "localhost:9092",
                "key.deserializer", StringDeserializer.class,
                "value.deserializer", StringDeserializer.class,
                "auto.offset.reset", "earliest"
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> cdcKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(cdcConsumerFactory());

        return factory;
    }
}
