package com.deepsea.vesseldataservice.model;

import com.opencsv.bean.CsvBindByName;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class InvalidVesselData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @CsvBindByName(column = "vessel_code")
    private String vesselCode;

    @CsvBindByName(column = "datetime")
    private String datetime;

    @CsvBindByName(column = "latitude")
    private String latitude;

    @CsvBindByName(column = "longitude")
    private String longitude;

    @CsvBindByName(column = "power")
    private String power;

    @CsvBindByName(column = "fuel_consumption")
    private String fuelConsumption;

    @CsvBindByName(column = "actual_speed_overground")
    private String actualSpeedOverground;

    @CsvBindByName(column = "proposed_speed_overground")
    private String proposedSpeedOverground;

    @CsvBindByName(column = "predicted_fuel_consumption")
    private String predictedFuelConsumption;
}
