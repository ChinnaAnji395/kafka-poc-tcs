package com.kafka.producer;

import com.kafka.model.Employee;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
                });
    }

    public CompletableFuture<Void> fallbackSend(Employee employee, Throwable t) {
        logger.error("Fallback: Failed to send employee {} due to {}", employee.getName(), t.toString());

        
        if (t instanceof CompletionException && t.getCause() != null) {
            logger.error("Root cause: {}", t.getCause().toString());
        }

        // Send to DLQ manually in fallback
        kafkaTemplate.send(DLQ_TOPIC, employee.getName(), employee);
        return CompletableFuture.completedFuture(null);
    }
}
