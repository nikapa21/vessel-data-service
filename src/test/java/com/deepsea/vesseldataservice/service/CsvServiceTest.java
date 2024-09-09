package com.deepsea.vesseldataservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.deepsea.vesseldataservice.model.ValidVesselData;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

class CsvServiceTest {

    @InjectMocks
    private CsvService csvService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetInvalidReasonWithValidData() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetInvalidReasonWithInvalidLatitude() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "100.0", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Invalid latitude or longitude", result);
    }

    @Test
    void testGetInvalidReasonWithInvalidLongitude() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-200.0", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Invalid latitude or longitude", result);
    }

    @Test
    void testGetInvalidReasonWithNegativeValues() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "-4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Negative values", result);
    }

    @Test
    void testGetInvalidReasonWithOutliers() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "20.0698497759178", "30.0");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Outliers", result);
    }

    @Test
    void testGetInvalidReasonWithNumberFormatException_NullValues() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("19310", "null",
                "null", "null", "null", "null", "null", "null", "0");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Number format exception", result);
    }

    @Test
    void testGetInvalidReasonWithNumberFormatException_NotNumber() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "invalidPower", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        String result = csvService.getInvalidReason(validData);

        // Assert
        assertEquals("Number format exception", result);
    }

    @Test
    void testCalculateNewMetricsWithValidData() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        csvService.calculateNewMetrics(validData);

        // Assert
        assertEquals( -1.109869775917801, validData.getSpeedDifference());
        assertEquals(88.97828864764128, validData.getCompliancePercentage()); // Assuming compliance is calculated correctly
    }

    @Test
    void testCalculateNewMetricsWithZeroProposedSpeed() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "0.0", "16.4471915433183");

        // Act
        csvService.calculateNewMetrics(validData);

        // Assert
        assertEquals(8.95998, validData.getSpeedDifference());
        assertEquals(0.0, validData.getCompliancePercentage());
    }

    @Test
    void testCalculateNewMetricsWithNegativeActualSpeed() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "-8.95998", "10.0698497759178", "16.4471915433183");

        // Act
        csvService.calculateNewMetrics(validData);

        // Assert
        assertEquals(-19.0298297759178, validData.getSpeedDifference());
        assertEquals(0.0, validData.getCompliancePercentage());
    }

    @Test
    void testCalculateCompliancePercentageWithValidData() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");
        validData.setSpeedDifference(-1.109869775917801); // Set speed difference for testing

        // Act
        double compliance = csvService.calculateCompliancePercentage(validData);

        // Assert
        assertEquals(88.97828864764128, compliance); // Expected compliance percentage
    }

    @Test
    void testCalculateCompliancePercentageWithZeroProposedSpeed() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "0.0", "16.4471915433183");

        // Act
        double compliance = csvService.calculateCompliancePercentage(validData);

        // Assert
        assertEquals(0.0, compliance); // Compliance should be 0% when proposed speed is 0
    }

    @Test
    void testCalculateCompliancePercentageWithExactCompliance() {
        // Arrange
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "44.8565368652344", "-51.4986343383789", "4947.18", "16.6132", "8.95998", "8.95998", "16.4471915433183");
        validData.setSpeedDifference(0.0); // Set speed difference to 0

        // Act
        double compliance = csvService.calculateCompliancePercentage(validData);

        // Assert
        assertEquals(100.0, compliance); // Compliance should be 100% when speed difference is 0
    }

    @Test
    void testParseLineToValidVesselDataWithValidInput() throws IOException {
        // Arrange
        String line = readLineFromCsv(1); // Read the first line

        line = line.replace("\"", ""); // Remove all double quotes from strings
        // Act
        ValidVesselData result = csvService.parseLineToValidVesselData(line);

        // Assert
        assertNotNull(result);
        assertEquals("3001", result.getVesselCode());
        assertEquals("2023-06-01 00:00:00", result.getDatetime());
        assertEquals("10.2894458770752", result.getLatitude());
        assertEquals("-14.788875579834", result.getLongitude());
        assertEquals("0", result.getPower());
        assertEquals("0", result.getFuelConsumption());
        assertEquals("0.039996", result.getActualSpeedOverground());
        assertEquals("-0.189904262498021", result.getProposedSpeedOverground());
        assertEquals("0", result.getPredictedFuelConsumption());
    }

    @Test
    void testParseLineToValidVesselDataWithNullValues() throws IOException {
        // Arrange
        String line = readLineFromCsv(2); // Read the second line

        line = line.replace("\"", ""); // Remove all double quotes from strings

        // Act
        ValidVesselData result = csvService.parseLineToValidVesselData(line);

        // Assert
        assertNotNull(result);
        assertEquals("19310", result.getVesselCode());
        assertEquals("2023-06-01 00:07:00", result.getDatetime());
        assertEquals("NULL", result.getLatitude());
        assertEquals("NULL", result.getLongitude());
        assertEquals("NULL", result.getPower());
        assertEquals("NULL", result.getFuelConsumption());
        assertEquals("NULL", result.getActualSpeedOverground());
        assertEquals("NULL", result.getProposedSpeedOverground());
        assertEquals("0", result.getPredictedFuelConsumption());
    }

    @Test
    void testParseLineToValidVesselDataWithInvalidInput() throws IOException {
        // Arrange
        String line = readLineFromCsv(3); // Only 3 fields

        line = line.replace("\"", ""); // Remove all double quotes from strings

        // Act
        ValidVesselData result = csvService.parseLineToValidVesselData(line);

        // Assert
        assertNull(result); // Should return null due to wrong number of fields
    }

    private String readLineFromCsv(int lineNumber) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get("src/test/resources/test-data.csv"))) {
            for (int i = 0; i < lineNumber; i++) {
                br.readLine(); // Skip lines until the desired line
            }
            return br.readLine(); // Read the desired line
        }
    }
}