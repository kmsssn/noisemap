package kz.noisemap.recordingservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationDto {
    private String model;
    private Double calibrationOffsetDb;
    private Boolean verified;
}