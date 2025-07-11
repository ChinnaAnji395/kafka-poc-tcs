package com.kafka.controller;

import com.kafka.consumer.EmployeeConsumer;
import com.kafka.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ConsumerFetchController {

    private final EmployeeConsumer employeeConsumer;

   
    @GetMapping("/consumed")
    public ResponseEntity<List<Employee>> getConsumedEmployees() {
        return ResponseEntity.ok(employeeConsumer.getConsumedEmployees());
    }
}
