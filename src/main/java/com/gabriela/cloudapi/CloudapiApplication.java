package com.gabriela.cloudapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

@SpringBootApplication
public class CloudapiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CloudapiApplication.class);
        ConfigurableEnvironment env = new StandardEnvironment();
        System.out.println("DB_URL=" + env.getProperty("DB_URL"));
        app.run(args);
    }

}