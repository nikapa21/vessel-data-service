package com.deepsea.vesseldataservice.service;

import static java.util.Objects.isNull;

import com.deepsea.vesseldataservice.model.InvalidVesselData;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CsvService {

    private static final Logger logger = LoggerFactory.getLogger(CsvService.class);

    private final ValidVesselDataRepository validVesselDataRepository;
    private final InvalidVesselDataRepository invalidVesselDataRepository;

    public CsvService(ValidVesselDataRepository validVesselDataRepository, InvalidVesselDataRepository invalidVesselDataRepository) {

        this.validVesselDataRepository = validVesselDataRepository;
        this.invalidVesselDataRepository = invalidVesselDataRepository;
    }

    public void readCsvInChunks() {

        var csvFilePath = System.getProperty("user.dir") + "/src/main/resources/static/vessel_data.csv";
        logger.info("Reading CSV file in chunks. Path is: {}", csvFilePath);

        try (var reader = new BufferedReader(new FileReader(csvFilePath, StandardCharsets.UTF_8))) {

            String line;
            var validDataInsertedCounter = 0;
            var invalidDataInsertedCounter = 0;
            int batchSize = 10000; // Adjust the batch size as needed
            List<ValidVesselData> validDataList = new ArrayList<>();
            List<InvalidVesselData> invalidDataList = new ArrayList<>();

            while ((line = reader.readLine()) != null) {

                line = line.replace("\"", ""); //Remove all double quotes from strings

                // Parse the line into a ValidVesselData object
                var data = parseLineToValidVesselData(line);

                if (isNull(data)) {
                    continue;
                }

                var invalidReason = getInvalidReason(data);
                if (isNull(invalidReason)) {
                    calculateNewMetrics(data);
                    validDataList.add(data);
                    validDataInsertedCounter++;
                } else {
                    invalidDataList.add(mapToInvalidData(data, invalidReason));
                    invalidDataInsertedCounter++;
                }

                if (validDataList.size() >= batchSize) {
                    validVesselDataRepository.saveAll(validDataList);
                    logger.info("Inserted {} valid data", validDataInsertedCounter);
                    validDataList = new ArrayList<>();
                }

                if (invalidDataList.size() >= batchSize) {
                    invalidVesselDataRepository.saveAll(invalidDataList);
                    logger.info("Inserted {} invalid data", invalidDataInsertedCounter);
                    invalidDataList = new ArrayList<>();
                }
            }

            // Save any remaining data
            if (!validDataList.isEmpty()) {
                validVesselDataRepository.saveAll(validDataList);
                logger.info("Flush the buffer with the last {} valid data", validDataList.size());
            }
            if (!invalidDataList.isEmpty()) {
                invalidVesselDataRepository.saveAll(invalidDataList);
                logger.info("Flush the buffer with the last {} invalid data", invalidDataList.size());
            }

            logger.info("Data processing and insertion completed.");
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during CSV processing: {}", e.getMessage(), e);
        }
    }

    private String getInvalidReason(ValidVesselData vesselData) {

        List<String> reasons = new ArrayList<>();

        try {
            var latitude = Double.parseDouble(vesselData.getLatitude());
            var longitude = Double.parseDouble(vesselData.getLongitude());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                reasons.add("Invalid latitude or longitude");
            }

            var power = Double.parseDouble(vesselData.getPower());
            var fuelConsumption = Double.parseDouble(vesselData.getFuelConsumption());
            var actualSpeedOverground = Double.parseDouble(vesselData.getActualSpeedOverground());
            var proposedSpeedOverground = Double.parseDouble(vesselData.getProposedSpeedOverground());
            var predictedFuelConsumption = Double.parseDouble(vesselData.getPredictedFuelConsumption());

            if (power < 0 || fuelConsumption < 0 || actualSpeedOverground < 0 || proposedSpeedOverground < 0 || predictedFuelConsumption < 0) {
                reasons.add("Negative values");
            }

            if (Math.abs(actualSpeedOverground - proposedSpeedOverground) > 10) {
                reasons.add("Outliers");
            }
        } catch (NumberFormatException e) {
            reasons.add("Number format exception");
        }

        return reasons.isEmpty() ? null : String.join(", ", reasons);
    }

    void calculateNewMetrics(ValidVesselData vesselData) {

        var actualSpeed = Double.parseDouble(vesselData.getActualSpeedOverground());
        var proposedSpeed = Double.parseDouble(vesselData.getProposedSpeedOverground());
        var speedDifference = actualSpeed - proposedSpeed;
        vesselData.setSpeedDifference(speedDifference);
        vesselData.setCompliancePercentage(calculateCompliancePercentage(vesselData));
    }

    private double calculateCompliancePercentage(ValidVesselData vesselData) {

        var speedDifference = vesselData.getSpeedDifference();
        var proposedSpeed = Double.parseDouble(vesselData.getProposedSpeedOverground());

        if (proposedSpeed == 0) {
            return 0; // Avoid division by zero
        }

        var compliance = 100 - (Math.abs(speedDifference) / proposedSpeed) * 100;
        return Math.max(compliance, 0); // Ensure compliance is not negative
    }

    protected InvalidVesselData mapToInvalidData(ValidVesselData validVesselData, String reason) {

        var invalidData = new InvalidVesselData();
        invalidData.setVesselCode(validVesselData.getVesselCode());
        invalidData.setDatetime(validVesselData.getDatetime());
        invalidData.setLatitude(validVesselData.getLatitude());
        invalidData.setLongitude(validVesselData.getLongitude());
        invalidData.setPower(validVesselData.getPower());
        invalidData.setFuelConsumption(validVesselData.getFuelConsumption());
        invalidData.setActualSpeedOverground(validVesselData.getActualSpeedOverground());
        invalidData.setProposedSpeedOverground(validVesselData.getProposedSpeedOverground());
        invalidData.setPredictedFuelConsumption(validVesselData.getPredictedFuelConsumption());
        invalidData.setInvalidReason(reason);
        return invalidData;
    }

    private ValidVesselData parseLineToValidVesselData(String line) {

        String[] fields = line.split(",");

        // Handle cases where the line might not have the expected number of fields
        if (fields.length != 9) {
            logger.warn("Skipping line due to wrong number fields: {}", line);
            return null;
        }
        return new ValidVesselData(fields);
    }
}