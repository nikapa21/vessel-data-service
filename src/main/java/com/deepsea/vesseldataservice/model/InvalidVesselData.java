package com.deepsea.vesseldataservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class InvalidVesselData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String vesselCode;

    private String datetime;

    private String latitude;

    private String longitude;

    private String power;

    private String fuelConsumption;

    private String actualSpeedOverground;

    private String proposedSpeedOverground;

    private String predictedFuelConsumption;

    private String invalidReason;

    public InvalidVesselData(String vesselCode, String datetime, String latitude, String longitude, String power, String fuelConsumption, String actualSpeedOverground, String proposedSpeedOverground, String predictedFuelConsumption) {

        this.vesselCode = vesselCode;
        this.datetime = datetime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.power = power;
        this.fuelConsumption = fuelConsumption;
        this.actualSpeedOverground = actualSpeedOverground;
        this.proposedSpeedOverground = proposedSpeedOverground;
        this.predictedFuelConsumption = predictedFuelConsumption;
    }
}
