package com.heraizen.dhi.dlibms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class DhiDlibmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DhiDlibmsApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner() {
        return (String... args) -> {
            System.out.println("Dlibms JR2 Application Started...");
        };
    }

}
