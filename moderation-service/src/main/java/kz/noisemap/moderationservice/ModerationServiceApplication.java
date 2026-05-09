package kz.noisemap.moderationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"kz.noisemap.moderationservice", "kz.noisemap.common"})
public class ModerationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }
}