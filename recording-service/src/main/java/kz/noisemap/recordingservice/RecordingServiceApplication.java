package kz.noisemap.recordingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"kz.noisemap.recordingservice", "kz.noisemap.common"})
public class RecordingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecordingServiceApplication.class, args);
    }
}