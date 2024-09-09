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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class CsvService {

    private static final Logger logger = LoggerFactory.getLogger(CsvService.class);

    @Value("classpath:static/vessel_data.csv")
    Resource csvFileResource;

    @Value("${csv.file.process.batch.size:100000}")
    private Integer batchSize;

    private final ValidVesselDataRepository validVesselDataRepository;
    private final InvalidVesselDataRepository invalidVesselDataRepository;

    public CsvService(ValidVesselDataRepository validVesselDataRepository, InvalidVesselDataRepository invalidVesselDataRepository) {

        this.validVesselDataRepository = validVesselDataRepository;
        this.invalidVesselDataRepository = invalidVesselDataRepository;
    }

    public void readCsvInChunks() {

        logger.info("Reading CSV file in chunks of {}. Path is: {}", batchSize, csvFileResource);

        try (var reader = new BufferedReader(new FileReader(csvFileResource.getFile(), StandardCharsets.UTF_8))) {

            String line;
            var validDataInsertedCounter = 0; // Use counters to be logged and keep track of the chunks insertions - Valid counter
            var invalidDataInsertedCounter = 0; // Invalid counter

            List<ValidVesselData> validDataList = new ArrayList<>();
            List<InvalidVesselData> invalidDataList = new ArrayList<>();

            while ((line = reader.readLine()) != null) {

                line = line.replace("\"", ""); // Remove all double quotes from strings

                // Parse the line into a ValidVesselData object
                var data = parseLineToValidVesselData(line);

                if (isNull(data)) {
                    continue;
                }

                var invalidReason = getInvalidReason(data);

                if (isNull(invalidReason)) { // Data are valid
                    calculateNewMetrics(data);
                    validDataList.add(data);
                    validDataInsertedCounter++;
                } else { // Data are invalid
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

    String getInvalidReason(ValidVesselData vesselData) {

        try {
            var latitude = Double.parseDouble(vesselData.getLatitude());
            var longitude = Double.parseDouble(vesselData.getLongitude());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return "Invalid latitude or longitude";
            }

            var power = Double.parseDouble(vesselData.getPower());
            var fuelConsumption = Double.parseDouble(vesselData.getFuelConsumption());
            var actualSpeedOverground = Double.parseDouble(vesselData.getActualSpeedOverground());
            var proposedSpeedOverground = Double.parseDouble(vesselData.getProposedSpeedOverground());
            var predictedFuelConsumption = Double.parseDouble(vesselData.getPredictedFuelConsumption());

            if (power < 0 || fuelConsumption < 0 || actualSpeedOverground < 0 || proposedSpeedOverground < 0 || predictedFuelConsumption < 0) {
                return "Negative values";
            }

            if (actualSpeedOverground > 20 || proposedSpeedOverground > 20) {
                return "Outliers";
            }
        } catch (NumberFormatException e) {
            return "Number format exception"; // Catch null values
        }
        return null; // Returns null, but is handled immediately
    }

    void calculateNewMetrics(ValidVesselData vesselData) {

        var actualSpeed = Double.parseDouble(vesselData.getActualSpeedOverground());
        var proposedSpeed = Double.parseDouble(vesselData.getProposedSpeedOverground());
        var speedDifference = actualSpeed - proposedSpeed;
        vesselData.setSpeedDifference(speedDifference);
        vesselData.setCompliancePercentage(calculateCompliancePercentage(vesselData));
    }

    double calculateCompliancePercentage(ValidVesselData vesselData) {

        var speedDifference = vesselData.getSpeedDifference();
        var proposedSpeed = Double.parseDouble(vesselData.getProposedSpeedOverground());

        if (proposedSpeed == 0) {
            return 0; // Avoid division by zero
        }

        var compliance = 100 - (Math.abs(speedDifference) / proposedSpeed) * 100;
        return Math.max(compliance, 0); // Ensure compliance is not negative
    }

    private InvalidVesselData mapToInvalidData(ValidVesselData validVesselData, String reason) {

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

    ValidVesselData parseLineToValidVesselData(String line) {

        String[] fields = line.split(",");

        // Handle cases where the line might not have the expected number of fields
        if (fields.length != 9) {
            logger.warn("Skipping line due to wrong number fields: {}", line);
            return null; // Returns null, but is handled immediately
        }
        return new ValidVesselData(fields);
    }
}