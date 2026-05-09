package kz.noisemap.gamificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"kz.noisemap.gamificationservice", "kz.noisemap.common"})
@EnableCaching
public class GamificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GamificationServiceApplication.class, args);
    }
}