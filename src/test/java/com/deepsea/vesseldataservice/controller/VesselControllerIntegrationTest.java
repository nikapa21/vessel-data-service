package com.deepsea.vesseldataservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows non-static @BeforeAll
class VesselControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    void whenGetRequestToProcessFile_thenCorrectResponse() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/processFile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string("Data processing and insertion completed!"));
    }

    @Test
    void whenGetRequestToSpeedDifferences_thenCorrectResponse() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }

    @Test
    void whenGetRequestToSpeedDifferencesPageZero_thenCorrectResponses() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].latitude").value("44.8565368652344"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].longitude").value("-51.4986343383789"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].speedDifference").value(-1.109869775917801))
                .andExpect(MockMvcResultMatchers.jsonPath("$['number']").value(0)) // Check that the returned page number is 0
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }

    @Test
    void whenGetRequestToSpeedDifferencesPageFive_thenCorrectResponses() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vessels/3001/speed-differences?page=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$['number']").value(5)) // Check that the returned page number is 5
                .andExpect(MockMvcResultMatchers.jsonPath("$['pageable']['paged']").value("true"));
    }

    @Test
    void handleSpeedDifferenceDataNotFoundException_ShouldReturnNotFound() throws Exception {

        // Arrange
        String vesselCode = "19310";
        String latitude = "44.8565368652344";
        String longitude = "-51.4986343383789";
        String errorMessage = "No data found for the given vessel code and coordinates.";

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/speed-difference")
                        .param("latitude", latitude)
                        .param("longitude", longitude)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));
    }

    @Test
    void handleSpeedDifferenceConstraintViolation_ShouldReturnBadRequest() throws Exception {

        // Arrange
        String vesselCode = "19310";
        String latitude = "somethingOtherThanTheRegexPrompted";
        String longitude = "-51.4986343383789";

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/speed-difference")
                        .param("latitude", latitude)
                        .param("longitude", longitude)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidReasonsSuccess() throws Exception {

        // Arrange
        String vesselCode = "19310";

        // 19310 vessel had for sure a lot of Number format exception, so we expect the result list to have this key
        // For the value of this key it depends on the dataset used, so we make no expectations here

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/invalid-reasons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].reason").value("Number format exception"))
                .andExpect(status().isOk());
    }

    @Test
    void handleInvalidReasonsDataNotFoundException_ShouldReturnNotFound() throws Exception {

        // Arrange
        String vesselCode = "4111"; // Wrong vesselCode
        String errorMessage = "No invalid data found for vessel code: " + vesselCode;

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/invalid-reasons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));
    }

    @Test
    void compareComplianceSuccess() throws Exception {

        // Arrange
        String vesselCode1 = "3001";
        String vesselCode2 = "19310";
        // Act & Assert
        mockMvc.perform(get("/api/vessels/compare-compliance")
                        .param("vesselCode1", vesselCode1)
                        .param("vesselCode2", vesselCode2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void handleCompareComplianceDataNotFoundException_ShouldReturnNotFound() throws Exception {

        // Arrange
        String vesselCode1 = "4111"; // Wrong vesselCode
        String vesselCode2 = "19310";
        String errorMessage = "No data found for vessel code: " + vesselCode1;

        // Act & Assert
        mockMvc.perform(get("/api/vessels/compare-compliance")
                        .param("vesselCode1", vesselCode1)
                        .param("vesselCode2", vesselCode2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));
    }

    @Test
    void statisticsDataSuccess() throws Exception {

        // Arrange
        String vesselCode = "3001";
        String startDate = "2023-10-06 00:00:00";
        String endDate = "2023-12-06 02:04:00";

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/data")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].latitude").value("44.8565368652344"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].longitude").value("-51.4986343383789"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].speedDifference").value(-1.109869775917801))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].compliancePercentage").value(88.97828864764128))
                .andExpect(status().isOk());
    }

    @Test
    void handleStatisticsDataNotFoundException_ShouldReturnNotFound() throws Exception {

        // Arrange
        String vesselCode = "19310";
        String startDate = "2023-10-06 00:00:00"; // Wrong year
        String endDate = "2023-12-06 02:04:00";
        String errorMessage = "No data found for vessel code: " + vesselCode + " in the specified period.";

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/data")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));
    }

    @Test
    void problemsSuccess() throws Exception {

        // Arrange
        String vesselCode = "19310";

        // 19310 vessel had for sure a lot of problems, so we expect the result list to have these keys
        // For the value of those keys it depends on the dataset used, so we make no expectations here

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/problems")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].size").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].startDateTime").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].endDateTime").exists())
                .andExpect(status().isOk());
    }

    @Test
    void handleProblemsDataNotFoundException_ShouldReturnNotFound() throws Exception {

        // Arrange
        String vesselCode = "4111"; // Wrong vesselCode
        String errorMessage = "No invalid data found for vessel code: " + vesselCode;

        // Act & Assert
        mockMvc.perform(get("/api/vessels/" + vesselCode + "/problems")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));
    }

}