package kz.noisemap.mappingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"kz.noisemap.mappingservice", "kz.noisemap.common"})
public class MappingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MappingServiceApplication.class, args);
    }
}