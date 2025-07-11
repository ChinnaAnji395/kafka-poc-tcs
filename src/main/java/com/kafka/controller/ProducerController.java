package com.kafka.controller;

import com.kafka.model.Employee;
import com.kafka.producer.EmployeeProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ProducerController {

    private final EmployeeProducer employeeProducer;
    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    @PostMapping("/publish")
    public ResponseEntity<String> publishEmployee(@RequestBody Employee employee) {
        logger.info("API request to publish: {}", employee);
        try {
            employeeProducer.sendEmployee(employee).join();
            return ResponseEntity.ok("Published successfully");
        } catch (Exception ex) {
            logger.error("Kafka unavailable. Fallback returned. {}", ex.getMessage());
            return ResponseEntity.status(503).body("Fallback: Kafka broker unavailable");
        }
    }
}
