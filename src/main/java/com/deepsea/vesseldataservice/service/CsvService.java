package com.deepsea.vesseldataservice.service;

import com.deepsea.vesseldataservice.model.InvalidVesselData;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    Logger logger = LoggerFactory.getLogger("CsvService");

    private final ValidVesselDataRepository validVesselDataRepository;
    private final InvalidVesselDataRepository invalidVesselDataRepository;

    public CsvService(ValidVesselDataRepository validVesselDataRepository, InvalidVesselDataRepository invalidVesselDataRepository) {

        this.validVesselDataRepository = validVesselDataRepository;
        this.invalidVesselDataRepository = invalidVesselDataRepository;
    }

    public void readCsv() throws IOException {

        var csvFilePath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\vessel_data_min.csv";
        logger.info("Reading CSV file. Path is: " + csvFilePath);

        Charset charset = StandardCharsets.UTF_8;

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath, charset))) {
            CsvToBean<ValidVesselData> csvToBean = new CsvToBeanBuilder<ValidVesselData>(reader)
                    .withType(ValidVesselData.class)
                    .build();

            List<ValidVesselData> beans = csvToBean.parse();

            for (ValidVesselData validVesselData : beans) {
                if (isValidData(validVesselData)) {
                    validVesselDataRepository.save(validVesselData);
                } else {
                    invalidVesselDataRepository.save(mapToInvalidData(validVesselData));
                }
            }
            logger.info("done.. ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidData(ValidVesselData vesselData) {

        try {
            // Validate latitude and longitude after parsing to double
            Double latitude = Double.parseDouble(vesselData.getLatitude());
            Double longitude = Double.parseDouble(vesselData.getLongitude());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return false;
            }

            // Validate numeric values (ensure they are not below zero)
            Double power = Double.parseDouble(vesselData.getPower());
            Double fuelConsumption = Double.parseDouble(vesselData.getFuelConsumption());
            Double actualSpeedOverground = Double.parseDouble(vesselData.getActualSpeedOverground());
            Double proposedSpeedOverground = Double.parseDouble(vesselData.getProposedSpeedOverground());
            Double predictedFuelConsumption = Double.parseDouble(vesselData.getPredictedFuelConsumption());

            if (power < 0 || fuelConsumption < 0 || actualSpeedOverground < 0 || proposedSpeedOverground < 0 || predictedFuelConsumption < 0) {
                return false;
            }

            // Add additional validation checks, such as for outliers
            // Example: check if the difference between actual and proposed speed is too large
            if (Math.abs(actualSpeedOverground - proposedSpeedOverground) > 10) {
                return false;
            }
        } catch (NumberFormatException e) {
            // If parsing fails, consider the data invalid
            return false;
        }

        return true;
    }

    private InvalidVesselData mapToInvalidData(ValidVesselData validVesselData) {
        // Map invalid rows to InvalidVesselData object
        InvalidVesselData invalidData = new InvalidVesselData();
        invalidData.setVesselCode(validVesselData.getVesselCode());
        invalidData.setDatetime(validVesselData.getDatetime());
        invalidData.setLatitude(validVesselData.getLatitude());
        invalidData.setLongitude(validVesselData.getLongitude());
        invalidData.setPower(validVesselData.getPower());
        invalidData.setFuelConsumption(validVesselData.getFuelConsumption());
        invalidData.setActualSpeedOverground(validVesselData.getActualSpeedOverground());
        invalidData.setProposedSpeedOverground(validVesselData.getProposedSpeedOverground());
        invalidData.setPredictedFuelConsumption(validVesselData.getPredictedFuelConsumption());
        return invalidData;
    }
}
