package com.smartdata.smartruledatagen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.smartdata.smartruledatagen")
public class TestDataGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestDataGenApplication.class, args);
    }
}
