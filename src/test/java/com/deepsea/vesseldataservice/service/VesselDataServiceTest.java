package com.deepsea.vesseldataservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.deepsea.vesseldataservice.exception.DataNotFoundException;
import com.deepsea.vesseldataservice.model.ValidVesselData;
import com.deepsea.vesseldataservice.repository.InvalidVesselDataRepository;
import com.deepsea.vesseldataservice.repository.ValidVesselDataRepository;
import com.deepsea.vesseldataservice.response.InvalidReasonResponse;
import com.deepsea.vesseldataservice.response.SpeedDifferenceResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VesselDataServiceTest {

    @Mock
    private ValidVesselDataRepository validVesselDataRepository;

    @Mock
    private InvalidVesselDataRepository invalidVesselDataRepository;

    @InjectMocks
    private VesselDataService vesselDataService;

    @Test
    void testCalculateSpeedDifferenceWithCoordinates() {
        // Arrange
        String vesselCode = "3001";
        String latitude = "44.8565368652344";
        String longitude = "-51.4986343383789";

        List<ValidVesselData> vesselDataList = new ArrayList<>();
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                latitude, longitude, "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");
        validData.setSpeedDifference(-1.109869775917801); // Set speed difference for testing
        vesselDataList.add(validData);

        when(validVesselDataRepository.findByVesselCodeAndLatitudeAndLongitude(vesselCode, latitude, longitude))
                .thenReturn(vesselDataList);

        // Act
        List<SpeedDifferenceResponse> response = vesselDataService.calculateSpeedDifference(vesselCode, latitude, longitude);

        // Assert
        assertEquals(1, response.size());
        assertEquals(latitude, response.getFirst().getLatitude());
        assertEquals(longitude, response.getFirst().getLongitude());
        assertEquals(-1.109869775917801, response.getFirst().getSpeedDifference());
    }

    @Test
    void testCalculateSpeedDifferenceWithoutCoordinates() {
        // Arrange
        String vesselCode = "3001";

        List<ValidVesselData> vesselDataList = new ArrayList<>();
        ValidVesselData validData = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "10.2894496917725", "-14.7888498306274", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");
        validData.setSpeedDifference(-0.846507989632189);
        ValidVesselData validData2 = new ValidVesselData("3001", "2023-10-06 01:06:00",
                "10.2894401550293", "-14.7888078689575", "4947.18", "16.6132", "8.95998", "10.0698497759178", "16.4471915433183");
        validData2.setSpeedDifference(-0.709315650920331);
        vesselDataList.add(validData);
        vesselDataList.add(validData2);

        when(validVesselDataRepository.findByVesselCode(vesselCode)).thenReturn(vesselDataList);

        // Act
        List<SpeedDifferenceResponse> response = vesselDataService.calculateSpeedDifference(vesselCode, null, null);

        // Assert
        assertEquals(2, response.size());
    }

    @Test
    void testCalculateSpeedDifferenceThrowsExceptionWhenNoDataFound() {
        // Arrange
        // Cannot find such data
        String vesselCode = "3001";
        String latitude = "42.8565368652344";
        String longitude = "-58.4986343383789";

        when(validVesselDataRepository.findByVesselCodeAndLatitudeAndLongitude(vesselCode, latitude, longitude))
                .thenReturn(new ArrayList<>()); // Return empty list

        // Act
        DataNotFoundException exception = assertThrows(DataNotFoundException.class, () -> vesselDataService.calculateSpeedDifference(vesselCode, latitude, longitude));

        // Assert
        assertEquals("No data found for the given vessel code and coordinates.", exception.getMessage());
    }

    @Test
    void testGetInvalidReasonsByVesselCode() {
        // Mocking the repository response
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[] {"Invalid latitude or longitude", 1L});
        when(invalidVesselDataRepository.findInvalidReasonsByVesselCode("1")).thenReturn(results);

        // Call the method
        List<InvalidReasonResponse> response = vesselDataService.getInvalidReasonsByVesselCode("1");

        // Verify the response
        assertEquals(1, response.size());
        assertEquals("Invalid latitude or longitude", response.getFirst().getReason());
        assertEquals(1L, response.getFirst().getCount());
    }

    @Test
    void testGetInvalidReasonsByVesselCodeThrowsException() {
        // Mocking the repository response
        when(invalidVesselDataRepository.findInvalidReasonsByVesselCode("1")).thenReturn(new ArrayList<>());

        // Call the method and expect an exception
        assertThrows(DataNotFoundException.class, () -> vesselDataService.getInvalidReasonsByVesselCode("1"));
    }
}