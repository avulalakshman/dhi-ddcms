/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 *
 * @author Pradeepkm
 */
@SpringBootApplication
public class DdmsTestApp {

    public static void main(String[] args) {
        SpringApplication.run(DdmsTestApp.class, args);
    }

    @Bean
    public CommandLineRunner runner() {
        return (String... args) -> {
            System.out.println("Ddms Test Application Started...");
        };
    }
}
