package com.kafka.producer;

import com.kafka.model.Employee;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmployeeProducer {

    private final KafkaTemplate<String, Employee> kafkaTemplate;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeProducer.class);
    private static final String TOPIC = "employee-topic";
    private static final String DLQ_TOPIC = "employee-topic-dlq";

    @CircuitBreaker(name = "employeeCB", fallbackMethod = "fallbackSend")
    @Retry(name = "employeeRetry")
    public CompletableFuture<Void> sendEmployee(Employee employee) {
        logger.info("Sending employee to Kafka: {}", employee);
        
        if (employee.getName() == null) {
            throw new IllegalArgumentException("Employee name cannot be null");
        }

        return kafkaTemplate.send(TOPIC, employee.getName(), employee)
                .completable()
                .thenAccept(result -> {
                    RecordMetadata metadata = result.getRecordMetadata();
                    logger.info("Sent to topic={} partition={} offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                })
                .exceptionally(ex -> {
                    logger.error("Failed to send to Kafka: {}", ex.getMessage());
                    // Send to DLQ
                    kafkaTemplate.send(DLQ_TOPIC, employee.getName(), employee);
                    throw new org.springframework.kafka.KafkaException("Kafka send failed, sent to DLQ", ex);
                });
    }

    public CompletableFuture<Void> fallbackSend(Employee employee, Throwable t) {
        logger.warn("Fallback for employee {}: {}", employee.getName(), t.getMessage());
        // Send to DLQ in fallback
        kafkaTemplate.send(DLQ_TOPIC, employee.getName(), employee);
        return CompletableFuture.completedFuture(null);
    }
}