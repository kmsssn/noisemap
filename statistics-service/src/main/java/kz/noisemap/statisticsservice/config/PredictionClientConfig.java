package kz.noisemap.statisticsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PredictionClientConfig {

    @Value("${services.prediction.url:http://ml-prediction:5000}")
    private String predictionUrl;

    @Value("${services.prediction.timeout-seconds:10}")
    private int timeoutSeconds;

    @Bean
    public RestClient predictionRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl(predictionUrl)
                .requestFactory(factory)
                .build();
    }
}