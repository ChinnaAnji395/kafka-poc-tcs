package com.kafka.model;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int age;
    private double salary;
    private LocalDate dob;
}
