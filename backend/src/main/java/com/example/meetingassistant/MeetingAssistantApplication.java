package com.example.meetingassistant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MeetingAssistantApplication {
  public static void main(String[] args) { SpringApplication.run(MeetingAssistantApplication.class, args); }
  @Bean CommandLineRunner seed(InternalClientRepository clients, EmployeeRepository employees) {
    return args -> {
      if (clients.count() == 0) clients.save(new InternalClient("Podjetje ABC d.o.o.", "Ana Novak"));
      if (employees.count() == 0) { employees.save(new Employee("Luka")); employees.save(new Employee("Ana")); employees.save(new Employee("Marko")); }
    };
  }
}
