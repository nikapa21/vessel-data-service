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
public class ValidVesselData {

    public ValidVesselData(String[] commaSeparatedValues) {

        this.vesselCode = commaSeparatedValues[0];
        this.datetime = commaSeparatedValues[1];
        this.latitude = commaSeparatedValues[2];
        this.longitude = commaSeparatedValues[3];
        this.power = commaSeparatedValues[4];
        this.fuelConsumption = commaSeparatedValues[5];
        this.actualSpeedOverground = commaSeparatedValues[6];
        this.proposedSpeedOverground = commaSeparatedValues[7];
        this.predictedFuelConsumption = commaSeparatedValues[8];
    }

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

    private double speedDifference;

    private double compliancePercentage;

    public ValidVesselData(String vesselCode, String datetime, String latitude, String longitude, String power, String fuelConsumption, String actualSpeedOverground, String proposedSpeedOverground, String predictedFuelConsumption) {

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
