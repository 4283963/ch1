package com.smartpark;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartpark.mapper")
public class SmartParkApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartParkApplication.class, args);
        System.out.println("========================================");
        System.out.println("  SmartPark Backend Started Successfully!");
        System.out.println("  Server Port: 9090");
        System.out.println("  WebSocket Endpoint: ws://localhost:9090/ws/parking");
        System.out.println("========================================");
    }
}
