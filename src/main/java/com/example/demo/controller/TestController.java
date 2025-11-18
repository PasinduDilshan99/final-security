package com.example.demo.controller;

import com.example.demo.model.Student;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v0")
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    @GetMapping(path = "/test")
    @PreAuthorize("hasAuthority('READ_PRIVILEGE')")
    public String test(HttpServletRequest httpServletRequest) {
        LOGGER.info(httpServletRequest.toString());
        return "Hello World";
    }

    List<Student> students = new ArrayList<>(List.of(
            new Student(1, "John", "Doe", "john.doe@example.com"),
            new Student(2, "Jane", "Smith", "jane.smith@example.com"),
            new Student(3, "Alice", "Johnson", "alice.johnson@example.com"),
            new Student(4, "Bob", "Brown", "bob.brown@example.com"),
            new Student(5, "Eva", "Davis", "eva.davis@example.com")
    ));


    @GetMapping(path = "/students")
    @PreAuthorize("hasAuthority('READ_PRIVILEGE')")
    public List<Student> getStudents() {
        return students;
    }

    @PostMapping(path = "/students")
    @PreAuthorize("hasAuthority('WRITE_PRIVILEGE')")
    public Student addStudent(@RequestBody Student student) {
        students.add(student);
        return student;
    }

}