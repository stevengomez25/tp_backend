package com.tempmailpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
// Estas anotaciones aseguran que Spring encuentre tus modelos y repositorios en las otras carpetas
@EntityScan("com.tempmailpro.domain.models")
@EnableJpaRepositories("com.tempmailpro.infrastructure.adapters")
public class TempMailProApplication {

    public static void main(String[] args) {
        SpringApplication.run(TempMailProApplication.class, args);
    }

}