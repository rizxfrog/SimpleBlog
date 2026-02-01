package com.simpleblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.simpleblog.mapper")
public class SimpleBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleBlogApplication.class, args);
    }
}
