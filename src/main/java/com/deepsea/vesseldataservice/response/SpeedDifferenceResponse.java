package com.deepsea.vesseldataservice.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpeedDifferenceResponse {

    private String latitude;
    private String longitude;
    private double speedDifference;
}