package com.kafka.consumer;

import com.kafka.model.Employee;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class EmployeeConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeConsumer.class);
    private final List<Employee> consumedEmployees = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "${kafka.topics.employee}", 
                  groupId = "${spring.kafka.consumer.group-id}", 
                  containerFactory = "kafkaListenerContainerFactory")
    @CircuitBreaker(name = "employeeConsumerCB", fallbackMethod = "fallbackConsume")
    @Retry(name = "employeeConsumerRetry")
    public void consumeEmployee(ConsumerRecord<String, Employee> record, Acknowledgment acknowledgment) {
        try {
            Employee employee = record.value();
            logger.info("Consumed employee: {} from partition {} offset {}", 
                       employee, record.partition(), record.offset());
            
            // Simulate processing
            if (employee.getName() == null) {
                throw new IllegalArgumentException("Invalid employee name");
            }
            
            consumedEmployees.add(employee);
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged employee: {}", employee);
        } catch (Exception e) {
            logger.error("Error processing record: {}", record, e);
            throw e;
        }
    }

    public void fallbackConsume(ConsumerRecord<String, Employee> record, Acknowledgment acknowledgment, Throwable t) {
        logger.error("Consumer fallback for record {} due to {}", record, t.getMessage());
        // Send to DLQ automatically handled by ErrorHandler
        acknowledgment.acknowledge();
    }

    public List<Employee> getConsumedEmployees() {
        return consumedEmployees;
    }
}