package com.deepsea.vesseldataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class VesselDataServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(VesselDataServiceApplication.class, args);
    }
}
