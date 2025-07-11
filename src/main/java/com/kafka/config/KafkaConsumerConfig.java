package com.kafka.config;

import com.kafka.model.Employee;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.topics.employee.dlq}")
    private String dlqTopic;

    @Bean
    public ConsumerFactory<String, Employee> consumerFactory() {
        JsonDeserializer<Employee> jsonDeserializer = new JsonDeserializer<>(Employee.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.setUseTypeMapperForKey(true);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
        props.put("spring.deserializer.key.delegate.class", StringDeserializer.class);
        props.put("spring.deserializer.value.delegate.class", JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.kafka.model.Employee");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Employee> kafkaListenerContainerFactory(
            KafkaTemplate<String, Employee> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Employee> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);

        
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, -1));

        
        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxInterval(10000L);

        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.println("Retry attempt #" + deliveryAttempt + " for record: " + record + ", Exception: " + ex);
        });

        

        
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                IllegalArgumentException.class
        );

        factory.setCommonErrorHandler(errorHandler);
        factory.setRetryTemplate(retryTemplate());

        return factory;
    }

    @Bean
    public org.springframework.retry.support.RetryTemplate retryTemplate() {
        org.springframework.retry.support.RetryTemplate retryTemplate = new org.springframework.retry.support.RetryTemplate();

        org.springframework.retry.backoff.ExponentialBackOffPolicy backOffPolicy =
                new org.springframework.retry.backoff.ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(new org.springframework.retry.policy.SimpleRetryPolicy(3));

        return retryTemplate;
    }
}
